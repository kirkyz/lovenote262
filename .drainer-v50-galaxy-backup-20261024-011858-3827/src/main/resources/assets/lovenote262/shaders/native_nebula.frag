#version 330 core
// V48: World-anchored, finite atmospheric volume along each view ray.
// Distant stellar sky is drawn by CelestialGpuRenderer (separate pass).
in vec3 v_SkyDirection;
out vec4 color;

layout(std140) uniform NebulaData {
    vec4 cameraTime;       // world camera xyz, elapsed animation time
    vec4 placement;        // lower world-Y, slab thickness, opacity, density
    vec4 primary;          // first RGB, movement speed
    vec4 secondary;        // second RGB, brightness
    vec4 controls;         // quality (0..2), max world sample distance, reserved
};

float hash12(vec2 p) {
    vec3 p3=fract(vec3(p.xyx)*0.1031);
    p3+=dot(p3,p3.yzx+33.33);
    return fract((p3.x+p3.y)*p3.z);
}
float noise2(vec2 p) {
    vec2 n=floor(p), f=fract(p);
    f=f*f*(3.0-2.0*f);
    return mix(mix(hash12(n),hash12(n+vec2(1,0)),f.x),
               mix(hash12(n+vec2(0,1)),hash12(n+vec2(1,1)),f.x),f.y);
}
float cloudDensity(vec3 world, float time, float detail) {
    // All spatial coordinates are absolute WORLD coordinates; no tile UVs.
    // Animated wind changes the volume, but walking does not translate it.
    vec2 p=world.xz*0.009 + vec2(time*0.023, -time*0.011);
    float warp=noise2(p*0.65+vec2(12.8,4.1));
    p+=vec2(warp-0.5, noise2(p*0.67+vec2(6.2,19.3))-0.5)*0.65;
    float broad=noise2(p);
    float detailNoise=noise2(p*2.3+vec2(-time*0.016,time*0.012));
    float density=smoothstep(0.42,0.70,broad*0.78+detailNoise*0.24);
    float wisps=smoothstep(0.48,0.72,detailNoise);
    float y01=clamp((world.y-placement.x)/max(1.0,placement.y),0.0,1.0);
    float vertical=pow(max(0.0,sin(3.14159265*y01)),0.85);
    return density*vertical*(0.57+0.43*wisps)*detail;
}

void main() {
    vec3 ray=normalize(v_SkyDirection);
    // Sky effect only: fade near horizon rather than stretching cloud layer
    // along rays almost parallel to its lower and upper planes.
    float horizon=smoothstep(0.075,0.26,ray.y);
    if (horizon < 0.002 || placement.w < 0.005 || placement.z < 0.005) discard;

    vec3 origin=cameraTime.xyz;
    float lower=placement.x;
    float upper=lower+max(16.0,placement.y);
    float nearT=max(0.0,(lower-origin.y)/max(ray.y,0.001));
    float farT=(upper-origin.y)/max(ray.y,0.001);
    if (farT < nearT || farT <= 0.0 || nearT > controls.y) discard;
    farT=min(farT,controls.y);
    if (farT <= nearT) discard;

    int steps=controls.x < 0.5 ? 3 : controls.x < 1.5 ? 5 : 7;
    float lengthT=farT-nearT;
    float time=cameraTime.w*clamp(primary.w,0.0,3.0);
    float integrated=0.0;
    float lit=0.0;
    for (int i=0;i<7;i++) {
        if (i>=steps) break;
        // Quasi-stratified samples; avoid camera-motion shimmering from
        // dynamic hash jitter or discrete altitude snapping.
        float u=(float(i)+0.5)/float(steps);
        vec3 world=origin+ray*(nearT+lengthT*u);
        float f=cloudDensity(world,time,clamp(placement.w,0.0,1.0));
        integrated+=f;
        lit+=f*smoothstep(0.53,0.85,noise2(world.xz*0.022+vec2(time*0.012,4.6)));
    }
    float avg=integrated/float(steps);
    float highlights=lit/max(integrated,0.0001);
    // Soft optical thickness keeps colour stable under camera movement.
    float transmittance=exp(-avg*min(lengthT,340.0)*0.014);
    float alpha=(1.0-transmittance)*placement.z*horizon;
    // Fade in at large distance so there is no hard volumetric cutoff.
    float farFade=1.0-smoothstep(controls.y*0.77,controls.y,nearT);
    alpha=clamp(alpha*farFade,0.0,0.72);
    if (alpha < 0.004) discard;
    vec3 tint=mix(primary.rgb,secondary.rgb,clamp(highlights*0.72+0.1,0.0,1.0));
    vec3 emission=tint*(0.62+highlights*0.42)*clamp(secondary.w,0.0,1.6);
    color=vec4(clamp(emission,0.0,1.0),alpha);
}

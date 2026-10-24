#version 330 core
// Drainer V50: world-anchored 3D nebula volume. The dome is ONLY a ray surface;
// all density is evaluated in absolute Minecraft world XYZ, not direction UVs.
// The stars are rendered separately by CelestialGpuRenderer.
in vec3 v_SkyDirection;
out vec4 color;

layout(std140) uniform NebulaData {
    vec4 cameraTime;  // camera world xyz, shader time
    vec4 placement;   // volume lower world Y, thickness, opacity, density
    vec4 primary;     // primary RGB, animation flow
    vec4 secondary;   // secondary RGB, brightness
    vec4 controls;    // quality, max ray distance, other controls
};

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}
float noise3(vec3 p) {
    vec3 i=floor(p), f=fract(p);
    f=f*f*(3.0-2.0*f);
    float a=hash31(i), b=hash31(i+vec3(1,0,0));
    float c=hash31(i+vec3(0,1,0)), d=hash31(i+vec3(1,1,0));
    float e=hash31(i+vec3(0,0,1)), g=hash31(i+vec3(1,0,1));
    float h=hash31(i+vec3(0,1,1)), j=hash31(i+vec3(1,1,1));
    return mix(mix(mix(a,b,f.x),mix(c,d,f.x),f.y),
               mix(mix(e,g,f.x),mix(h,j,f.x),f.y),f.z);
}

// The frequency is world-scaled: formations are hundreds of blocks across.
// No position is ever generated from the camera origin or camera-facing UVs.
vec2 gasDensity(vec3 world, float time, float quality) {
    vec3 p=world * vec3(0.0037,0.0056,0.0037);
    float wind=clamp(primary.w,0.0,3.0);
    vec3 motion=vec3(time*0.014, -time*0.006, time*0.010)*wind;
    float low=noise3(p*0.72 + motion*0.35 + vec3(2.2,3.7,9.3));
    // Fine ridges bend along the broad cloud structure, not flat ceiling stripes.
    float ridge=noise3(p*1.93 + motion + vec3(low*0.95,-low*0.43,low*0.6));
    float body=low*0.67+ridge*0.33;
    if (quality > 1.5) {
        body=body*0.86+noise3(p*3.8-motion*0.46+vec3(9.0,1.7,3.5))*0.14;
    }
    float lower=placement.x;
    float height=max(40.0,placement.y);
    float y=clamp((world.y-lower)/height,0.0,1.0);
    // Soft fade through the TOP and BOTTOM of a true 3D region.
    float heightEnvelope=smoothstep(0.0,0.19,y)*(1.0-smoothstep(0.78,1.0,y));
    float cloud=smoothstep(0.47,0.69,body)*heightEnvelope;
    float luminous=smoothstep(0.55,0.78,body*0.72+ridge*0.28);
    return vec2(cloud,luminous);
}

void main() {
    vec3 ray=normalize(v_SkyDirection);
    // Below the horizon leave Minecraft's normal atmosphere untouched.
    float horizon=smoothstep(0.075,0.26,ray.y);
    if (horizon<0.002 || placement.z<=0.002 || placement.w<=0.002) discard;

    vec3 origin=cameraTime.xyz;
    float lower=placement.x;
    float upper=lower+max(40.0,placement.y);
    // From below the volume we march forward into the sky. From within it,
    // begin at the camera. All sample coordinates stay absolute WORLD space.
    float nearT=max(0.0,(lower-origin.y)/max(ray.y,0.001));
    float farT=(upper-origin.y)/max(ray.y,0.001);
    float maxRange=max(100.0,controls.y);
    if (farT<=nearT || farT<=0.0 || nearT>=maxRange) discard;
    farT=min(farT,maxRange);
    if (farT<=nearT) discard;

    int steps=controls.x<0.5 ? 4 : controls.x<1.5 ? 7 : 10;
    float path=farT-nearT;
    float optical=0.0;
    vec3 light=vec3(0.0);
    float time=cameraTime.w;
    for (int i=0;i<10;i++) {
        if (i>=steps) break;
        float travel=nearT+path*(float(i)+0.5)/float(steps);
        vec3 world=origin+ray*travel;
        vec2 data=gasDensity(world,time,controls.x);
        float f=data.x*clamp(placement.w,0.0,1.0);
        float increment=f*path/float(steps)*0.0050;
        vec3 tint=mix(primary.rgb,secondary.rgb,clamp(data.y*0.80+0.08,0.0,1.0));
        light+=tint*increment*(0.35+0.60*data.y)*exp(-optical);
        optical+=increment;
    }
    float absorption=1.0-exp(-optical);
    float alpha=absorption*clamp(placement.z,0.0,0.80);
    // No hard ray cutoff: blend out before the maximum viewing distance.
    alpha*=1.0-smoothstep(maxRange*0.77,maxRange,nearT);
    alpha*=horizon;
    if(alpha<0.003) discard;
    // RGB remains independent of fade/opacity: adjusting opacity must not
    // blow colours out into saturated blocks at the horizon.
    vec3 emission=light*clamp(secondary.w,0.0,1.6);
    vec3 rgb=clamp(emission/max(absorption,0.003),vec3(0.0),vec3(1.0));
    color=vec4(rgb,clamp(alpha,0.0,0.75));
}

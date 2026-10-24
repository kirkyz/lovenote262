#version 330 core
in vec3 v_World;
in vec2 v_Local;
in float v_Layer;
out vec4 color;

layout (std140) uniform NebulaData {
    vec4 cameraTime;
    vec4 placement;
    vec4 primary;
    vec4 secondary;
    vec4 controls;
};

float hash21(vec2 p) {
    p = fract(p * vec2(123.34,456.21));
    p += dot(p,p + 45.32);
    return fract(p.x * p.y);
}
float noise2(vec2 p) {
    vec2 i=floor(p), f=fract(p);
    f=f*f*(3.0-2.0*f);
    float a=hash21(i), b=hash21(i+vec2(1.0,0.0));
    float c=hash21(i+vec2(0.0,1.0)), d=hash21(i+vec2(1.0,1.0));
    return mix(mix(a,b,f.x), mix(c,d,f.x), f.y);
}
float fbm(vec2 p) {
    float x = noise2(p)*0.55;
    x += noise2(p*2.07 + vec2(12.7,5.2))*0.29;
    // Save significant work on Performance quality.
    if (controls.w > 0.5) x += noise2(p*4.19 + vec2(5.5,13.7))*0.16;
    else x /= 0.84;
    return x;
}
void main() {
    // Animated domain warp, evaluated in ABSOLUTE world coordinates, never
    // screen UV or viewing direction. Walking does not re-centre patterns.
    float t = cameraTime.w * clamp(secondary.w, 0.0, 3.0);
    vec2 p = v_World.xz * 0.012;
    vec2 advected = p + vec2(t * 0.035, -t * 0.023);
    vec2 warp = vec2(fbm(advected*0.65 + vec2(13.4,2.1)),
                     fbm(advected*0.61 + vec2(4.8,20.9))) - 0.5;
    float broad = fbm(advected + warp*1.35 + v_Layer*1.37);
    float fine = fbm(advected*2.45 - warp*0.38 + v_Layer*2.9 + vec2(-t*0.055,t*0.038));
    float body = smoothstep(0.44,0.73, broad*0.78+fine*0.30);
    float luminous = smoothstep(0.59,0.81, broad*0.69+fine*0.37);

    // Cloud banks have large coherent gaps; avoid blanket-colouring the sky.
    float arc = sin(p.x*0.31+p.y*0.38 + sin(p.y*0.23)*1.1 + v_Layer*1.7);
    float structure = smoothstep(-0.45,0.6,arc);
    float cloud = body*mix(0.25,1.0,structure);
    float star = 0.0;
    if (controls.w > 0.5) {
        vec2 st = v_World.xz * 0.13;
        vec2 center = floor(st);
        float r = length(fract(st)-vec2(0.5));
        star = smoothstep(0.997,1.0,hash21(center+v_Layer*47.0))*exp(-r*r*170.0)*0.10;
    }

    // Fade by world-space distance, NOT by local tile edge. No visible 3x3 grid.
    float distanceXZ = distance(v_World.xz, cameraTime.xz);
    float distanceFade = 1.0 - smoothstep(controls.z*0.72, controls.z*1.38, distanceXZ);
    float angleFade = smoothstep(0.0,0.18,abs(normalize(v_World-cameraTime.xyz).y));
    float layerWeight = v_Layer < 0.5 ? 0.80 : v_Layer < 1.5 ? 0.60 : 0.42;
    float alpha = (cloud*0.46 + luminous*0.27) * primary.w * controls.y
                * layerWeight * distanceFade * angleFade;
    alpha = clamp(alpha + star * distanceFade, 0.0, 0.64);
    if (alpha < 0.003) discard;

    vec3 tint=mix(primary.rgb,secondary.rgb,clamp(0.25+fine*0.75,0.0,1.0));
    vec3 emission=(tint*(0.62+luminous*0.60) + secondary.rgb*luminous*0.16)
                 *clamp(controls.x,0.0,1.6);
    color=vec4(clamp(emission,0.0,1.0),alpha);
}

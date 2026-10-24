#version 330 core
// Drainer V62 | Independent world-space cloud field for the existing GPU skies.
// Only this shader changes: V61's renderer, module UI, macros and Rage are kept.
// A ray samples FIXED WORLD COORDINATES inside a bounded 3D atmosphere.
// Unlike flat sky-plane projection, no 1/direction.y horizon singularity.
in vec3 v_SkyDirection;
out vec4 color;

layout (std140) uniform SkyData {
    vec4 skyControl;      // seconds, mode, quality (1..3), brightness
    vec4 skyPrimary;      // primary RGB, gas density
    vec4 skySecondary;    // secondary RGB
    vec4 skyExtra;        // spatial scale
    vec4 skyWorld;        // actual camera world x/y/z, supplied by V61 Java
};

float saturate(float v) { return clamp(v, 0.0, 1.0); }
float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 33.33);
    return fract((p.x + p.y) * p.z);
}
float noise3(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    f = f*f*(3.0-2.0*f);
    float a = mix(hash13(i), hash13(i + vec3(1,0,0)), f.x);
    float b = mix(hash13(i + vec3(0,1,0)),hash13(i + vec3(1,1,0)),f.x);
    float c = mix(hash13(i + vec3(0,0,1)),hash13(i + vec3(1,0,1)),f.x);
    float e = mix(hash13(i + vec3(0,1,1)),hash13(i + vec3(1,1,1)),f.x);
    return mix(mix(a,b,f.y),mix(c,e,f.y),f.z);
}
float fbm(vec3 p, int octaves) {
    float val = 0.0, amp = 0.55, total = 0.0;
    for (int j=0; j<4; j++) {
        if (j>=octaves) break;
        val += noise3(p)*amp;
        total += amp;
        p = p*2.03+vec3(7.9,13.7,3.4);
        amp*=0.48;
    }
    return val/max(total,0.001);
}
// Astronomically distant stars SHOULD have no perceptible walking parallax;
// only the nearby volumetric formations respond to camera translation.
float distantStar(vec3 d) {
    vec3 p=d*194.0;
    vec3 cell=floor(p), delta=fract(p)-0.5;
    float seed=hash13(cell+vec3(5.2,8.1,2.9));
    float glint=1.0-smoothstep(0.033,0.09,length(delta));
    return step(0.9955,seed)*glint*(0.8+0.2*sin(skyControl.x*0.61+seed*60.0));
}
// This envelope selects an absolute world-altitude region, NOT a plane.
// Fade smoothly on both sides, so rays aimed near the horizon cannot create
// huge stretched streaks or abrupt dividing lines.
float airVolume(float worldY) {
    return smoothstep(100.0,220.0,worldY) *
           (1.0-smoothstep(830.0,1080.0,worldY));
}

vec3 colourAt(vec3 worldPos, vec3 direction, float time,
              float scale, vec3 primary, vec3 secondary,
              float mode, int octaves, out float intensity) {
    // Coordinates are in the Minecraft world, with time advecting the *gas*,
    // not spinning the field around the current player's camera.
    vec3 p=worldPos*(0.008*scale);
    float warp=fbm(p*0.83+vec3(time*0.032,-time*0.013,time*0.019),octaves);
    vec3 q=p+vec3(0.88,-0.42,1.24)*(warp-0.5);
    float broad=fbm(q*1.28+vec3(time*0.014,0.0,-time*0.012),octaves);
    float fine=noise3(q*2.38+vec3(-time*0.028,time*0.017,time*0.014));
    float mass=smoothstep(0.40,0.74,broad*0.85+fine*0.23);
    vec3 tint=mix(primary,secondary,saturate(0.12+fine*0.77));

    if (mode<0.5) { // Plasma: world-anchored electric filaments.
        float wave=sin(q.x*3.25+time*0.42+warp*2.8)
                 +0.71*sin(q.z*3.4-time*0.33+fine*1.7)
                 +0.40*cos(q.y*3.1+time*0.18);
        float filament=exp(-3.9*abs(wave));
        intensity=saturate(filament*0.76+mass*0.36);
        return tint*(0.33+filament*0.82);
    }
    if (mode<1.5) { // Ocean-like moving interference.
        float wave=0.5+0.5*sin(q.x*2.8+sin(q.z*1.9+time*0.7)*1.4+time*0.3+warp*2.0);
        intensity=saturate((wave*wave*0.50+mass*0.58));
        return mix(primary,secondary,saturate(wave*0.84+fine*0.16))*0.72;
    }
    if (mode<2.5) { // Water-like moving caustic veins.
        float caustic=pow(0.5+0.5*sin(q.y*3.8-time*0.8+sin(q.x*1.8+time*0.25)),4.0);
        intensity=saturate(mass*0.64+caustic*0.64);
        return mix(primary,secondary,saturate(caustic*0.8+warp*0.3))*0.76;
    }
    if (mode<3.5) { // Nebula: layered drifting gas separated by empty space.
        float upper=fbm(q*0.74+vec3(time*0.008,0.0,time*0.011),octaves);
        float body=smoothstep(0.37,0.73,upper*0.70+broad*0.28+fine*0.14);
        intensity=saturate(body*0.92+mass*0.18);
        return tint*(0.54+fine*0.53);
    }
    // Aurora: bent, world-positioned luminous curtains, NOT latitude stripes.
    float x=q.x*2.35+q.z*0.55;
    float fold=sin(x+sin(q.z*1.2+time*0.25)*1.8+warp*2.0+time*0.34);
    float filament=exp(-3.2*abs(fold));
    float filigree=pow(saturate(0.5+0.5*sin(x*3.3-time*0.78+fine*2.0)),7.0);
    intensity=saturate((filament*0.82+filigree*0.20)*
        (0.60+0.40*smoothstep(200.0,540.0,worldPos.y)));
    return mix(primary,secondary,saturate(0.25+worldPos.y/1200.0+fine*0.2))*0.94;
}
void main() {
    vec3 d=normalize(v_SkyDirection);
    float mode=skyControl.y;
    float time=skyControl.x*0.24;
    float scale=clamp(skyExtra.x,0.4,8.0);
    float density=clamp(skyPrimary.w,0.05,1.5);
    vec3 primary=clamp(skyPrimary.rgb,0.0,1.0);
    vec3 secondary=clamp(skySecondary.rgb,0.0,1.0);
    int quality=int(clamp(skyControl.z,1.0,3.0)+0.5);
    // Performance: 3 steps/2 octaves. Balanced: 4/3. Cinematic: 5/4.
    int steps=quality+2;
    int octaves=quality+1;
    vec3 accumulated=vec3(0.0);
    float transmittance=1.0;
    // No division by d.y, no infinite horizon intersection. Every ray uses
    // finite sample distances; all noise is evaluated in shared world space.
    for (int i=0;i<5;i++) {
        if (i>=steps) break;
        float distance=140.0+float(i)*190.0;
        vec3 worldSample=skyWorld.xyz+d*distance;
        float altitude=airVolume(worldSample.y);
        if (altitude<0.002) continue;
        float light=0.0;
        vec3 tint=colourAt(worldSample,d,time,scale,primary,secondary,
                           mode,octaves,light);
        float layerAlpha=saturate(light*density*altitude*0.45);
        // Front-to-back bounded emission, avoiding flat opaque colour bands.
        accumulated+=transmittance*tint*layerAlpha;
        transmittance*=1.0-layerAlpha;
    }
    // The horizon remains clear; no fixed-plane ray creates edge stretching.
    float horizon=smoothstep(-0.09,0.16,d.y);
    vec3 base=vec3(0.002,0.004,0.014);
    vec3 sky=base+accumulated*horizon;
    sky+=vec3(0.68,0.78,1.0)*distantStar(d)*0.63;
    sky=vec3(1.0)-exp(-sky*skyControl.w*1.55);
    color=vec4(clamp(sky,0.0,1.0),1.0);
}

#version 330 core

in vec3 v_SkyDirection;
out vec4 color;

// Same 64-byte std140 SkyData layout as V40. No Java changes required.
layout (std140) uniform SkyData {
    vec4 skyControl;   // time, preset, quality, brightness
    vec4 skyPrimary;   // primary RGB, density
    vec4 skySecondary; // secondary RGB, flow
    vec4 skyExtra;     // cloud scale, black hole size, aurora strength, expanse
    vec4 skyWorld;     // camera world xyz, fixed world-space aurora altitude
};

const float PI = 3.141592653589793;

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f*f*(3.0 - 2.0*f);
    float a = mix(hash31(i), hash31(i + vec3(1,0,0)), u.x);
    float b = mix(hash31(i + vec3(0,1,0)), hash31(i + vec3(1,1,0)), u.x);
    float e = mix(hash31(i + vec3(0,0,1)), hash31(i + vec3(1,0,1)), u.x);
    float f1 = mix(hash31(i + vec3(0,1,1)), hash31(i + vec3(1,1,1)), u.x);
    return mix(mix(a,b,u.y), mix(e,f1,u.y), u.z);
}

float fbm(vec3 p, float quality) {
    float sum = 0.0;
    float amp = 0.53;
    for (int i=0; i<4; ++i) {
        if (quality < 0.5 && i >= 2) break;
        if (quality < 1.5 && i >= 3) break;
        sum += amp * noise3(p);
        amp *= 0.49;
        p = p * 2.02 + vec3(1.8, 3.1, 2.7);
    }
    return sum;
}


// World-anchored atmospheric curtains, evaluated by projecting a view ray onto
// two FIXED horizontal atmospheric layers. Translation changes the sampled
// world position; animation only changes the curtains' internal turbulence.
// Unlike angle-only procedural strips, walking past this effect yields parallax.
float periodicDistance(float value, float period) {
    return abs(fract(value / period + 0.5) - 0.5) * period;
}

vec3 auroraWorldLayer(vec3 origin, vec3 dir, float altitude, float t,
                      float quality, float flow, vec3 primary, vec3 secondary,
                      float phase) {
    // No intersection above the player's eye or on rays below the horizon.
    if (dir.y <= 0.025 || origin.y >= altitude - 3.0) return vec3(0.0);
    float travel = (altitude - origin.y) / dir.y;
    if (travel < 0.0 || travel > 18000.0) return vec3(0.0);
    vec2 p = origin.xz + dir.xz * travel;
    float drift = t * flow;

    // Long spatial wavelengths make the curtain continuous over world regions.
    float fold = 20.0 * sin(p.x * 0.010 + drift * 0.16 + phase)
               + 13.0 * sin(p.x * 0.024 - drift * 0.11 + phase * 1.4);
    float wisp = 11.0 * sin(p.x * 0.053 + drift * 0.25 + phase);
    float distanceToRibbon = periodicDistance(p.y + fold + wisp + phase * 127.0, 490.0);
    float ribbon = exp(-pow(distanceToRibbon / 57.0, 2.0));

    // Different frequencies prevent a rigid spotlight-style appearance.
    float fine = 0.73 + 0.27 * sin(p.x * 0.092 - drift * 0.32
                      + 1.3 * sin(p.y * 0.014 + drift * 0.21));
    if (quality > 0.5) {
        float turbulent = noise3(vec3(p.x * 0.018, p.y * 0.016, drift * 0.08 + phase));
        fine *= mix(0.68, 1.18, turbulent);
    }
    float atHorizon = smoothstep(0.025, 0.12, dir.y);
    float farFade = 1.0 - smoothstep(7200.0, 17000.0, travel);
    float strength = ribbon * fine * atHorizon * farFade;
    float colourMix = 0.5 + 0.5 * sin(p.x * 0.011 + drift * 0.15 + phase);
    vec3 hue = mix(primary, secondary, colourMix);
    return hue * strength;
}

void main() {
    vec3 dir = normalize(v_SkyDirection);
    float t = skyControl.x;
    float mode = skyControl.y;
    float quality = skyControl.z;
    float brightness = skyControl.w;
    float density = clamp(skyPrimary.w, 0.0, 1.0);
    float flow = skySecondary.w;
    float expanse = clamp(skyExtra.w, 0.7, 3.0);
    vec3 c1 = skyPrimary.rgb;
    vec3 c2 = skySecondary.rgb;
    float h = 1.0 - abs(dir.y);

    // Subdued continuous atmosphere: no discrete left/right projection split.
    vec3 col = mix(vec3(0.003,0.005,0.015), vec3(0.010,0.012,0.026) + c1*0.028,
                   smoothstep(0.0,1.0,h));

    if (mode < 0.5) {
        // Large-scale galaxy band with continuous 3D noise, animated slowly.
        float timeShift = t * flow * 0.025;
        vec3 p = dir * (4.5 * skyExtra.x / expanse);
        vec3 warp = vec3(
            noise3(p*0.7 + vec3(timeShift, 0.0, 0.0)),
            noise3(p*0.7 + vec3(0.0, -timeShift, 0.0)),
            noise3(p*0.7 + vec3(0.0, 0.0, timeShift))
        ) - vec3(0.5);
        float coarse = fbm(p + warp*0.8 + vec3(timeShift, 0.0, -timeShift*0.45), quality);
        float fine = fbm(p*2.15 - warp*0.45 + vec3(-timeShift*0.4, timeShift*0.8, 0.0), quality);
        float dist = dot(dir, normalize(vec3(0.32, 0.81, -0.49)));
        float belt = exp(-dist*dist*6.5 / expanse);
        float structure = smoothstep(0.20,0.62,coarse*0.92 + fine*0.24);
        float horizonFade = smoothstep(-0.20, 0.14, dir.y);
        float amount = density * belt * structure * (0.50 + 0.50*horizonFade);
        vec3 cloudHue = mix(c1*0.62,c2*0.75,smoothstep(0.26,0.72,fine));
        col += cloudHue * amount * (0.24 + 0.43*coarse);
        col += c2 * amount * 0.12 * smoothstep(0.44,0.77,fine);
    } else if (mode < 1.5) {
        // Tilted accretion disc with smoothly travelling turbulence, not spokes.
        vec3 centre = normalize(vec3(0.25,0.52,-1.0));
        vec3 right = normalize(cross(centre, vec3(0.0,1.0,0.0)));
        vec3 up = normalize(cross(centre,right));
        vec2 off = vec2(dot(dir,right),dot(dir,up));
        float scale = expanse / max(0.45,skyExtra.y);
        vec2 uv = off * scale;
        vec2 ellipse = vec2(uv.x,uv.y*1.48);
        float radius = length(ellipse);
        float a = atan(ellipse.y,ellipse.x);
        float swirl = a - t*flow*0.33;
        float grain = fbm(vec3(cos(swirl)*2.3+radius*5.5,
                                sin(swirl)*2.3,
                                radius*8.0-t*flow*0.12), quality);
        float inner = exp(-pow((radius-0.165)*22.0,2.0));
        float outer = exp(-pow((radius-0.227)*13.0,2.0));
        float plasma = (0.45*inner + 0.48*outer) * (0.78+0.22*grain);
        col += mix(c1,c2,smoothstep(0.24,0.67,grain)) * plasma * 1.25;
        col += c1 * exp(-pow((radius-0.212)*6.0,2.0)) * 0.07;
        float sphereRadius = length(uv);
        float core = 1.0-smoothstep(0.086,0.108,sphereRadius);
        col *= (1.0-core);
        col += c2 * exp(-pow((sphereRadius-0.112)*26.0,2.0)) * 0.09;
    } else if (mode > 2.5) {
        // World-space Aurora V2: fixed altitude planes in world coordinates.
        // No player-position reset, and no camera-following aurora field.
        vec3 origin = skyWorld.xyz;
        float baseAlt = skyWorld.w;
        vec3 ribbons = auroraWorldLayer(origin, dir, baseAlt, t, quality, flow,
                                         c1 * 0.67, c2 * 0.78, 0.0);
        if (quality > 0.5) {
            ribbons += auroraWorldLayer(origin, dir, baseAlt + 38.0, t, quality, flow,
                                         c2 * 0.44, c1 * 0.41, 2.4) * 0.48;
        }
        // Keep atmospheric light subordinate to the sky; avoid screen-filling beams.
        col += ribbons * clamp(skyExtra.z, 0.0, 2.0) * 0.52;
    } else {
        // Curtains run ACROSS the sky as waving horizontal arcs. Small vertical
        // folds live inside them, unlike V40's isolated vertical spotlights.
        float az = atan(dir.z,dir.x);
        float elev = asin(clamp(dir.y,-1.0,1.0));
        float drift = t*flow;
        float toward = smoothstep(-0.74,0.18,cos(az+0.25));
        float waves = sin(az*2.25 + drift*0.11) * 0.08
                    + sin(az*5.6 - drift*0.17) * 0.022;
        float light = 0.0;
        float haze = 0.0;
        for (int i=0; i<2; i++) {
            float fi = float(i);
            float base = 0.27 + fi*0.20 + waves
                       + 0.027*sin(az*8.2 + fi*1.8 + drift*0.09);
            float delta = elev - base;
            float ribbon = exp(-pow(delta/(0.095+fi*0.025),2.0));
            float top = smoothstep(-0.04,0.03,delta) * exp(-max(0.0,delta)*4.5);
            float folds = 0.60 + 0.40*pow(0.5+0.5*sin(az*34.0 + fi*2.4
                        + 2.0*sin(elev*6.0 + drift*0.22) + drift*0.26),2.0);
            light += (ribbon*0.50 + top*0.40)*folds;
            haze += exp(-pow(delta/0.21,2.0))*0.12;
        }
        float fade = smoothstep(0.05,0.20,elev) * (1.0-smoothstep(1.08,1.35,elev));
        float intensity = fade * toward * skyExtra.z;
        vec3 auroraHue = mix(c1*0.55,c2*0.83,smoothstep(0.18,0.82,elev));
        col += auroraHue * intensity * (light*0.62+haze*0.33);
    }

    // Stable dim star points, visually secondary to the selected sky layer.
    vec3 cell = floor(dir*350.0);
    float rnd = hash31(cell);
    if (rnd > 0.997) {
        float twinkle = 0.90 + 0.10*sin(t*1.04 + rnd*69.0);
        col += vec3(0.28,0.35,0.50) * twinkle * (rnd-0.997)*93.0;
    }

    col *= brightness;
    col = vec3(1.0) - exp(-max(col,vec3(0.0)));
    color = vec4(col,1.0);
}

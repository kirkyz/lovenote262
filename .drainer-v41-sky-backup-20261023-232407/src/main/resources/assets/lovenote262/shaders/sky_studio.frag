#version 330 core

in vec2 v_SkyUV;
out vec4 color;

// Unchanged uniform layout: compatible with Drainer V38 SkyGpuRenderer.java.
layout (std140) uniform SkyData {
    vec4 skyControl;   // time, mode, quality, brightness
    vec4 skyPrimary;   // RGB and nebula density
    vec4 skySecondary; // RGB and flow strength
    vec4 skyExtra;     // cloud scale, black hole size, aurora intensity, unused
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
    float c = mix(hash31(i + vec3(0,0,1)), hash31(i + vec3(1,0,1)), u.x);
    float d = mix(hash31(i + vec3(0,1,1)), hash31(i + vec3(1,1,1)), u.x);
    return mix(mix(a,b,u.y),mix(c,d,u.y),u.z);
}

float fbm(vec3 p, float quality) {
    float amplitude = 0.55;
    float result = 0.0;
    for (int i = 0; i < 4; i++) {
        if (i >= 2 && quality < 0.5) break;
        if (i >= 3 && quality < 1.5) break;
        result += noise3(p) * amplitude;
        p = p * 2.04 + vec3(1.9, 3.5, 2.2);
        amplitude *= 0.48;
    }
    return result;
}

// Smooth short-angle difference; no hard seam at longitude +/-PI.
float angleDifference(float a, float b) {
    return atan(sin(a-b), cos(a-b));
}

void main() {
    float t = skyControl.x;
    float mode = skyControl.y;
    float quality = skyControl.z;
    float brightness = skyControl.w;
    float density = skyPrimary.w;
    float flow = skySecondary.w;
    // A wider-looking sky changes angular composition, not the physical mesh radius.
    float expanse = clamp(skyExtra.w, 0.70, 3.0);
    vec3 primary = skyPrimary.rgb;
    vec3 secondary = skySecondary.rgb;

    float lon = v_SkyUV.x * (2.0 * PI);
    float lat = v_SkyUV.y * PI;
    vec3 dir = normalize(vec3(cos(lon)*sin(lat), cos(lat), sin(lon)*sin(lat)));
    float horizon = pow(1.0 - abs(dir.y), 2.0);
    vec3 col = mix(vec3(0.004, 0.005, 0.019), primary * 0.058, horizon);

    if (mode < 0.5) {
        // Galactic Dream: broad diffuse clouds, a coherent galactic belt,
        // and slowly advecting noise. Avoid sharp cloud thresholds/black ink shapes.
        float drift = t * 0.055 * flow;
        vec3 p = dir * ((3.2 * skyExtra.x) / expanse);
        vec3 warp = vec3(
            noise3(p * 0.52 + vec3(drift, 0.0, 0.0)),
            noise3(p * 0.52 + vec3(0.0, drift * 0.75, 0.0)),
            noise3(p * 0.52 + vec3(0.0, 0.0, -drift * 0.66))
        ) - vec3(0.5);
        float broad = fbm(p + warp*0.72 + vec3(drift*0.40, drift*0.12, -drift*0.28), quality);
        float wisps = fbm(p*1.64 - warp*0.60 + vec3(-drift*0.17,drift*0.19,drift*0.20), quality);
        float galacticPlane = dot(dir, normalize(vec3(0.38, 0.74, -0.54)))
                            + 0.08 * sin(lon*1.4 + t*0.019);
        float belt = exp(-galacticPlane * galacticPlane * (3.5 / sqrt(expanse)));
        float softCloud = smoothstep(0.18, 0.77, broad * 0.82 + wisps * 0.24);
        // Blend softly near the horizon rather than showing a hard cloud wall.
        float horizonBlend = 0.45 + 0.55*smoothstep(-0.30, 0.24, dir.y);
        float opacity = density * belt * softCloud * horizonBlend;
        vec3 nebula = mix(primary*0.65, secondary*0.79, smoothstep(0.23, 0.73, wisps));
        col += nebula * opacity * (0.44 + 0.38 * broad);
        // Dim inner highlights, never hard-edged bright cloud islands.
        col += secondary * opacity * 0.12 * smoothstep(0.44, 0.78, broad);
    } else if (mode < 1.5) {
        // Event Horizon: a tilted, continuous plasma disc rather than radial spikes.
        vec3 centre = normalize(vec3(0.2, 0.55, -1.0));
        vec3 tangent = normalize(cross(centre, vec3(0.0,1.0,0.0)));
        vec3 bitangent = cross(centre, tangent);
        vec2 offset = vec2(dot(dir,tangent), dot(dir,bitangent));
        float sphereR = length(offset) * expanse / max(0.45, skyExtra.y);
        vec2 disc = vec2(offset.x, offset.y * 1.52) * expanse / max(0.45, skyExtra.y);
        float discR = length(disc);
        float azimuth = atan(disc.y,disc.x);
        float drift = t * flow;
        float turbulence = fbm(vec3(
            cos(azimuth*1.5 - drift*0.22)*2.0 + discR*7.0,
            sin(azimuth*1.5 - drift*0.22)*2.0,
            discR*9.0 - drift*0.35), quality);
        float coreRing = exp(-pow((discR - 0.175)*23.0, 2.0));
        float outerRing = exp(-pow((discR - 0.240)*12.5, 2.0));
        float travellingPlasma = 0.76 + 0.24 * sin(azimuth*3.0 - drift*0.40 + turbulence*2.5);
        float luminance = (outerRing*0.70 + coreRing*0.84) * travellingPlasma;
        vec3 plasmaColor = mix(primary, secondary, smoothstep(0.25,0.72,turbulence));
        col += plasmaColor * luminance * 1.55;
        col += plasmaColor * exp(-pow((discR - 0.225)*6.0, 2.0))*0.10;
        // Black occluding sphere stays circular; disc remains elongated behind it.
        float blackCore = 1.0 - smoothstep(0.086,0.115,sphereR);
        col *= 1.0 - blackCore;
        col += secondary * exp(-pow((sphereR-0.122)*25.0,2.0)) * 0.16;
    } else {
        // Aurora Nights: narrow translucent ribbons of light with height-based
        // envelopes and fine vertical folds, not full-screen luminous spotlights.
        float elevation = dir.y;
        float fadeIn = smoothstep(0.07,0.25,elevation);
        float fadeOut = 1.0 - smoothstep(0.72,0.96,elevation);
        float envelope = fadeIn * fadeOut;
        float drift = t * flow;
        float glow = 0.0;
        float curtain = 0.0;
        for (int i=0; i<3; i++) {
            float j = float(i);
            float centre = -2.30 + j*2.08;
            float bend = 0.18*sin(elevation*4.5 + drift*(0.15 + j*0.045) + j*1.7)
                       + 0.075*sin(elevation*10.0 - drift*0.22 + j);
            float delta = angleDifference(lon, centre + bend + drift*0.032);
            float width = (0.18 + j*0.055) * (0.84 + 0.16*expanse);
            float ribbon = exp(-pow(delta/width,2.0));
            float fold = 0.77 + 0.23*sin(lon*19.0 + elevation*16.0 + drift*0.54 + j*3.0);
            curtain += ribbon * fold;
            glow += exp(-pow(delta/(width*2.5),2.0))*0.29;
        }
        float shimmer = 0.90 + 0.10*sin(lon*16.0 + elevation*27.0 - drift*0.48);
        vec3 hue = mix(primary*0.66,secondary, smoothstep(0.2,0.88,elevation));
        col += hue * envelope * skyExtra.z * shimmer * (curtain*0.95 + glow*0.27);
        col += secondary * envelope * skyExtra.z * curtain * 0.07;
    }

    // Distant stars: subpixel noise, intentionally dim and unobtrusive.
    vec3 starCell = floor(dir*320.0);
    float rnd = hash31(starCell);
    if (rnd > 0.9965) {
        float twinkle = 0.89 + 0.11*sin(t*1.12 + rnd*70.0);
        col += vec3(0.30, 0.38, 0.57) * twinkle * (rnd-0.9965)*90.0;
    }

    col *= brightness;
    col = 1.0 - exp(-max(col,vec3(0.0)));
    color = vec4(col, 1.0);
}

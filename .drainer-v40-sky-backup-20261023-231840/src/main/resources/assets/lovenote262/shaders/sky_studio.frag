#version 330 core

in vec2 v_SkyUV;
out vec4 color;

layout (std140) uniform SkyData {
    vec4 skyControl;   // time, mode, quality, brightness
    vec4 skyPrimary;   // R G B nebula density
    vec4 skySecondary; // R G B flow
    vec4 skyExtra;     // cloud scale, black-hole size, aurora intensity, reserved
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
    float a = mix(hash31(i + vec3(0,0,0)), hash31(i + vec3(1,0,0)), u.x);
    float b = mix(hash31(i + vec3(0,1,0)), hash31(i + vec3(1,1,0)), u.x);
    float c = mix(hash31(i + vec3(0,0,1)), hash31(i + vec3(1,0,1)), u.x);
    float d = mix(hash31(i + vec3(0,1,1)), hash31(i + vec3(1,1,1)), u.x);
    return mix(mix(a,b,u.y),mix(c,d,u.y),u.z);
}

float fbm(vec3 p, float quality) {
    float a = 0.55;
    float f = 0.0;
    for (int i = 0; i < 4; ++i) {
        if (i >= 2 && quality < 0.5) break;
        if (i >= 3 && quality < 1.5) break;
        f += noise3(p) * a;
        p = p * 2.07 + vec3(1.9,3.5,2.2);
        a *= 0.49;
    }
    return f;
}

void main() {
    float t = skyControl.x;
    float mode = skyControl.y;
    float quality = skyControl.z;
    float bright = skyControl.w;
    float density = skyPrimary.w;
    float flow = skySecondary.w;
    vec3 a = skyPrimary.rgb;
    vec3 b = skySecondary.rgb;

    // Mapping to camera-independent celestial directions; no player-anchored sky motion.
    float lon = v_SkyUV.x * 2.0 * PI;
    float lat = v_SkyUV.y * PI;
    vec3 dir = normalize(vec3(cos(lon) * sin(lat), cos(lat), sin(lon) * sin(lat)));
    float horizon = pow(1.0 - abs(dir.y), 2.0);
    vec3 col = mix(vec3(0.006,0.006,0.025), a * 0.085, horizon);

    if (mode < 0.5) {
        // Galactic Dream: actual evolving 3D fBm, domain-warped to form living clouds.
        vec3 p = dir * (4.0 * skyExtra.x);
        float drift = t * 0.10 * flow;
        vec3 warp = vec3(noise3(p * 0.75 + vec3(drift,0,0)),
                         noise3(p * 0.75 + vec3(0,drift * .8,0)),
                         noise3(p * 0.75 + vec3(0,0,-drift))) - 0.5;
        float cloud = fbm(p + warp * 1.5 + vec3(drift*.75, drift*.2,-drift*.4), quality);
        float turbulent = fbm(p * 1.8 - warp * 1.2 + vec3(-drift*.3,drift*.6,drift*.4), quality);
        float band = exp(-abs(dir.y - 0.15*sin(lon*2.0 + t*0.035)) * 3.0);
        float cloudMask = smoothstep(0.35,0.78, cloud*0.83 + turbulent*0.38) * band * density;
        vec3 nebula = mix(a,b, clamp(turbulent*1.55,0.0,1.0));
        col += nebula * cloudMask * 2.0;
        col += b * smoothstep(0.56,0.9,cloud) * band * density * 0.26;
    } else if (mode < 1.5) {
        // Event Horizon: a stable celestial position, but a continuously advecting plasma disk.
        vec3 centre = normalize(vec3(0.2,0.55,-1.0));
        vec3 tangent = normalize(cross(centre,vec3(0.0,1.0,0.0)));
        vec3 bitangent = cross(centre,tangent);
        vec2 d = vec2(dot(dir,tangent), dot(dir,bitangent));
        float r = length(d) / max(0.45,skyExtra.y);
        float az = atan(d.y,d.x);
        float twist = az * 5.0 - t * 1.5 * flow + 0.14 / max(r,0.04);
        float plasma = fbm(vec3(cos(twist)*5.0,sin(twist)*5.0,r*9.0 - t*.15),quality);
        float outer = exp(-pow((r - 0.21) * 19.0,2.0));
        float inner = exp(-pow((r - 0.132) * 34.0,2.0));
        float rays = 0.60 + 0.4*sin(twist*2.0 + plasma*6.0);
        col += mix(a,b,plasma) * (outer*rays + inner*0.65) * 2.0;
        float black = 1.0 - smoothstep(0.085,0.116,r);
        col = mix(col,vec3(0.0),black);
        col += b * exp(-pow((r-0.27)*7.0,2.0)) * .10;
    } else {
        // Aurora Nights: moving translucent curtains with changing folds and bands.
        float az = lon;
        float drift = t * flow * 0.28;
        float elev = dir.y;
        float low = 0.18 + 0.10*sin(az*3.0 + drift);
        float active = smoothstep(low - 0.13,low + 0.18,elev) *
                       (1.0 - smoothstep(0.83,1.0,elev));
        float folds = sin(az*14.0 + 2.5*sin(az*3.3 + drift) + elev*9.0 - t*.4);
        folds = pow(folds*0.5 + 0.5,3.0);
        float curtains = 0.0;
        for (int i=0;i<3;i++) {
            float offset = float(i)*0.36;
            float line = sin(az*(3.0 + offset) + drift*(0.6+offset) +
                         sin(elev*9.0 + t*.19 + offset)*.85);
            curtains += smoothstep(0.27,0.84,line) * (0.5 + 0.5*folds) / 3.0;
        }
        float shimmer = 0.75 + 0.25*sin(elev*34.0 - t*.55 + az*9.0);
        col += mix(a,b,clamp(elev*1.3 + .20,0.0,1.0)) * curtains * active * shimmer * 2.6 * skyExtra.z;
    }

    // Stable subpixel stars; dim twinkle can animate without geometry rebuilding.
    vec3 cell = floor(dir * 310.0);
    float rnd = hash31(cell);
    if (rnd > 0.995) {
        float twinkle = 0.78 + 0.22*sin(t*1.9 + rnd*70.0);
        col += vec3(0.34,0.45,0.66) * twinkle * (rnd - 0.995)*100.0;
    }

    col *= bright;
    col = 1.0 - exp(-max(col,vec3(0.0))); // filmic-looking soft rolloff, not bloom
    color = vec4(col,1.0);
}

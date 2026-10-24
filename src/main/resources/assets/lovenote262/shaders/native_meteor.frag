#version 330 core
in vec2 v_UV;
in vec3 v_Color;
in float v_Alpha;
in float v_Type;
in float v_PlanetRings;
in float v_PlanetTime;
in float v_PlanetSeed;
in float v_SkyStyle;
out vec4 color;

void main(){
    float a;
    vec3 rgb = v_Color;

    if (v_Type > 4.5) {
        // An independently animated spherical planet, with rotating longitude,
        // evolving gas bands, moving storms and a separate translucent ring mask.
        vec2 p = (v_UV - vec2(0.5)) * 2.0;
        float r = length(p);
        float planetRadius = 0.57;
        float disk = 1.0 - smoothstep(planetRadius - 0.012, planetRadius + 0.018, r);
        float theta = asin(clamp(p.x / planetRadius, -0.999, 0.999));
        float lat = asin(clamp(p.y / planetRadius, -0.999, 0.999));
        float lon = theta + v_PlanetTime * (0.15 + 0.08 * sin(v_PlanetSeed));
        float warp = 0.10 * sin(3.0 * lon + lat * 7.0 + v_PlanetTime * 0.33);
        float bands = 0.74 + 0.14 * sin(lat * 24.0 + warp * 3.0 + sin(lon * 4.0) * 0.6)
                         + 0.08 * sin(lat * 57.0 - lon * 2.5 + v_PlanetTime * 0.24);
        float vortex = 0.5 + 0.5 * cos((lon - 0.9) * 5.0 + sin(lat * 9.0) + v_PlanetTime * 0.35);
        float storm = exp(-22.0 * pow(lat - 0.14 * sin(lon * 1.4), 2.0)) * pow(vortex, 8.0);
        bands += 0.23 * storm;
        float clouds = 0.5 + 0.5 * sin(lon * 11.0 + sin(lat * 12.0 + v_PlanetTime * 0.11) + v_PlanetTime * 0.37);
        vec3 normal = normalize(vec3(p / planetRadius, sqrt(max(0.002, 1.0 - r*r / (planetRadius*planetRadius)))));
        float lit = 0.16 + 0.84 * max(0.0, dot(normal, normalize(vec3(-0.7, 0.35, 0.8))));
        vec3 base = mix(v_Color, vec3(0.25, 0.6, 0.95), 0.16 + 0.12 * sin(v_PlanetSeed));
        if (v_SkyStyle > 2.5) base = mix(base, vec3(0.35, 0.10, 0.60), 0.55);
        if (v_SkyStyle > 1.5 && v_SkyStyle < 2.5) base = mix(base, vec3(1.0, 0.53, 0.16), 0.65);
        vec3 surface = base * bands * lit + vec3(0.24, 0.32, 0.42) * clouds * 0.10 * lit;
        float atmosphere = (1.0 - smoothstep(planetRadius-0.02, planetRadius+0.09, r)) - disk;
        vec2 tilted = vec2(p.x * 0.72 + p.y * 0.22, p.y * 2.55 - p.x * 0.18);
        float ringRad = length(tilted);
        float ring = smoothstep(0.66, 0.695, ringRad) * (1.0 - smoothstep(0.9, 0.94, ringRad));
        ring *= 0.70 + 0.30 * sin(ringRad * 75.0 + v_PlanetTime * 0.18);
        ring *= v_PlanetRings;
        // Front ring is more pronounced, rear ring fades behind the globe.
        float ringVisible = (p.y < 0.0) ? 1.0 : (1.0 - disk * 0.85);
        float ringAlpha = ring * ringVisible * 0.52;
        a = (disk + 0.35 * max(0.0, atmosphere) + ringAlpha) * v_Alpha;
        rgb = surface * disk + base * max(atmosphere,0.0) * 0.66 + mix(base, vec3(0.96), 0.65) * ringAlpha;
    } else if (v_Type > 3.5) {
        float across = 1.0 - abs(v_UV.x * 2.0 - 1.0);
        float glow = pow(max(across, 0.0), 2.4);
        float head = pow(clamp(v_UV.y, 0.0, 1.0), 1.25);
        a = (0.18 + 0.82 * head) * glow * v_Alpha;
        rgb *= 1.25 + 0.55 * glow;
    } else if (v_Type > 2.5) {
        vec2 uv = v_UV - vec2(0.5);
        float r = length(vec2(uv.x, uv.y * 1.75));
        float soft = 1.0 - smoothstep(0.05, 0.55, r);
        a = soft * v_Alpha;
        rgb *= 0.78 + 0.22 * soft;
    } else if (v_Type > 1.5) {
        float r = length(v_UV - vec2(0.5));
        float soft = 1.0 - smoothstep(0.0, 0.5, r);
        a = soft * soft * v_Alpha;
        rgb *= 0.95 + 0.45 * soft;
    } else if (v_Type > 0.5) {
        float r = length(v_UV - vec2(0.5));
        a = (1.0 - smoothstep(0.06, 0.49, r)) * v_Alpha;
        rgb *= 1.05;
    } else {
        float across = 1.0 - abs(v_UV.x * 2.0 - 1.0);
        float glow = pow(max(across, 0.0), 2.2);
        float head = pow(clamp(v_UV.y, 0.0, 1.0), 1.6);
        a = (0.12 + 0.88 * head) * glow * v_Alpha;
        rgb *= 0.68 + 0.58 * glow;
    }

    if (a < 0.004) discard;
    color = vec4(clamp(rgb, 0.0, 1.0), clamp(a, 0.0, 0.95));
}

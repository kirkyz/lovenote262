#version 330 core
in vec2 v_UV;
in vec3 v_Color;
in float v_Alpha;
in float v_Type;
in float v_PlanetRings;
out vec4 color;

void main(){
    float a;
    vec3 rgb = v_Color;

    if (v_Type > 4.5) {
        // Procedurally shaded planet: lit globe, animated atmospheric bands,
        // and an independently toggleable tilted ring system.
        vec2 p = (v_UV - vec2(0.5)) * 2.0;
        float r = length(p);
        float disk = 1.0 - smoothstep(0.55, 0.61, r);
        float z = sqrt(max(0.0, 1.0 - min(r * r / 0.36, 1.0)));
        vec3 normal = normalize(vec3(p / 0.60, z));
        float light = 0.17 + 0.83 * max(0.0, dot(normal, normalize(vec3(-0.62, 0.44, 0.82))));
        float bands = 0.74 + 0.13 * sin((p.y + 0.20 * p.x) * 22.0)
                     + 0.10 * sin((p.y - 0.12 * p.x) * 48.0);
        float atmosphere = (1.0 - smoothstep(0.57, 0.69, r)) - disk;
        float ringEllipse = length(vec2(p.x * 0.72 + p.y * 0.12, p.y * 2.4));
        float ring = (1.0 - smoothstep(0.82, 0.88, ringEllipse))
                   * smoothstep(0.67, 0.73, ringEllipse) * v_PlanetRings;
        a = max(disk, atmosphere * 0.22) * v_Alpha + ring * v_Alpha * 0.48;
        rgb = v_Color * (bands * light * disk + atmosphere * 0.65)
              + mix(v_Color, vec3(0.95), 0.45) * ring * 0.43;
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

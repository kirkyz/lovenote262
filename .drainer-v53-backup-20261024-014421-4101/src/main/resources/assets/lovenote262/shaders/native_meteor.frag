#version 330 core
in vec2 v_UV;
in vec3 v_Color;
in float v_Alpha;
in float v_Type;
out vec4 color;

void main(){
    float a;
    vec3 rgb = v_Color;

    if (v_Type > 3.5) {
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

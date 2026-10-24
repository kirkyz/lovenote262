#version 330 core
in vec2 v_UV;
in vec3 v_Color;
in float v_Fade;
in float v_Band;
in vec3 v_World;
out vec4 color;

layout (std140) uniform AuroraData {
    vec4 cameraTime;
    vec4 placement;
    vec4 primary;
    vec4 secondary;
    vec4 extras;
};

float hash21(vec2 p) {
    return fract(sin(dot(p, vec2(41.7, 289.3))) * 47453.5453);
}

float noise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1,0)), f.x),
               mix(hash21(i + vec2(0,1)), hash21(i + vec2(1,1)), f.x), f.y);
}

void main() {
    float t = cameraTime.w * max(secondary.w, 0.0);
    float u = v_UV.x;
    float v = v_UV.y;
    // Dissolve curtain ends and vertically feather the top and bottom.
    float edge = smoothstep(0.0, 0.12, u) * (1.0 - smoothstep(0.88, 1.0, u));
    float vertical = pow(max(0.0, sin(v * 3.14159265)), 1.35);
    float wave = sin(16.3 * u + 2.7 * v + 0.21 * t + v_Band * 1.3);
    float fine = sin(47.0 * u + 6.1 * v - 0.37 * t + sin(u * 9.0));
    float irregular = noise2(vec2(u * 11.0 - t * 0.042,
                                  v * 3.3 + t * 0.023 + v_Band * 8.0));
    float streaks = (0.70 + 0.17 * wave + 0.10 * fine) * (0.74 + 0.26 * irregular);
    float strength = clamp(streaks, 0.0, 1.0);
    float distanceAlpha = clamp(v_Fade, 0.0, 1.0);
    float layerStrength = (v_Band > 1.5) ? 0.72 : 1.0;
    float alpha = primary.w * edge * vertical * strength * distanceAlpha * layerStrength;
    alpha *= clamp(extras.x, 0.0, 1.5);
    if (alpha <= 0.003) discard;
    vec3 light = v_Color * (0.74 + 0.38 * irregular);
    color = vec4(clamp(light, 0.0, 1.0), clamp(alpha, 0.0, 0.82));
}

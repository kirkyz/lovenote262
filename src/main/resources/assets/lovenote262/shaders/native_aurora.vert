#version 330 core
layout (location = 0) in vec3 Position;
layout (location = 1) in vec2 Texture;
layout (location = 2) in vec4 Color;

layout (std140) uniform MeshData {
    mat4 u_Proj;
    mat4 u_ModelView;
};
layout (std140) uniform AuroraData {
    vec4 cameraTime;   // camera X/Y/Z, time
    vec4 placement;    // fixed world-grid cell X/Z, altitude, height
    vec4 primary;      // RGB + opacity
    vec4 secondary;    // RGB + flow
    vec4 extras;       // brightness, quality, cellSize, reserved
};

out vec2 v_UV;
out vec3 v_Color;
out float v_Fade;
out float v_Band;
out vec3 v_World;

const float TAU = 6.28318530718;

float h21(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    int code = int(floor(Position.z + 0.5));
    int tile = code / 3;
    int band = code - tile * 3;
    int ix = tile % 3 - 1;
    int iz = tile / 3 - 1;
    vec2 cellID = placement.xy + vec2(float(ix), float(iz));
    vec2 base = (cellID + 0.5) * extras.z;
    float phase = TAU * h21(cellID + float(band) * 7.0);
    float orient = 0.17 + 1.3 * h21(cellID + float(band) * 13.0);
    vec2 tangent = vec2(cos(orient), sin(orient));
    vec2 normal = vec2(-tangent.y, tangent.x);
    float u = Texture.x;
    float v = Texture.y;
    float x = (u - 0.5) * (extras.z * 1.24);
    // Huge world-space curtains with multiple independent evolving folds.
    float drift = cameraTime.w * max(0.0, secondary.w);
    float fold = 15.0 * sin(u * 8.7 + phase + drift * 0.28)
               + 7.0 * sin(u * 22.1 - drift * 0.47 + phase * 0.7)
               + 3.3 * sin(u * 42.0 + drift * 0.19 + phase * 1.3);
    float verticalFold = 3.8 * sin(v * 7.0 + u * 10.0 + drift * 0.12 + phase);
    float y = placement.z + v * placement.w
            + 13.0 * sin(u * 5.1 + phase * 0.53)
            + verticalFold;
    vec2 xz = base + tangent * x + normal * (float(band - 1) * 31.0 + fold);
    // WORLD geometry; camera position enters only the view transform.
    vec3 worldPos = vec3(xz.x, y, xz.y);
    vec3 cameraRelative = vec3(worldPos.x - cameraTime.x, worldPos.y, worldPos.z - cameraTime.z);
    gl_Position = u_Proj * u_ModelView * vec4(cameraRelative, 1.0);
    v_UV = vec2(u, v);
    v_World = worldPos;
    v_Band = float(band);
    v_Fade = 1.0 - smoothstep(extras.z * 1.08, extras.z * 1.85,
                              distance(worldPos.xz, cameraTime.xz));
    v_Color = mix(primary.rgb, secondary.rgb,
                  clamp(v * 0.68 + 0.13 * float(band), 0.0, 1.0));
}

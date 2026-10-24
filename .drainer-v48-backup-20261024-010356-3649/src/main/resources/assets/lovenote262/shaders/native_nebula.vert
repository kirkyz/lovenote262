#version 330 core
layout (location = 0) in vec3 Position;
layout (location = 1) in vec2 Texture;
layout (location = 2) in vec4 Color;

layout (std140) uniform MeshData {
    mat4 u_Proj;
    mat4 u_ModelView;
};
layout (std140) uniform NebulaData {
    vec4 cameraTime;    // world camera x/y/z, time
    vec4 placement;     // stable tile ID x/z, altitude, layer spacing
    vec4 primary;       // RGB + opacity
    vec4 secondary;     // RGB + flow
    vec4 controls;      // brightness, density, world cell size, quality
};

out vec3 v_World;
out vec2 v_Local;
out float v_Layer;

void main() {
    int encoded = int(floor(Position.z + 0.5));
    int tile = encoded / 3;
    int layer = encoded - tile * 3;
    vec2 tileID = placement.xy + vec2(float(tile % 3 - 1), float(tile / 3 - 1));
    vec2 worldXZ = (tileID + Texture.xy) * controls.z;
    // Surface deformation is evaluated in absolute world coordinates. This
    // keeps the same cloud formation fixed as the camera moves through tiles.
    float distortion = 7.0 * sin(worldXZ.x * 0.022 + worldXZ.y * 0.011 + float(layer))
                     + 5.0 * sin(worldXZ.y * 0.028 - worldXZ.x * 0.013 + float(layer) * 1.7);
    float y = placement.z + float(layer) * placement.w + distortion;
    v_World = vec3(worldXZ.x, y, worldXZ.y);
    v_Local = Texture.xy;
    v_Layer = float(layer);
    vec3 relative = vec3(worldXZ.x - cameraTime.x, y, worldXZ.y - cameraTime.z);
    gl_Position = u_Proj * u_ModelView * vec4(relative, 1.0);
}

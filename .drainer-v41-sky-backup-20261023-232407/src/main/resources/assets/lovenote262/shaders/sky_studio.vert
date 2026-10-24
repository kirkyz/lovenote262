#version 330 core

layout (location = 0) in vec3 Position;
layout (location = 1) in vec2 Texture;
layout (location = 2) in vec4 Color;

layout (std140) uniform MeshData {
    mat4 u_Proj;
    mat4 u_ModelView;
};

out vec2 v_SkyUV;

void main() {
    gl_Position = u_Proj * u_ModelView * vec4(Position, 1.0);
    v_SkyUV = Texture;
}

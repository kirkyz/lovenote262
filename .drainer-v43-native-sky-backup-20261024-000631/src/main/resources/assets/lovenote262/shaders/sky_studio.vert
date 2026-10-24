#version 330 core

layout (location = 0) in vec3 Position;
layout (location = 1) in vec2 Texture;
layout (location = 2) in vec4 Color;

layout (std140) uniform MeshData {
    mat4 u_Proj;
    mat4 u_ModelView;
};

// The direction is periodic around the dome's longitude seam. Interpolating
// this, rather than the raw 0..1 UV, avoids a discontinuous procedural sky.
out vec3 v_SkyDirection;

const float PI = 3.141592653589793;

void main() {
    float longitude = Texture.x * (2.0 * PI);
    float latitude = Texture.y * PI;
    v_SkyDirection = vec3(
        cos(longitude) * sin(latitude),
        cos(latitude),
        sin(longitude) * sin(latitude)
    );
    gl_Position = u_Proj * u_ModelView * vec4(Position, 1.0);
}

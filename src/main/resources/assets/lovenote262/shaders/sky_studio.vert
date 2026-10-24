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
    // Use camera rotation only. Camera movement must not translate the sky.
    // Minecraft 26.2 uses reversed-Z depth: 0.0 is FAR, 1.0 is NEAR.
    // The old 0.999 placed this dome in front of nearly all world geometry.
    // Keep XY projection, but force the depth to the far plane. The sky
    // pipeline has depth testing ON and depth writes OFF, so opaque blocks,
    // entities and terrain already in the depth buffer occlude the sky.
    vec4 clip = u_Proj * vec4(mat3(u_ModelView) * Position, 1.0);
    gl_Position = vec4(clip.xy, 0.0, clip.w);
}

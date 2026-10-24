#version 330 core
layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 Texture;
layout(location = 2) in vec4 Color;
layout(std140) uniform MeshData { mat4 u_Proj; mat4 u_ModelView; };
layout(std140) uniform MeteorData {
    vec4 cameraTime;
    vec4 tileAltitudeLength;
    vec4 primary;
    vec4 secondary;
    vec4 extras;
    vec4 flags;
    vec4 additions;
};
out vec2 v_UV;
out vec3 v_Color;
out float v_Alpha;
out float v_Type;
out float v_PlanetRings;

float hash21(vec2 p){ return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123); }
float hash11(float p){ return fract(sin(p * 91.3458) * 47453.5453); }

void main(){
    int encoded = int(floor(Position.z + 0.5));
    int tile = encoded / 16;
    int particle = encoded - tile * 16;
    int ix = tile % 5 - 2;
    int iz = tile / 5 - 2;

    vec2 id = tileAltitudeLength.xy + vec2(float(ix), float(iz));
    float seed = hash21(id * 7.31 + float(particle) * 15.7);
    float seed2 = hash21(id * 19.43 + float(particle) * 5.9);
    float seed3 = hash21(id * 0.79 + float(particle) * 31.9);
    float seed4 = hash21(id * 11.73 + float(particle) * 4.19);

    vec2 home = (id + vec2(0.1 + 0.8 * seed, 0.1 + 0.8 * seed2)) * extras.z;
    float t = max(0.0, cameraTime.w);
    float local = t * max(0.01, secondary.w) / (6.0 + seed2 * 8.0) + seed * 41.0;
    float cycle = fract(local);

    // Each quality tier contains a representative mix of effect types.
    // V52 placed comets/haze after its Balanced particle budget, hiding them.
    bool isStar = (particle == 1 || particle == 5 || particle == 8 ||
                   particle == 12 || particle == 14);
    bool isDust = (particle == 2 || particle == 7);
    bool isComet = (particle == 3 || particle == 13);
    bool isHaze = (particle == 4 || particle == 9 || particle == 15);
    bool isPlanet = (particle == 10);

    float altitude = tileAltitudeLength.z + 38.0 * (seed3 - 0.5) + float(particle % 3) * 22.0;
    vec3 world;
    float alpha;
    float width;
    vec3 head;
    vec3 tail;
    float type = 0.0;

    if (isPlanet) {
        // Planets occupy rare, stable world cells rather than following the camera.
        // A single billboard is shaded into a globe and optional rings in the fragment shader.
        type = 5.0;
        vec3 centre = vec3(home.x, tileAltitudeLength.z + 58.0 + 34.0 * seed2, home.y);
        vec3 look = normalize(vec3(cameraTime.xyz) - centre);
        vec3 right = normalize(cross(vec3(0.0, 1.0, 0.0), look));
        if (length(right) < 0.001) right = vec3(1.0, 0.0, 0.0);
        vec3 up = normalize(cross(look, right));
        float size = (35.0 + 38.0 * seed3) * additions.z;
        world = centre + right * (Texture.x - 0.5) * size + up * (Texture.y - 0.5) * size;
        alpha = additions.x * (seed4 > 0.86 ? 0.87 : 0.0);
    } else if (isHaze) {
        type = 3.0;
        float size = mix(85.0, 155.0, seed2) * mix(0.85, 1.35, flags.w);
        vec3 centre = vec3(home.x + (seed - 0.5) * 110.0,
                           tileAltitudeLength.z - 48.0 + seed3 * 30.0,
                           home.y + (seed2 - 0.5) * 110.0);
        vec3 look = normalize(vec3(cameraTime.xyz) - centre);
        vec3 right = normalize(cross(vec3(0.0, 1.0, 0.0), look));
        if (length(right) < 0.001) right = vec3(1.0, 0.0, 0.0);
        vec3 up = normalize(cross(look, right));
        world = centre + right * (Texture.x - 0.5) * size + up * (Texture.y - 0.5) * size * 0.55;
        float pulse = 0.65 + 0.35 * sin(t * (0.14 + 0.24 * seed4) + seed * 6.283185);
        alpha = flags.y * flags.z * primary.w * 0.34 * pulse;
    } else if (isComet) {
        type = 4.0;
        float progress = fract(local * 0.45 + seed3 * 13.0);
        float theta = 2.4 + seed4 * 1.2;
        vec3 direction = normalize(vec3(cos(theta), -0.08 - 0.12 * seed2, sin(theta)));
        float travelled = (progress - 0.5) * (350.0 + seed2 * 140.0);
        head = vec3(home.x, tileAltitudeLength.z + 26.0 + seed3 * 68.0, home.y) + direction * travelled;
        tail = head - direction * tileAltitudeLength.w * (1.65 + 0.7 * seed2);
        width = 3.4 + seed3 * 4.6;
        float fadeIn = smoothstep(0.02, 0.14, progress);
        float fadeOut = 1.0 - smoothstep(0.76, 0.99, progress);
        alpha = flags.x * fadeIn * fadeOut * (0.85 + 0.15 * sin(t * 3.0 + seed4 * 9.0));
    } else if (isDust) {
        type = 2.0;
        float drift = sin(t * (0.12 + seed2 * 0.22) + seed * 6.283185) * 14.0;
        vec3 centre = vec3(home.x + drift,
                           tileAltitudeLength.z + 6.0 + seed3 * 55.0,
                           home.y + cos(t * (0.09 + seed3 * 0.19) + seed2 * 6.283185) * 14.0);
        vec3 look = normalize(vec3(cameraTime.xyz) - centre);
        vec3 right = normalize(cross(vec3(0.0, 1.0, 0.0), look));
        if (length(right) < 0.001) right = vec3(1.0, 0.0, 0.0);
        vec3 up = normalize(cross(look, right));
        width = 2.2 + seed4 * 3.0;
        world = centre + right * (Texture.x - 0.5) * width + up * (Texture.y - 0.5) * width;
        alpha = extras.y * additions.w * 0.45 * (0.55 + 0.45 * pow(0.5 + 0.5 * sin(t * (0.55 + 0.55 * seed4) + seed3 * 6.283185), 2.0));
    } else if (isStar) {
        type = 1.0;
        head = vec3(home.x, altitude + seed2 * 42.0, home.y);
        tail = head;
        width = 0.7 + seed3 * 1.3;
        alpha = extras.y * (0.45 + 0.55 * additions.w) * (0.35 + 0.65 * pow(0.5 + 0.5 * sin(t * (0.4 + 1.9 * seed) + seed3 * 6.283185), 3.0));
        vec3 look = normalize(vec3(cameraTime.xyz) - head);
        vec3 right = normalize(cross(vec3(0.0, 1.0, 0.0), look));
        if (length(right) < 0.001) right = vec3(1.0, 0.0, 0.0);
        vec3 up = normalize(cross(look, right));
        world = head + right * (Texture.x - 0.5) * width + up * (Texture.y - 0.5) * width;
    } else {
        type = 0.0;
        float progress = cycle;
        float theta = 2.1 + seed3 * 0.9;
        vec3 direction = normalize(vec3(cos(theta), -0.12 - 0.25 * seed2, sin(theta)));
        float travelled = (progress - 0.5) * (240.0 + seed2 * 170.0);
        head = vec3(home.x, altitude, home.y) + direction * travelled;
        tail = head - direction * tileAltitudeLength.w * (0.65 + 0.8 * seed2);
        width = 1.6 + seed3 * 2.4;
        float fadeIn = smoothstep(0.04, 0.20, progress);
        float fadeOut = 1.0 - smoothstep(0.72, 0.99, progress);
        alpha = fadeIn * fadeOut;
    }

    if (type == 0.0 || type == 4.0) {
        vec3 along = normalize(head - tail + vec3(0.0001, 0.0001, 0.0001));
        vec3 middle = (head + tail) * 0.5;
        vec3 look = normalize(vec3(cameraTime.xyz) - middle);
        vec3 sideways = normalize(cross(along, look));
        if (length(sideways) < 0.001) sideways = normalize(cross(along, vec3(0.0, 1.0, 0.0)));
        world = mix(tail, head, Texture.y) + sideways * (Texture.x - 0.5) * width;
    }

    vec3 relative = vec3(world.x - cameraTime.x, world.y, world.z - cameraTime.z);
    gl_Position = u_Proj * u_ModelView * vec4(relative, 1.0);

    float dist = length(world - vec3(cameraTime.xyz));
    if (type == 3.0) {
        alpha *= 1.0 - smoothstep(120.0, 450.0, dist);
    } else {
        alpha *= 1.0 - smoothstep(280.0, 690.0, dist);
    }
    alpha *= clamp(primary.w * extras.x, 0.0, 1.0);

    v_UV = Texture;
    v_Type = type;
    v_Color = mix(primary.rgb, secondary.rgb, seed2);
    if (type == 2.0) v_Color = mix(v_Color, secondary.rgb, 0.45);
    if (type == 3.0) v_Color = mix(primary.rgb, secondary.rgb, 0.65);
    if (type == 4.0) v_Color = mix(secondary.rgb, vec3(1.0), 0.55);
    if (type == 5.0) v_Color = mix(primary.rgb, secondary.rgb, 0.35 + seed * 0.4);
    v_Alpha = alpha;
    v_PlanetRings = additions.y;
}

#version 330 core
// Drainer V63 sky-anchor test. Original, isolated diagnostic shader.
// Do NOT copy this effect into normal skies until in-game anchoring is verified.
in vec3 v_SkyDirection;
out vec4 color;
layout (std140) uniform SkyData {
    vec4 skyControl;
    vec4 skyPrimary;
    vec4 skySecondary;
    vec4 skyExtra;        // x unused; y,z,w = fixed world-space anchor
    vec4 skyWorld;        // xyz = current world-space camera
};
float sat(float v) { return clamp(v, 0.0, 1.0); }
// The closest distance from a world-space ray to a fixed 3D centre.
// Unlike ray/height-plane intersections, this is finite and has no horizon pole.
float distanceToRay(vec3 ray, vec3 point) {
    vec3 relative = point - skyWorld.xyz;
    float forward = dot(relative, ray);
    if (forward <= 2.0) return 100000.0;
    return length(relative - ray * forward);
}
vec3 glowingBeacon(vec3 ray, vec3 center, float radius, vec3 tint, float t, float phase) {
    float radial = distanceToRay(ray, center);
    float core = 1.0 - smoothstep(radius * .65, radius, radial);
    float rim = (1.0 - smoothstep(radius * 1.2, radius * 1.85, radial)) * (1.0 - core);
    float glow = 1.0 - smoothstep(radius * 1.5, radius * 4.2, radial);
    float pulse = .82 + .18 * sin(t * 2.0 + phase);
    vec3 outputColor = tint * (core * 1.0 + rim * .45 + glow * .18) * pulse;
    outputColor += vec3(.88,.92,1.0) * core * .32;
    return outputColor;
}
void main() {
    vec3 ray = normalize(v_SkyDirection);
    vec3 center = skyExtra.yzw;
    vec3 camera = skyWorld.xyz;
    vec3 left  = center + vec3(-72.0, 14.0, -20.0);
    vec3 right = center + vec3( 70.0, 33.0,  40.0);
    // The background is a simple stationary reference, NOT the old nebula renderer.
    vec3 background = mix(vec3(.003,.006,.020),vec3(.009,.015,.035),sat(ray.y * .45 + .55));
    float seconds = skyControl.x;
    vec3 light = glowingBeacon(ray, center, 16.0, skyPrimary.rgb, seconds, 0.0);
    light += glowingBeacon(ray, left,  10.0, skySecondary.rgb, seconds, 1.9);
    light += glowingBeacon(ray, right, 12.0, vec3(.94,.62,.25), seconds, 3.6);
    // Stationary concentric outline around the main marker: a clear visual
    // reference for translation parallax while walking sideways.
    float borderDistance = distanceToRay(ray, center);
    float ring = (1.0-smoothstep(20.0,21.4,borderDistance)) * smoothstep(18.1,19.5,borderDistance);
    light += mix(skyPrimary.rgb,skySecondary.rgb,.5) * ring * .7;
    float horizonFade = smoothstep(-.12, .05, ray.y);
    vec3 rgb = (background + light * horizonFade) * skyControl.w;
    color = vec4(clamp(rgb,0.0,1.0),1.0);
}

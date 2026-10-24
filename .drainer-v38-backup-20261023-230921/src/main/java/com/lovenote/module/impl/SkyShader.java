package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Tex;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import net.minecraft.client.Camera;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Drainer V36 Sky Studio: first three distinct animated sky renderers.
 *
 * World-oriented sky effects (the sky centres on the camera, but stars / celestial
 * directions DO NOT rotate with the camera). No entities, chunk scanning, shader
 * downloads or new rendering dependencies. CPU-work budgets stay deterministic.
 *
 * The V36 sky intentionally does NOT claim HDR bloom, volumetrics or GPU mesh caching.
 */
public final class SkyShader extends Module {
    private static final float R = 235f;
    private static final float PI = (float) Math.PI;
    private static final float TAU = PI * 2f;
    private static final int MAX_STARS = 900;

    // Keep the historic module/setting names that matter for the client menu.
    private final ModeSetting preset = add(new ModeSetting("Preset", "Galactic Dream",
            "Off", "Galactic Dream", "Event Horizon", "Aurora Nights"));
    private final ModeSetting palette = add(new ModeSetting("Palette", "Violet & Cyan",
            "Violet & Cyan", "Pink & Blue", "Emerald", "Crimson", "Client Theme"));
    private final ModeSetting quality = add(new ModeSetting("Sky quality", "Balanced",
            "Performance", "Balanced", "Cinematic"));
    private final BoolSetting overworldOnly = add(new BoolSetting("Overworld only", true));
    private final BoolSetting vanillaClouds = add(new BoolSetting("Vanilla clouds", false));
    private final BoolSetting hideWhenGuiOpen = add(new BoolSetting("Hide in menus", false));
    private final BoolSetting animated = add(new BoolSetting("Animate sky", true));
    private final NumberSetting animationSpeed = add(new NumberSetting("Animation speed", 1.0, 0.0, 3.0, 0.05));
    private final NumberSetting skyBrightness = add(new NumberSetting("Sky brightness", 0.8, 0.15, 1.5, 0.05));
    private final NumberSetting starCount = add(new NumberSetting("Star count", 320, 0, MAX_STARS, 20));
    private final NumberSetting starBrightness = add(new NumberSetting("Star brightness", 0.75, 0, 1, 0.05));
    private final NumberSetting starSize = add(new NumberSetting("Star size", 2.3, 0.5, 5, 0.1));
    private final NumberSetting twinkle = add(new NumberSetting("Star twinkle", 0.5, 0, 1, 0.05));
    private final NumberSetting cloudDensity = add(new NumberSetting("Nebula density", 0.72, 0, 1, 0.05));
    private final NumberSetting cloudScale = add(new NumberSetting("Nebula scale", 1, 0.3, 2.0, 0.05));
    private final NumberSetting cloudDrift = add(new NumberSetting("Nebula drift", 0.6, 0, 2, 0.05));
    private final NumberSetting auroraBands = add(new NumberSetting("Aurora bands", 3, 1, 6, 1));
    private final NumberSetting auroraWaves = add(new NumberSetting("Aurora waves", 1.0, 0, 3, 0.05));
    private final NumberSetting auroraOpacity = add(new NumberSetting("Aurora opacity", 0.62, 0, 1, 0.05));
    private final NumberSetting blackHoleSize = add(new NumberSetting("Black hole size", 1, 0.4, 1.8, 0.05));
    private final NumberSetting blackHoleSpin = add(new NumberSetting("Accretion speed", 1, 0, 3, 0.05));
    private final NumberSetting skyHaze = add(new NumberSetting("Horizon glow", 0.3, 0, 1, 0.05));
    private final BoolSetting shootingStars = add(new BoolSetting("Shooting stars", true));
    private final NumberSetting meteorRate = add(new NumberSetting("Meteor rate", 0.4, 0, 1, 0.05));

    // Pre-compute once. No per-frame Random instances / new particle objects.
    private final float[] sx = new float[MAX_STARS];
    private final float[] sy = new float[MAX_STARS];
    private final float[] sz = new float[MAX_STARS];
    private final float[] seed = new float[MAX_STARS];
    private long startNanos = System.nanoTime();

    public SkyShader() {
        super("SkyShader", Category.RENDER);
        describe("Sky Studio V36: animated galaxy, black hole and aurora skies with visual budgets.");
        Random random = new Random(0x36A5B1L);
        for (int i = 0; i < MAX_STARS; i++) {
            // Upper hemisphere is weighted to avoid wasting stars far below horizon.
            float y = 0.04f + random.nextFloat() * 0.95f;
            float a = random.nextFloat() * TAU;
            float radial = (float) Math.sqrt(Math.max(0f, 1f - y * y));
            sx[i] = (float) Math.cos(a) * radial * R * 0.94f;
            sy[i] = y * R * 0.94f;
            sz[i] = (float) Math.sin(a) * radial * R * 0.94f;
            seed[i] = random.nextFloat();
        }
    }

    @Override public void onEnable() { startNanos = System.nanoTime(); }
    public boolean active() { return enabled && !preset.is("Off"); }
    public boolean vanillaClouds() { return vanillaClouds.get(); }

    @Override
    public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if (mc.level == null || preset.is("Off")) return;
        if (overworldOnly.get() && mc.level.dimensionType().skybox() != DimensionType.Skybox.OVERWORLD) return;
        if (hideWhenGuiOpen.get() && mc.gui.screen() != null) return;

        Camera camera = ctx.camera();
        Matrix4f mat = stack.last().pose();
        float cx = (float) camera.position().x;
        float cy = (float) camera.position().y;
        float cz = (float) camera.position().z;
        float t = animated.get() ? (float) ((System.nanoTime() - startNanos) * 1e-9) * animationSpeed.getF() : 0;
        int level = quality.is("Performance") ? 0 : quality.is("Cinematic") ? 2 : 1;
        float budget = Math.max(0.18f, Math.min(1f, VisualOptimizer.budget(100, 18) / 100f));
        int primary = colourOne();
        int secondary = colourTwo();

        Fx.cameraBasis(camera);
        Tex.ensure();
        renderDome(mat, cx, cy, cz, level, primary, secondary);
        renderStars(mat, cx, cy, cz, t, level, budget, secondary);

        switch (preset.get()) {
            case "Galactic Dream" -> galaxy(mat, cx, cy, cz, t, level, budget, primary, secondary);
            case "Event Horizon" -> eventHorizon(mat, cx, cy, cz, t, level, budget, primary, secondary);
            case "Aurora Nights" -> aurora(mat, cx, cy, cz, t, level, budget, primary, secondary);
            default -> { }
        }
        if (shootingStars.get() && meteorRate.getF() > 0.01f) {
            renderMeteors(mat, cx, cy, cz, t, level, budget, secondary);
        }
    }

    private int colourOne() {
        return switch (palette.get()) {
            case "Pink & Blue" -> 0xFFFF67CA;
            case "Emerald" -> 0xFF31E6C0;
            case "Crimson" -> 0xFFFF5C84;
            case "Client Theme" -> Colors.C1;
            default -> 0xFF9A70FF;
        };
    }

    private int colourTwo() {
        return switch (palette.get()) {
            case "Pink & Blue" -> 0xFF54ADFF;
            case "Emerald" -> 0xFF58D9FF;
            case "Crimson" -> 0xFFFFA553;
            case "Client Theme" -> Colors.C3;
            default -> 0xFF60E8F4;
        };
    }

    private int backgroundTop() {
        return switch (preset.get()) {
            case "Aurora Nights" -> 0xFF030C21;
            case "Event Horizon" -> 0xFF04020E;
            default -> 0xFF08051D;
        };
    }

    private int backgroundHorizon(int primary) {
        int base = switch (preset.get()) {
            case "Aurora Nights" -> 0xFF103A44;
            case "Event Horizon" -> 0xFF170E35;
            default -> 0xFF2C174D;
        };
        return Colors.lerp(base, primary, skyHaze.getF() * 0.22f);
    }

    private static final class Dome {
        final int segments, rings;
        final float[] ringY, ringRadius, cos, sin;
        Dome(int segments, int rings) {
            this.segments = segments;
            this.rings = rings;
            ringY = new float[rings + 1];
            ringRadius = new float[rings + 1];
            cos = new float[segments + 1];
            sin = new float[segments + 1];
            for (int row = 0; row <= rings; row++) {
                double phi = Math.PI * row / rings;
                ringY[row] = (float) Math.cos(phi) * R;
                ringRadius[row] = (float) Math.sin(phi) * R;
            }
            for (int col = 0; col <= segments; col++) {
                double phi = Math.PI * 2 * col / segments;
                cos[col] = (float) Math.cos(phi);
                sin[col] = (float) Math.sin(phi);
            }
        }
    }

    private static final Dome[] DOMES = {
            new Dome(18, 7), new Dome(24, 9), new Dome(36, 12)
    };

    private void renderDome(Matrix4f mat, float cx, float cy, float cz, int detail, int primary, int secondary) {
        // Old default: 48x16=768 quads. V36: 126/216/432. All trigonometry is cached.
        Dome dome = DOMES[detail];
        int top = Colors.lerp(0xFF010207, backgroundTop(), Math.min(1f, skyBrightness.getF()));
        int horizon = Colors.lerp(top, backgroundHorizon(primary), Math.min(1f, skyBrightness.getF()));
        int down = Colors.lerp(horizon, 0xFF000207, 0.75f);
        MeshBuilder mesh = Fx.colorQuadsDepth();
        for (int row = 0; row < dome.rings; row++) {
            float y0 = dome.ringY[row], y1 = dome.ringY[row + 1];
            float r0 = dome.ringRadius[row], r1 = dome.ringRadius[row + 1];
            int c0 = y0 >= 0 ? Colors.lerp(horizon, top, y0 / R) : Colors.lerp(horizon, down, -y0 / R);
            int c1 = y1 >= 0 ? Colors.lerp(horizon, top, y1 / R) : Colors.lerp(horizon, down, -y1 / R);
            for (int col = 0; col < dome.segments; col++) {
                float cosA = dome.cos[col], sinA = dome.sin[col];
                float cosB = dome.cos[col + 1], sinB = dome.sin[col + 1];
                Fx.vc(mesh, mat, cx + cosA * r0, cy + y0, cz + sinA * r0, c0);
                Fx.vc(mesh, mat, cx + cosB * r0, cy + y0, cz + sinB * r0, c0);
                Fx.vc(mesh, mat, cx + cosB * r1, cy + y1, cz + sinB * r1, c1);
                Fx.vc(mesh, mat, cx + cosA * r1, cy + y1, cz + sinA * r1, c1);
            }
        }
        Fx.draw();
    }

    private void renderStars(Matrix4f mat, float cx, float cy, float cz, float t, int level, float budget, int tint) {
        int cap = Math.min(MAX_STARS, Math.round(starCount.getI() * budget * (level == 0 ? 0.65f : level == 2 ? 1f : 0.85f)));
        if (cap <= 0 || starBrightness.getF() <= 0.001f) return;
        float opacity = starBrightness.getF();
        float flicker = twinkle.getF();
        float starRadius = starSize.getF();
        Fx.texture(Tex.STAR);
        MeshBuilder b = Fx.texQuadsDepth();
        for (int i = 0; i < cap; i++) {
            float f = 1f - flicker * 0.48f * (0.5f + 0.5f * (float) Math.sin(t * 1.9f + seed[i] * 24f));
            int a = Math.round(218f * f * opacity);
            if (a < 4) continue;
            int colour = i % 7 == 0 ? tint : 0xFFE9EDFF;
            Fx.billboard(b, mat, cx + sx[i], cy + sy[i], cz + sz[i],
                    starRadius * (0.65f + seed[i] * 0.6f), Colors.alpha(colour, a));
        }
        Fx.draw();
    }

    private void galaxy(Matrix4f mat, float cx, float cy, float cz, float t, int level, float budget, int primary, int secondary) {
        // A softly moving S-shaped nebula band, not a static 1-colour background.
        float density = cloudDensity.getF();
        int count = Math.round((level == 0 ? 18 : level == 1 ? 34 : 55) * budget * density);
        if (count < 1) return;
        float drift = t * cloudDrift.getF();
        Fx.texture(Tex.SOFT);
        MeshBuilder cloud = Fx.texQuadsDepth();
        for (int i = 0; i < count; i++) {
            float f = (i + 0.5f) / count;
            float az = -1.9f + f * 2.65f + 0.08f * (float) Math.sin(f * 18f + drift * 0.25f);
            float elev = 0.55f + 0.26f * (float) Math.sin(f * PI * 1.5f) + 0.11f * (float) Math.cos(f * PI * 2f);
            float scatter = hash(i * 13) - 0.5f;
            elev += scatter * 0.16f;
            float circle = R * 0.84f * (float) Math.cos(elev);
            float xx = cx + (float) Math.cos(az) * circle;
            float yy = cy + (float) Math.sin(elev) * R * 0.84f;
            float zz = cz + (float) Math.sin(az) * circle;
            float size = cloudScale.getF() * (45f + (i % 5) * 10f) * (1f + .06f * (float) Math.sin(drift + i));
            int hue = Colors.lerp(primary, secondary, 0.15f + 0.7f * f);
            int alpha = Math.round((level == 0 ? 29f : 38f) * density * (0.7f + 0.3f * hash(i)));
            Fx.billboard(cloud, mat, xx, yy, zz, size, Colors.alpha(hue, alpha));
        }
        Fx.draw();
        // Separate, lighter stars along the central band make it distinguishable from the aurora.
        Fx.texture(Tex.STAR);
        MeshBuilder sparkles = Fx.texQuadsDepth();
        int dust = Math.round((level == 0 ? 42 : 100) * budget * density);
        for (int i = 0; i < dust; i++) {
            float f = hash(i * 3 + 71);
            float az = -1.9f + 2.65f * f;
            float elev = 0.55f + 0.26f * (float) Math.sin(f * PI * 1.5f)
                    + (hash(i * 3 + 73) - 0.5f) * 0.25f;
            float rr = R * 0.96f * (float) Math.cos(elev);
            int alpha = Math.round(100f * density * (0.5f + 0.5f * hash(i + 33)));
            Fx.billboard(sparkles, mat,
                    cx + (float) Math.cos(az) * rr,
                    cy + (float) Math.sin(elev) * R * 0.96f,
                    cz + (float) Math.sin(az) * rr,
                    1.1f + hash(i) * 1.7f, Colors.alpha(secondary, alpha));
        }
        Fx.draw();
    }

    private void eventHorizon(Matrix4f mat, float cx, float cy, float cz, float t, int level,
                              float budget, int primary, int secondary) {
        // Fixed position in the celestial sphere; does not orbit the player.
        float baseX = cx + R * 0.63f;
        float baseY = cy + R * 0.41f;
        float baseZ = cz - R * 0.41f;
        float size = blackHoleSize.getF();
        float spin = t * blackHoleSpin.getF() * 0.28f;
        float r1 = size * 16f;
        float r2 = size * 24f;
        float r3 = size * 34f;
        int detail = level == 0 ? 20 : level == 1 ? 32 : 48;
        // Render a tinted outer corona, an accretion ring and a dark centre.
        MeshBuilder ring = Fx.colorQuadsDepth();
        ringBand(ring, mat, baseX, baseY, baseZ, r2, r3, spin, detail, secondary, primary, 12, 0.70f);
        ringBand(ring, mat, baseX, baseY, baseZ, r1, r2, -spin * 1.5f, detail, primary, secondary, 150, 0.64f);
        ringBand(ring, mat, baseX, baseY, baseZ, size * 10.8f, r1, spin * 0.7f, detail,
                0xFF02020A, 0xFF100B1A, 245, 1f);
        ringBand(ring, mat, baseX, baseY, baseZ, 0, size * 11.3f, 0, detail,
                0xFF010208, 0xFF010208, 255, 1f);
        Fx.draw();

        // Luminous orbital debris in one batched draw; bounded by quality/budget.
        int sprites = Math.round((level == 0 ? 22 : level == 1 ? 44 : 70) * budget);
        if (sprites < 1) return;
        Fx.texture(Tex.GLOW);
        MeshBuilder sparks = Fx.texQuadsDepth();
        for (int i = 0; i < sprites; i++) {
            float offset = TAU * (i + 0.33f * hash(i * 7)) / sprites;
            float angle = offset + spin * (0.5f + hash(i) * 0.9f);
            float radius = size * (20 + 15 * hash(i + 4));
            float x = (float) Math.cos(angle) * radius;
            float y = (float) Math.sin(angle) * radius * 0.66f;
            int glow = Colors.lerp(primary, secondary, hash(i + 17));
            Fx.billboard(sparks, mat, baseX + x, baseY + y, baseZ + (float) Math.sin(angle) * 2f,
                    (1.9f + hash(i + 15) * 4f) * size,
                    Colors.alpha(glow, 70 + Math.round(60 * hash(i + 16))));
        }
        Fx.draw();
    }

    private void ringBand(MeshBuilder b, Matrix4f mat, float cx, float cy, float cz,
                          float inner, float outer, float spin, int segments,
                          int aColor, int bColor, int opacity, float squash) {
        // Angled celestial plane; the loop itself is stable in world space.
        for (int i = 0; i < segments; i++) {
            float a0 = TAU * i / segments;
            float a1 = TAU * (i + 1) / segments;
            float shift = spin;
            float c0 = (float) Math.cos(a0 + shift), s0 = (float) Math.sin(a0 + shift);
            float c1 = (float) Math.cos(a1 + shift), s1 = (float) Math.sin(a1 + shift);
            int colour0 = Colors.alpha(Colors.lerp(aColor, bColor, 0.5f + .5f * (float) Math.sin(a0 * 2 + shift)), opacity);
            int colour1 = Colors.alpha(Colors.lerp(aColor, bColor, 0.5f + .5f * (float) Math.sin(a1 * 2 + shift)), opacity);
            Fx.vc(b, mat, cx + c0 * inner, cy + s0 * inner * squash, cz + s0 * 0.2f * inner, colour0);
            Fx.vc(b, mat, cx + c1 * inner, cy + s1 * inner * squash, cz + s1 * 0.2f * inner, colour1);
            Fx.vc(b, mat, cx + c1 * outer, cy + s1 * outer * squash, cz + s1 * 0.2f * outer, colour1);
            Fx.vc(b, mat, cx + c0 * outer, cy + s0 * outer * squash, cz + s0 * 0.2f * outer, colour0);
        }
    }

    private void aurora(Matrix4f mat, float cx, float cy, float cz, float t, int level,
                        float budget, int primary, int secondary) {
        int bands = Math.max(1, Math.round(auroraBands.getI() * (level == 0 ? 0.7f : 1) * budget));
        int slices = level == 0 ? 18 : level == 1 ? 28 : 42;
        float movement = auroraWaves.getF();
        float opacity = auroraOpacity.getF();
        if (opacity <= 0.001f) return;
        MeshBuilder b = Fx.colorQuadsDepth();
        for (int band = 0; band < bands; band++) {
            float offset = band * 0.62f;
            float depth = R * (0.33f + band * 0.055f);
            for (int col = 0; col < slices; col++) {
                float u0 = col / (float) slices;
                float u1 = (col + 1f) / slices;
                auroraSegment(b, mat, cx, cy, cz, u0, u1, offset, depth,
                        t * 0.48f * movement, primary, secondary, opacity);
            }
        }
        Fx.draw();
    }

    private void auroraSegment(MeshBuilder b, Matrix4f mat, float cx, float cy, float cz,
                               float u0, float u1, float offset, float depth,
                               float movement, int primary, int secondary, float opacity) {
        float x0 = cx + (u0 - 0.5f) * R * 1.28f;
        float x1 = cx + (u1 - 0.5f) * R * 1.28f;
        float z0 = cz + depth + 15f * (float) Math.sin(u0 * TAU * 1.1f + movement + offset);
        float z1 = cz + depth + 15f * (float) Math.sin(u1 * TAU * 1.1f + movement + offset);
        float y0 = cy + 60f + 18f * (float) Math.sin(u0 * TAU * 1.5f - movement + offset);
        float y1 = cy + 60f + 18f * (float) Math.sin(u1 * TAU * 1.5f - movement + offset);
        float tall0 = 60f + 16f * (float) Math.sin(u0 * TAU * 2.3f + movement + offset);
        float tall1 = 60f + 16f * (float) Math.sin(u1 * TAU * 2.3f + movement + offset);
        // Top bright, bottom faint. Each ribbon column warps independently with time.
        int bottom = Colors.alpha(primary, Math.round(14f * opacity));
        int top0 = Colors.alpha(Colors.lerp(primary, secondary, 0.35f + 0.4f * u0), Math.round(105f * opacity));
        int top1 = Colors.alpha(Colors.lerp(primary, secondary, 0.35f + 0.4f * u1), Math.round(105f * opacity));
        Fx.vc(b, mat, x0, y0, z0, bottom);
        Fx.vc(b, mat, x1, y1, z1, bottom);
        Fx.vc(b, mat, x1, y1 + tall1, z1, top1);
        Fx.vc(b, mat, x0, y0 + tall0, z0, top0);
    }

    private void renderMeteors(Matrix4f mat, float cx, float cy, float cz, float t, int level, float budget, int tint) {
        // Deterministic, time-indexed streaks: no allocations or random spawning per frame.
        float rate = meteorRate.getF();
        int lanes = Math.round((level == 0 ? 1 : level == 1 ? 2 : 3) * budget * rate);
        if (lanes < 1) return;
        Fx.texture(Tex.STAR);
        MeshBuilder mesh = Fx.texQuadsDepth();
        for (int i = 0; i < lanes; i++) {
            float cycle = t * (0.13f + 0.03f * i) + i * 0.61f;
            float phase = cycle - (float) Math.floor(cycle);
            float alpha = Math.max(0f, 1f - Math.abs(phase * 2f - 1f));
            if (alpha < 0.1f) continue;
            float angle = 1.3f + hash(i * 37 + (int) Math.floor(cycle) * 19) * PI * 1.2f;
            float x = (float) Math.cos(angle) * R * .78f + phase * 60f;
            float z = (float) Math.sin(angle) * R * .78f - phase * 35f;
            float y = R * .63f - phase * 72f;
            int colour = Colors.alpha(tint, Math.round(210f * alpha * rate));
            Fx.billboard(mesh, mat, cx + x, cy + y, cz + z, 3.1f, colour);
            Fx.billboard(mesh, mat, cx + x - 11, cy + y + 12, cz + z + 8, 5.3f,
                    Colors.alpha(tint, Math.round(55f * alpha * rate)));
        }
        Fx.draw();
    }

    private static float hash(int input) {
        int n = input * 0x45d9f3b;
        n = (n ^ (n >>> 16)) * 0x45d9f3b;
        n = n ^ (n >>> 16);
        return (n & 0x7fffffff) / (float) 0x7fffffff;
    }
}

package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Tex;
import com.lovenote.util.Vfx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;

/** Native 26.2 ambient particles, lightweight and client-side only. */
public final class AmbientParticles extends Module {
    private final ModeSetting palette = add(new ModeSetting("Palette", "Theme", "Theme", "Rainbow", "Ice", "White"));
    private final NumberSetting count = add(new NumberSetting("Particles", 24, 1, 100, 1));
    private final NumberSetting radius = add(new NumberSetting("Radius", 7, 2, 20, 0.5));
    private final NumberSetting speed = add(new NumberSetting("Float speed", 0.5, 0, 2.5, 0.05));
    private final NumberSetting size = add(new NumberSetting("Size", 0.28, 0.05, 1.2, 0.02));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 145, 10, 255, 5));

    public AmbientParticles() {
        super("AmbientParticles", Category.VISUALS);
        describe("Client-side glowing motes floating nearby: density, speed, size, opacity, palette.");
    }

    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if (mc.player == null || mc.level == null) return;
        Matrix4f mat = stack.last().pose();
        Fx.cameraBasis(ctx.camera());
        Tex.ensure();
        Fx.texture(Tex.SOFT);
        MeshBuilder b = Fx.texQuadsDepth();
        final float t = System.currentTimeMillis() * 0.001f;
        final float r = radius.getF(), sp = speed.getF();
        final double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        for (int i = 0, n = count.getI(); i < n; i++) {
            float a = Vfx.hash(i * 23 + 1), c = Vfx.hash(i * 23 + 2), d = Vfx.hash(i * 23 + 3);
            float phase = ((c + t * sp * (0.025f + d * 0.02f)) % 1f);
            float px = (float) (x + (a * 2 - 1) * r + Math.sin(t * sp + i) * 0.3f);
            float py = (float) (y - 1 + phase * (r * 1.4));
            float pz = (float) (z + (d * 2 - 1) * r);
            int tint = switch (palette.get()) {
                case "Rainbow" -> Colors.rainbow(a + t * 0.025f);
                case "Ice" -> 0xFF9AE8FF;
                case "White" -> 0xFFFFFFFF;
                default -> Colors.wave(a);
            };
            int alpha = (int) (opacity.getI() * (0.35 + 0.65 * Math.sin(Math.PI * phase)));
            Fx.billboard(b, mat, px, py, pz, size.getF() * (0.7f + c * 0.8f), Colors.alpha(tint, alpha));
        }
        Fx.draw();
    }
}

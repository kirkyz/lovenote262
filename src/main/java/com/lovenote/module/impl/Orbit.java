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

/** Native 26.2 visual effect; no server packets or world mutation. */
public final class Orbit extends Module {
    private final ModeSetting palette = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Pink", "White"));
    private int tint(float f) {
        return switch (palette.get()) {
            case "Rainbow" -> Colors.rainbow(f);
            case "Ice" -> 0xFF93E6FF;
            case "Pink" -> 0xFFFF7AD9;
            case "White" -> 0xFFFFFFFF;
            default -> Colors.wave(f);
        };
    }
    private final NumberSetting particles = add(new NumberSetting("Sparks", 20, 3, 70, 1));
    private final NumberSetting radius = add(new NumberSetting("Radius", 2.2, 0.5, 8, 0.1));
    private final NumberSetting height = add(new NumberSetting("Height", 1.1, -1, 4, 0.1));
    private final NumberSetting size = add(new NumberSetting("Size", 0.27, 0.05, 1.2, 0.05));
    private final NumberSetting speed = add(new NumberSetting("Rotation speed", 1, 0, 5, 0.1));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 170, 5, 255, 5));

    public Orbit() { super("Orbit", Category.VISUALS); describe("Glowing motes orbit the player; speed, radius, count, colour and opacity are adjustable."); }
    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if (mc.player == null) return;
        Matrix4f mat = stack.last().pose();
        Fx.cameraBasis(ctx.camera()); Tex.ensure(); Fx.texture(Tex.GLOW);
        MeshBuilder b = Fx.texQuadsDepth();
        double t = System.currentTimeMillis() * 0.001 * speed.get();
        int n = particles.getI();
        for (int i = 0; i < n; ++i) {
            double a = t + i * Math.PI * 2 / n;
            float y = (float)(mc.player.getY() + height.get() + Math.sin(a * 2) * 0.4);
            Fx.billboard(b, mat, (float)(mc.player.getX()+Math.cos(a)*radius.get()), y,
                (float)(mc.player.getZ()+Math.sin(a)*radius.get()), size.getF(),
                Colors.alpha(tint(i/(float)n), opacity.getI()));
        }
        Fx.draw();
    }

}

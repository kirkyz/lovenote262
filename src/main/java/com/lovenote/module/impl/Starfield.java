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

/** Extra low-cost star overlay around the camera; independent of SkyShader presets. */
public final class Starfield extends Module {
    private final ModeSetting palette = add(new ModeSetting("Palette", "Theme", "Theme", "Ice", "White", "Rainbow"));
    private final NumberSetting stars = add(new NumberSetting("Star count", 75, 10, 220, 5));
    private final NumberSetting distance = add(new NumberSetting("Distance", 130, 50, 240, 10));
    private final NumberSetting size = add(new NumberSetting("Star size", 0.65, 0.15, 1.8, 0.05));
    private final NumberSetting twinkle = add(new NumberSetting("Twinkle", 0.4, 0, 1, 0.05));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 180, 15, 255, 5));

    public Starfield() {
        super("Starfield", Category.VISUALS);
        describe("Optional 3D star overlay with palette, density and twinkle settings.");
    }

    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if (mc.level == null || mc.player == null) return;
        Matrix4f mat = stack.last().pose();
        var cam = ctx.camera().position();
        Fx.cameraBasis(ctx.camera());
        Tex.ensure();
        Fx.texture(Tex.GLOW);
        MeshBuilder b = Fx.texQuadsDepth();
        float t = System.currentTimeMillis() * 0.001f;
        for (int i = 0, n = stars.getI(); i < n; i++) {
            double az = Vfx.hash(i * 11 + 1) * Math.PI * 2;
            double el = (Vfx.hash(i * 11 + 2) * 0.78 + 0.16) * (Math.PI / 2.0);
            float r = distance.getF() * (0.8f + Vfx.hash(i * 11 + 3) * 0.2f);
            float px = (float) (cam.x + Math.cos(az) * Math.cos(el) * r);
            float py = (float) (cam.y + Math.sin(el) * r);
            float pz = (float) (cam.z + Math.sin(az) * Math.cos(el) * r);
            float flash = (float) (0.6 + 0.4 * Math.sin(t * 1.3 + i * 3.14));
            int alpha = (int) (opacity.getI() * (1 - twinkle.getF() + twinkle.getF() * flash));
            int tint = switch (palette.get()) {
                case "Ice" -> 0xFF9DD9FF;
                case "White" -> 0xFFFFFFFF;
                case "Rainbow" -> Colors.rainbow(Vfx.hash(i * 5));
                default -> Colors.wave(Vfx.hash(i * 5));
            };
            Fx.billboard(b, mat, px, py, pz, size.getF() * (0.6f + Vfx.hash(i * 9) * 0.7f), Colors.alpha(tint, alpha));
        }
        Fx.draw();
    }
}

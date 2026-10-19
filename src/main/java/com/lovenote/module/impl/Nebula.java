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
public final class Nebula extends Module {
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
    private final NumberSetting clouds = add(new NumberSetting("Clouds", 22, 3, 65, 1));
    private final NumberSetting spread = add(new NumberSetting("Spread", 6, 2, 18, 0.5));
    private final NumberSetting size = add(new NumberSetting("Cloud size", 2, 0.3, 7, 0.1));
    private final NumberSetting drift = add(new NumberSetting("Drift speed", 0.25, 0, 2, 0.05));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 50, 5, 130, 5));

    public Nebula() { super("Nebula", Category.VISUALS); describe("Floating atmospheric nebula clouds with adjustable spread, colour and opacity."); }
    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if (mc.player == null) return;
        Matrix4f mat = stack.last().pose();
        Fx.cameraBasis(ctx.camera()); Tex.ensure(); Fx.texture(Tex.SOFT);
        MeshBuilder b = Fx.texQuadsDepth();
        float time = System.currentTimeMillis() * 0.001f * drift.getF();
        for(int i=0, n=clouds.getI(); i<n; ++i) {
            float h1=Vfx.hash(i*7+1), h2=Vfx.hash(i*7+2), h3=Vfx.hash(i*7+3);
            float x=(float)mc.player.getX()+(h1*2-1)*spread.getF()+ (float)Math.sin(time+i)*0.35f;
            float y=(float)mc.player.getY()+h2*5-1.5f;
            float z=(float)mc.player.getZ()+(h3*2-1)*spread.getF();
            Fx.billboard(b,mat,x,y,z,size.getF()*(0.6f+h2),Colors.alpha(tint(h1),opacity.getI()));
        }
        Fx.draw();
    }

}

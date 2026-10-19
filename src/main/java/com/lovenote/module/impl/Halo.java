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
public final class Halo extends Module {
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
    private final NumberSetting radius = add(new NumberSetting("Radius", 0.75, 0.3, 2.5, 0.05));
    private final NumberSetting height = add(new NumberSetting("Head offset", 2.2, 1.2, 4, 0.05));
    private final NumberSetting width = add(new NumberSetting("Width", 0.08, 0.01, 0.3, 0.01));
    private final NumberSetting speed = add(new NumberSetting("Pulse speed", 1.3, 0, 4, 0.1));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 200, 5, 255, 5));

    public Halo() { super("Halo", Category.VISUALS); describe("Animated neon ring above the player with size, height and opacity settings."); }
    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if(mc.player==null) return;
        Matrix4f mat=stack.last().pose();
        MeshBuilder b=Fx.colorQuadsDepth();
        double t=System.currentTimeMillis()*.001*speed.get();
        double r=radius.get()*(1+Math.sin(t)*0.06);
        int c=Colors.alpha(tint((float)((Math.sin(t)+1)*.5)),opacity.getI());
        Vfx.ringFlat(b,mat,mc.player.getX(),mc.player.getY()+height.get(),mc.player.getZ(),
            r,r+width.get(),60,c,Colors.alpha(c,0));
        Fx.draw();
    }

}

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
public final class GroundRings extends Module {
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
    private final NumberSetting rings = add(new NumberSetting("Rings", 3, 1, 8, 1));
    private final NumberSetting radius = add(new NumberSetting("Radius", 2, 0.5, 9, 0.1));
    private final NumberSetting width = add(new NumberSetting("Ring width", 0.12, 0.02, 0.5, 0.02));
    private final NumberSetting speed = add(new NumberSetting("Pulse speed", 0.7, 0, 3, 0.05));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 135, 5, 255, 5));

    public GroundRings() { super("GroundRings", Category.VISUALS); describe("Pulsating neon circles around the player at ground level."); }
    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if(mc.player==null) return;
        Matrix4f mat=stack.last().pose();
        MeshBuilder b=Fx.colorQuadsDepth();
        double t=System.currentTimeMillis()*.001*speed.get();
        for(int i=0,n=rings.getI();i<n;i++) {
            double r=radius.get()*(i+1.)/n+Math.sin(t+i*1.2)*0.12;
            int color=Colors.alpha(tint(i/(float)n),opacity.getI());
            Vfx.ringFlat(b,mat,mc.player.getX(),mc.player.getY()+0.055+i*0.003,
                mc.player.getZ(),r,r+width.get(),48,color,Colors.alpha(color,0));
        }
        Fx.draw();
    }

}

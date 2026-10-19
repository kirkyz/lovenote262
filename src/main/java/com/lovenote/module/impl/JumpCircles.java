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
public final class JumpCircles extends Module {
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
    private final NumberSetting duration = add(new NumberSetting("Duration (ms)", 1100, 250, 2800, 50));
    private final NumberSetting radius = add(new NumberSetting("Max radius", 2.3, 0.5, 7, 0.1));
    private final NumberSetting width = add(new NumberSetting("Width", 0.14, 0.02, 0.6, 0.02));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 210, 10, 255, 5));

    public JumpCircles() { super("JumpCircles", Category.VISUALS); describe("Expanding luminous ground ring emitted whenever the player jumps."); }
    private long started;
    private double px, py, pz;
    private boolean wasOnGround=true;
    @Override public void onTick() {
        if(mc.player==null) return;
        boolean grounded=mc.player.onGround();
        if(wasOnGround && !grounded && mc.player.getDeltaMovement().y>0.05) {
            started=System.currentTimeMillis(); px=mc.player.getX(); py=mc.player.getY()+.04; pz=mc.player.getZ();
        }
        wasOnGround=grounded;
    }
    @Override public void onDisable() { started=0; wasOnGround=true; }
    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if(started==0) return;
        float phase=(System.currentTimeMillis()-started)/(float)duration.getI();
        if(phase>=1) { started=0; return; }
        Matrix4f mat=stack.last().pose();
        MeshBuilder b=Fx.colorQuadsDepth();
        int c=Colors.alpha(tint(phase),(int)(opacity.getI()*(1-phase)));
        Vfx.ringFlat(b,mat,px,py,pz,Math.max(.01,radius.get()*phase),
            Math.max(.01,radius.get()*phase)+width.get(),48,c,Colors.alpha(c,0));
        Fx.draw();
    }

}

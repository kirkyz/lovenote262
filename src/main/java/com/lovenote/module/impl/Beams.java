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
public final class Beams extends Module {
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
    private final NumberSetting beams = add(new NumberSetting("Beams", 8, 1, 24, 1));
    private final NumberSetting radius = add(new NumberSetting("Spread", 10, 2, 26, 0.5));
    private final NumberSetting height = add(new NumberSetting("Height", 13, 3, 50, 1));
    private final NumberSetting width = add(new NumberSetting("Width", 0.09, 0.01, 0.5, 0.01));
    private final NumberSetting speed = add(new NumberSetting("Spin speed", 0.35, 0, 2.5, 0.05));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 160, 5, 255, 5));

    public Beams() { super("Beams", Category.VISUALS); describe("Animated vertical light pillars surrounding the player."); }
    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if(mc.player==null) return;
        Matrix4f mat=stack.last().pose();
        var cam=ctx.camera().position();
        MeshBuilder b=Fx.colorQuadsDepth();
        double t=System.currentTimeMillis()*.001*speed.get();
        for(int i=0,n=beams.getI();i<n;i++) {
            double a=t+i*Math.PI*2/n;
            double x=mc.player.getX()+Math.cos(a)*radius.get();
            double y=mc.player.getY()-1;
            double z=mc.player.getZ()+Math.sin(a)*radius.get();
            int c=Colors.alpha(tint(i/(float)n),opacity.getI());
            Vfx.tube(b,mat,x,y,z,x+Math.sin(a)*1.2,y+height.get(),z+Math.cos(a)*1.2,
                width.getF(),c,Colors.alpha(c,0),cam.x,cam.y,cam.z);
        }
        Fx.draw();
    }

}

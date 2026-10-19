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
public final class ParticleField extends Module {
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
    private final NumberSetting count = add(new NumberSetting("Particles", 65, 5, 200, 5));
    private final NumberSetting range = add(new NumberSetting("Range", 12, 3, 30, 1));
    private final NumberSetting size = add(new NumberSetting("Size", 0.18, 0.03, 0.8, 0.02));
    private final NumberSetting speed = add(new NumberSetting("Drift speed", 0.4, 0, 3, 0.05));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 180, 5, 255, 5));

    public ParticleField() { super("ParticleField", Category.VISUALS); describe("Client-side star particle field, with independent density, range, colour, size and drift."); }
    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if(mc.player==null) return;
        Matrix4f mat=stack.last().pose();
        Fx.cameraBasis(ctx.camera()); Tex.ensure(); Fx.texture(Tex.STAR);
        MeshBuilder b=Fx.texQuadsDepth();
        float t=System.currentTimeMillis()*0.001f*speed.getF();
        float r=range.getF();
        for(int i=0,n=count.getI();i<n;i++) {
            float a=Vfx.hash(i*13), c=Vfx.hash(i*13+1), d=Vfx.hash(i*13+2);
            float x=(float)mc.player.getX()+(a*2-1)*r;
            float y=(float)mc.player.getY()-2+((c+t*.12f)%1f)*r*.75f;
            float z=(float)mc.player.getZ()+(d*2-1)*r;
            Fx.billboard(b,mat,x,y,z,size.getF()*(0.6f+a),Colors.alpha(tint(a),opacity.getI()));
        }
        Fx.draw();
    }

}

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
import com.lovenote.util.WorldFxGrid;
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
    private final ModeSetting shape = add(new ModeSetting("Shape", "Soft glow", "Soft glow", "Star"));
    private final NumberSetting count = add(new NumberSetting("Particles", 65, 5, 200, 5));
    private final NumberSetting range = add(new NumberSetting("Range", 12, 3, 30, 1));
    private final NumberSetting size = add(new NumberSetting("Size", 0.24, 0.03, 0.8, 0.02));
    private final NumberSetting speed = add(new NumberSetting("Drift speed", 0.4, 0, 3, 0.05));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 180, 5, 255, 5));

    public ParticleField() { super("ParticleField", Category.VISUALS); describe("Client-side star particle field, with independent density, range, colour, size and drift."); }
    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if (mc.player == null || mc.level == null) return;
        double r = range.get(), span = Math.max(2.0, r*0.7);
        double ox=mc.player.getX(), oy=mc.player.getY()+1, oz=mc.player.getZ();
        int gx=WorldFxGrid.cell(ox,span),gy=WorldFxGrid.cell(oy,span),gz=WorldFxGrid.cell(oz,span);
        Matrix4f mat=stack.last().pose();
        Fx.cameraBasis(ctx.camera()); Tex.ensure(); Fx.texture(shape.is("Star") ? Tex.STAR : Tex.SOFT);
        MeshBuilder b=Fx.texQuadsDepth();
        double t=System.nanoTime()*1e-9*speed.get();
        int n=VisualOptimizer.budget(count.getI(), 5);
        for (int i=0;i<n;i++) {
            int tile=(i*11)%27;
            int ix=gx+(tile%3)-1, iy=gy+((tile/3)%3)-1, iz=gz+((tile/9)%3)-1;
            int seed=i/27;
            float a=WorldFxGrid.hash(ix,iy,iz,seed*7+1), c=WorldFxGrid.hash(ix,iy,iz,seed*7+2),d=WorldFxGrid.hash(ix,iy,iz,seed*7+3);
            double x=(ix+a)*span + Math.sin(t*0.42+c*6.28)*0.20;
            double y=(iy+c)*span + Math.sin(t*0.6+a*6.28)*0.26;
            double z=(iz+d)*span + Math.cos(t*0.38+d*6.28)*0.20;
            float fade=WorldFxGrid.edgeFade(x-ox,y-oy,z-oz,r);
            float twinkle=0.74f+0.26f*(float)Math.sin(t*1.15+i*2.1);
            int alpha=(int)(opacity.getI()*fade*twinkle);
            if(alpha < 5)continue;
            Fx.billboard(b,mat,(float)x,(float)y,(float)z,size.getF()*(0.7f+a*0.65f),Colors.alpha(tint(a),alpha));
        }
        Fx.draw();
    }
}

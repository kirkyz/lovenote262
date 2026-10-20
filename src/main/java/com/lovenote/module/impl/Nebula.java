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
        if(mc.player==null || mc.level==null)return;
        double r=spread.get()*1.6,span=Math.max(3.0,r*0.7);
        double ox=mc.player.getX(),oy=mc.player.getY()+1,oz=mc.player.getZ();
        int gx=WorldFxGrid.cell(ox,span),gy=WorldFxGrid.cell(oy,span),gz=WorldFxGrid.cell(oz,span);
        Matrix4f mat=stack.last().pose();Fx.cameraBasis(ctx.camera());Tex.ensure();Fx.texture(Tex.SOFT);
        MeshBuilder b=Fx.texQuadsDepth();
        double t=System.nanoTime()*1e-9*drift.get();
        int n=VisualOptimizer.budget(clouds.getI(),3);
        for(int i=0;i<n;i++) {
            int tile=(i*11)%27;
            int ix=gx+tile%3-1,iy=gy+(tile/3)%3-1,iz=gz+(tile/9)%3-1,seed=i/27;
            float a=WorldFxGrid.hash(ix,iy,iz,seed*17+1),c=WorldFxGrid.hash(ix,iy,iz,seed*17+2),d=WorldFxGrid.hash(ix,iy,iz,seed*17+3);
            double x=(ix+a)*span+Math.sin(t*0.36+i)*0.45;
            double y=(iy+c)*span+Math.sin(t*0.23+c*6.28)*0.3;
            double z=(iz+d)*span+Math.cos(t*0.31+i)*0.45;
            float fade=WorldFxGrid.edgeFade(x-ox,y-oy,z-oz,r);
            int alpha=(int)(opacity.getI()*fade*(0.7+0.3*Math.sin(t*0.6+a*6.28)));
            if(alpha<4)continue;
            Fx.billboard(b,mat,(float)x,(float)y,(float)z,size.getF()*(0.6f+c*0.8f),Colors.alpha(tint(a),alpha));
        }
        Fx.draw();
    }
}

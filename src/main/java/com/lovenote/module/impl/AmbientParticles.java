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

/** Native 26.2 ambient particles, lightweight and client-side only. */
public final class AmbientParticles extends Module {
    private final ModeSetting palette = add(new ModeSetting("Palette", "Theme", "Theme", "Rainbow", "Ice", "White"));
    private final NumberSetting count = add(new NumberSetting("Particles", 24, 1, 100, 1));
    private final NumberSetting radius = add(new NumberSetting("Radius", 7, 2, 20, 0.5));
    private final NumberSetting speed = add(new NumberSetting("Float speed", 0.5, 0, 2.5, 0.05));
    private final NumberSetting size = add(new NumberSetting("Size", 0.28, 0.05, 1.2, 0.02));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 145, 10, 255, 5));

    public AmbientParticles() {
        super("AmbientParticles", Category.VISUALS);
        describe("Client-side glowing motes floating nearby: density, speed, size, opacity, palette.");
    }

    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if(mc.player==null || mc.level==null)return;
        double r=radius.get(),span=Math.max(2.0,r*0.7);
        double ox=mc.player.getX(),oy=mc.player.getY()+1,oz=mc.player.getZ();
        int gx=WorldFxGrid.cell(ox,span),gy=WorldFxGrid.cell(oy,span),gz=WorldFxGrid.cell(oz,span);
        Matrix4f mat=stack.last().pose();
        Fx.cameraBasis(ctx.camera());Tex.ensure();Fx.texture(Tex.SOFT);
        MeshBuilder b=Fx.texQuadsDepth();
        double t=System.nanoTime()*1e-9*speed.get();
        int n=VisualOptimizer.budget(count.getI(),3);
        for(int i=0;i<n;i++) {
            int tile=(i*11)%27;
            int ix=gx+tile%3-1,iy=gy+(tile/3)%3-1,iz=gz+(tile/9)%3-1,seed=i/27;
            float a=WorldFxGrid.hash(ix,iy,iz,seed*11+1), c=WorldFxGrid.hash(ix,iy,iz,seed*11+2),d=WorldFxGrid.hash(ix,iy,iz,seed*11+3);
            double x=(ix+a)*span + Math.sin(t*0.66+a*6)*0.28;
            double y=(iy+c)*span + Math.sin(t*0.95+d*6)*0.37;
            double z=(iz+d)*span + Math.cos(t*0.48+c*6)*0.28;
            int tint=switch(palette.get()) {
                case "Rainbow" -> Colors.rainbow(a+(float)t*0.025f);
                case "Ice" -> 0xFF9AE8FF;
                case "White" -> 0xFFFFFFFF;
                default -> Colors.wave(a);
            };
            float fade=WorldFxGrid.edgeFade(x-ox,y-oy,z-oz,r);
            int alpha=(int)(opacity.getI()*fade*(0.7+0.3*Math.sin(t*1.5+a*6.28)));
            if(alpha<5)continue;
            Fx.billboard(b,mat,(float)x,(float)y,(float)z,size.getF()*(0.6f+c*0.65f),Colors.alpha(tint,alpha));
        }
        Fx.draw();
    }
}

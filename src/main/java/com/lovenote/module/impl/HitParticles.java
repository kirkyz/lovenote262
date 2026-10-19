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
public final class HitParticles extends Module {
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
    private final NumberSetting count = add(new NumberSetting("Particles", 20, 4, 80, 1));
    private final NumberSetting duration = add(new NumberSetting("Duration (ms)", 900, 150, 2500, 50));
    private final NumberSetting scatter = add(new NumberSetting("Scatter", 2.4, 0.3, 8, 0.1));
    private final NumberSetting size = add(new NumberSetting("Size", 0.2, 0.03, 0.9, 0.02));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 215, 5, 255, 5));

    public HitParticles() { super("HitParticles", Category.VISUALS); describe("Glowing hit sparks on confirmed attack events with adjustable scatter and lifetime."); }
    private long started;
    private double hx,hy,hz;
    @Override public void onAttack(net.minecraft.world.entity.Entity target) {
        if(target==null) return;
        hx=target.getX(); hy=target.getY()+target.getBbHeight()*.55; hz=target.getZ();
        started=System.currentTimeMillis();
    }
    @Override public void onDisable() { started=0; }
    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if(started==0) return;
        float phase=(System.currentTimeMillis()-started)/(float)duration.getI();
        if(phase>=1) {started=0; return;}
        Matrix4f mat=stack.last().pose();
        Fx.cameraBasis(ctx.camera()); Tex.ensure(); Fx.texture(Tex.STAR);
        MeshBuilder b=Fx.texQuadsDepth();
        for(int i=0,n=count.getI();i<n;i++) {
            double a=Vfx.hash(i*31)*Math.PI*2, e=Vfx.hash(i*31+1)*Math.PI-1.57;
            double r=scatter.get()*phase*(.2+.8*Vfx.hash(i*31+2));
            Fx.billboard(b,mat,(float)(hx+Math.cos(a)*Math.cos(e)*r),
                (float)(hy+Math.sin(e)*r-phase*phase),(float)(hz+Math.sin(a)*Math.cos(e)*r),
                size.getF()*(1-phase*.5f),Colors.alpha(tint(i/(float)n),
                    (int)(opacity.getI()*(1-phase))));
        }
        Fx.draw();
    }

}

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

import java.util.ArrayDeque;

/** Native 26.2 visual effect; no server packets or world mutation. */
public final class GhostTrail extends Module {
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
    private final NumberSetting life = add(new NumberSetting("Lifetime (ms)", 1500, 200, 4500, 100));
    private final NumberSetting size = add(new NumberSetting("Glow size", 0.45, 0.08, 1.5, 0.05));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 140, 5, 255, 5));
    private static final class Sample {
        final double x, y, z; final long time;
        Sample(double x, double y, double z, long time) {
            this.x=x; this.y=y; this.z=z; this.time=time;
        }
    }
    private final ArrayDeque<Sample> history=new ArrayDeque<>();
    @Override public void onTick() {
        if(mc.player==null || mc.level==null) {history.clear(); return;}
        long now=System.currentTimeMillis();
        Sample last=history.peekLast();
        double x=mc.player.getX(), y=mc.player.getY()+.15, z=mc.player.getZ();
        if(last==null || Math.hypot(x-last.x,z-last.z)>.13 || Math.abs(y-last.y)>.15)
            history.addLast(new Sample(x,y,z,now));
        while(history.size()>180 || (!history.isEmpty() && now-history.peekFirst().time>life.getI()))
            history.removeFirst();
    }
    @Override public void onDisable() {history.clear();}

    public GhostTrail() { super("GhostTrail", Category.VISUALS); describe("Fading glow footprints left behind while moving."); }
    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if(history.isEmpty()) return;
        Matrix4f mat=stack.last().pose();
        Fx.cameraBasis(ctx.camera()); Tex.ensure(); Fx.texture(Tex.GLOW);
        MeshBuilder b=Fx.texQuadsDepth();
        long now=System.currentTimeMillis();
        for(Sample s:history) {
            float age=Math.min(1,(now-s.time)/(float)life.getI());
            Fx.billboard(b,mat,(float)s.x,(float)s.y,(float)s.z,size.getF()*(1-age*.5f),
                Colors.alpha(tint(age),(int)(opacity.getI()*(1-age))));
        }
        Fx.draw();
    }

}

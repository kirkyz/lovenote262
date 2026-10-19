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
public final class Trail extends Module {
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
    private final NumberSetting life = add(new NumberSetting("Lifetime (ms)", 1300, 200, 4500, 100));
    private final NumberSetting width = add(new NumberSetting("Width", 0.1, 0.02, 0.6, 0.01));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 170, 5, 255, 5));
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

    public Trail() { super("Trail", Category.VISUALS); describe("Continuous fading neon line behind the moving player."); }
    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if(history.size()<2) return;
        Matrix4f mat=stack.last().pose(); var cam=ctx.camera().position();
        MeshBuilder b=Fx.colorQuadsDepth();
        long now=System.currentTimeMillis();
        Sample prev=null;
        for(Sample s:history) {
            if(prev!=null) {
                float age=Math.min(1,(now-s.time)/(float)life.getI());
                int c=Colors.alpha(tint(age),(int)(opacity.getI()*(1-age)));
                Vfx.tube(b,mat,prev.x,prev.y,prev.z,s.x,s.y,s.z,width.getF(),
                    c,c,cam.x,cam.y,cam.z);
            }
            prev=s;
        }
        Fx.draw();
    }

}

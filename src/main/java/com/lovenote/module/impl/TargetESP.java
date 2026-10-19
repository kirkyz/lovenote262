package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Vfx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

/** Client-side target marker, following only your most recently attacked target. */
public final class TargetESP extends Module {
    private final ModeSetting colour = add(new ModeSetting("Colour", "Accent", "Accent", "Rainbow", "Cyan", "Pink", "White"));
    private final NumberSetting seconds = add(new NumberSetting("Duration (seconds)", 4, 1, 15, 1));
    private final NumberSetting scale = add(new NumberSetting("Size", 1.08, 0.5, 2.0, 0.05));
    private final NumberSetting speed = add(new NumberSetting("Rotation speed", 1.0, 0.0, 3.0, 0.1));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 175, 10, 255, 5));
    private LivingEntity target;
    private long expiry;

    public TargetESP() {
        super("TargetESP", Category.VISUALS);
        describe("Animated, depth-tested targeting ring around the last living entity you attack.");
    }
    @Override public void onAttack(Entity entity) {
        if (entity instanceof LivingEntity living) {
            target = living;
            expiry = System.currentTimeMillis() + seconds.getI() * 1000L;
        }
    }
    @Override public void onTick() {
        if (target != null && (System.currentTimeMillis() > expiry || !target.isAlive() || mc.level == null))
            target = null;
    }
    @Override public void onDisable() { target = null; expiry = 0; }
    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if (target == null || mc.player == null || !target.isAlive()) return;
        Matrix4f mat=stack.last().pose();
        float t=System.currentTimeMillis()/1000.0f;
        float fade=Math.min(1f,(expiry-System.currentTimeMillis())/450f);
        int base=switch (colour.get()) {
            case "Rainbow" -> Colors.rainbow(0);
            case "Cyan" -> 0xFF62E8FF;
            case "Pink" -> 0xFFFF78D2;
            case "White" -> 0xFFFFFFFF;
            default -> Colors.wave(0);
        };
        int c=Colors.alpha(base,(int)(opacity.get()*Math.max(0,fade)));
        double x=target.getX(), y=target.getY()+0.045, z=target.getZ();
        double radius=Math.max(0.4, target.getBbWidth()*0.58) * scale.get();
        MeshBuilder mesh=Fx.colorQuadsDepth();
        for (int i=0;i<32;i++) {
            double a=(i/32.0)*Math.PI*2+t*speed.get(), b=((i+1)/32.0)*Math.PI*2+t*speed.get();
            Vfx.tube(mesh,mat,x+Math.cos(a)*radius,y,z+Math.sin(a)*radius,
                    x+Math.cos(b)*radius,y,z+Math.sin(b)*radius,
                    0.022f,c,c,0,0,0);
        }
        Fx.draw();
    }
}

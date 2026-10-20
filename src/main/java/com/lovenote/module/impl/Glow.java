package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
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
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

/** Depth-tested luminous marker rings (not a post-processing outline shader). */
public final class Glow extends Module {
    private final ModeSetting colour = add(new ModeSetting("Colour", "Accent", "Accent", "Cyan", "Pink", "Gold", "Rainbow"));
    private final NumberSetting distance = add(new NumberSetting("Range", 20, 4, 48, 1));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 125, 15, 255, 5));
    private final NumberSetting thickness = add(new NumberSetting("Ring thickness", 0.024, 0.008, 0.09, 0.004));
    private final NumberSetting limit = add(new NumberSetting("Max entities", 14, 1, 40, 1));
    private final BoolSetting playersOnly = add(new BoolSetting("Players only", false));
    private final BoolSetting animate = add(new BoolSetting("Animated pulse", true));
    public Glow() {
        super("Glow", Category.VISUALS);
        describe("Depth-tested coloured aura rings around visible nearby entities; lightweight alternative to shaders.");
    }
    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if(mc.player == null || mc.level == null) return;
        Matrix4f mat = stack.last().pose();
        var cam = ctx.camera().position();
        MeshBuilder mesh = null;
        int count = 0;
        double dist = distance.get() * distance.get();
        double pulse = animate.get() ? 1 + Math.sin(System.nanoTime() * 1e-9 * 2.0) * .06 : 1;
        int cap = VisualOptimizer.budget(limit.getI(), 1);
        for(Entity e : mc.level.entitiesForRendering()) {
            if(e == mc.player || !(e instanceof LivingEntity) || !e.isAlive() || e.distanceToSqr(mc.player) > dist) continue;
            if(playersOnly.get() && !(e instanceof Player)) continue;
            int rgb = switch(colour.get()) {
                case "Cyan" -> 0xFF65EFFF;
                case "Pink" -> 0xFFFF76CF;
                case "Gold" -> 0xFFFFCF6B;
                case "Rainbow" -> Colors.rainbow(count * 0.12f);
                default -> Colors.wave(count * 0.12f);
            };
            int c = Colors.alpha(rgb, opacity.getI());
            if(mesh == null) mesh = Fx.colorQuadsDepth();
            double r = Math.max(0.35, e.getBbWidth() * .55) * pulse;
            double x = e.getX(), y = e.getY() + e.getBbHeight() * .50, z = e.getZ();
            for(int i = 0; i < 20; i++) {
                double a = i * Math.PI / 10, b = (i + 1) * Math.PI / 10;
                Vfx.tube(mesh, mat, x + Math.cos(a)*r, y, z + Math.sin(a)*r,
                    x + Math.cos(b)*r, y, z + Math.sin(b)*r, thickness.getF(), c, c, cam.x, cam.y, cam.z);
            }
            if(++count >= cap) break;
        }
        if(count > 0) Fx.draw();
    }
}

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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;

/** Depth-tested outline of the block currently under the crosshair. */
public final class BlockHighlight extends Module {
    private final ModeSetting colour = add(new ModeSetting("Colour", "Accent", "Accent", "Rainbow", "Cyan", "White", "Pink"));
    private final NumberSetting thickness = add(new NumberSetting("Line width", 0.014, 0.005, 0.055, 0.001));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 190, 30, 255, 5));
    private final NumberSetting expansion = add(new NumberSetting("Expand", 0.004, 0.001, 0.045, 0.001));
    private final NumberSetting pulse = add(new NumberSetting("Pulse", 0.0, 0.0, 1.0, 0.05));

    public BlockHighlight() {
        super("BlockHighlight", Category.VISUALS);
        describe("Terrain-occluded outline of the block under your crosshair, with adjustable colour and thickness.");
    }

    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if (mc.level == null || mc.player == null || !(mc.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) return;
        var pos = hit.getBlockPos();
        if (mc.level.getBlockState(pos).isAir()) return;
        var cam = ctx.camera().position();
        Matrix4f mat = stack.last().pose();
        float now = System.currentTimeMillis() / 1000.0f;
        int tint = switch (colour.get()) {
            case "Rainbow" -> Colors.rainbow(0);
            case "Cyan" -> 0xFF62E8FF;
            case "White" -> 0xFFFFFFFF;
            case "Pink" -> 0xFFFF78D2;
            default -> Colors.wave(0);
        };
        int a = (int) (opacity.get() * (1.0 - pulse.get() * (0.25 + 0.25 * Math.sin(now * 4.0))));
        int c = Colors.alpha(tint, a);
        double d = expansion.get();
        double x0=pos.getX()-d, y0=pos.getY()-d, z0=pos.getZ()-d;
        double x1=pos.getX()+1+d, y1=pos.getY()+1+d, z1=pos.getZ()+1+d;
        double[][] p={{x0,y0,z0},{x1,y0,z0},{x0,y1,z0},{x1,y1,z0},
                {x0,y0,z1},{x1,y0,z1},{x0,y1,z1},{x1,y1,z1}};
        int[][] edges={{0,1},{2,3},{4,5},{6,7},{0,2},{1,3},{4,6},{5,7},{0,4},{1,5},{2,6},{3,7}};
        MeshBuilder mesh = Fx.colorQuadsDepth();
        for (int[] edge : edges) {
            double[] s=p[edge[0]], e=p[edge[1]];
            Vfx.tube(mesh,mat,s[0],s[1],s[2],e[0],e[1],e[2],thickness.getF(),
                    c,c,cam.x,cam.y,cam.z);
        }
        Fx.draw();
    }
}

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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Bounded scan for loaded storage blocks; no x-ray / through-wall rendering. */
public final class StorageESP extends Module {
    private final ModeSetting colour = add(new ModeSetting("Colour", "Accent", "Accent", "Cyan", "Gold", "White", "Rainbow"));
    private final NumberSetting radius = add(new NumberSetting("Scan radius", 10, 3, 15, 1));
    private final NumberSetting vertical = add(new NumberSetting("Vertical radius", 6, 2, 10, 1));
    private final NumberSetting limit = add(new NumberSetting("Max markers", 20, 1, 45, 1));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 185, 10, 255, 5));
    private final NumberSetting width = add(new NumberSetting("Line width", 0.025, 0.005, 0.08, 0.005));
    private final List<BlockPos> storage = new ArrayList<>();
    private int delay;

    public StorageESP() {
        super("StorageESP", Category.VISUALS);
        describe("Highlights nearby loaded chests, barrels and shulker boxes with depth-tested outlines.");
    }
    @Override public void onEnable() { delay=0; }
    @Override public void onDisable() { storage.clear(); delay=0; }
    @Override public void onTick() {
        if(mc.player==null||mc.level==null){storage.clear();return;}
        if(--delay>0)return;
        delay=50;
        storage.clear();
        BlockPos origin=mc.player.blockPosition();
        int r=radius.getI(),v=vertical.getI();
        for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            if(dx*dx+dz*dz>r*r)continue;
            for(int dy=-v;dy<=v;dy++){
                BlockPos pos=origin.offset(dx,dy,dz);
                if(!mc.level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4))continue;
                var block=mc.level.getBlockState(pos).getBlock();
                if(block==Blocks.CHEST||block==Blocks.TRAPPED_CHEST||block==Blocks.ENDER_CHEST
                        ||block==Blocks.BARREL||block instanceof ShulkerBoxBlock){
                    storage.add(pos.immutable());
                    if(storage.size()>=limit.getI())return;
                }
            }
        }
    }
    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if(mc.player==null||mc.level==null||storage.isEmpty())return;
        Matrix4f mat=stack.last().pose();
        var cam=ctx.camera().position();
        MeshBuilder mesh=Fx.colorQuadsDepth();
        int i=0;
        for(BlockPos pos:storage){
            int base=switch(colour.get()){
                case "Cyan" -> 0xFF7DEAFF;
                case "Gold" -> 0xFFFFDA75;
                case "White" -> 0xFFFFFFFF;
                case "Rainbow" -> Colors.rainbow(i*.09f);
                default -> Colors.wave(i*.09f);
            };
            int c=Colors.alpha(base,opacity.getI());
            double x0=pos.getX()+0.015, y0=pos.getY()+0.015,z0=pos.getZ()+0.015;
            double x1=pos.getX()+0.985, y1=pos.getY()+0.985,z1=pos.getZ()+0.985;
            double[][] p={{x0,y0,z0},{x1,y0,z0},{x0,y1,z0},{x1,y1,z0},
                    {x0,y0,z1},{x1,y0,z1},{x0,y1,z1},{x1,y1,z1}};
            int[][] edges={{0,1},{2,3},{4,5},{6,7},{0,2},{1,3},{4,6},{5,7},{0,4},{1,5},{2,6},{3,7}};
            for(int[] edge:edges){
                double[] a=p[edge[0]],b=p[edge[1]];
                Vfx.tube(mesh,mat,a[0],a[1],a[2],b[0],b[1],b[2],
                        width.getF(),c,c,cam.x,cam.y,cam.z);
            }
            i++;
        }
        Fx.draw();
    }
}

package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import java.util.List;
/** Throttled coordinates HUD; only loaded chunks are scanned. */
public final class SpawnerESP extends Module {
    private final NumberSetting radius=add(new NumberSetting("Scan radius",7,2,8,1));
    private int ticks;
    private final List<BlockPos> found=new ArrayList<>();
    public SpawnerESP(){super("SpawnerESP",Category.DONUT);describe("Lists coordinates of nearby loaded spawners. No through-wall rendering yet.");}
    @Override public void onTick(){
        if(mc.player==null||mc.level==null){found.clear();return;}
        if(--ticks>0)return;ticks=80;
        found.clear();int r=radius.getI();BlockPos base=mc.player.blockPosition();
        outer:for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            if(!mc.level.getChunkSource().hasChunk((base.getX()+dx)>>4,(base.getZ()+dz)>>4))continue;
            for(int dy=-r;dy<=r;dy++){
                BlockPos at=base.offset(dx,dy,dz);
                if(mc.level.getBlockState(at).is(Blocks.SPAWNER)){
                    found.add(at.immutable());if(found.size()>=8)break outer;
                }
            }
        }
    }
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(mc.player==null||found.isEmpty())return;
        for(int i=0;i<Math.min(found.size(),4);i++){
            BlockPos p=found.get(i);
            String s="SPAWNER  |  "+p.getX()+"  "+p.getY()+"  "+p.getZ();
            Ui.pill(g,8,255+i*18,Ui.textW(s)+12,18);Ui.text(g,s,14,260+i*18,0xFFBFC8FF);
        }
    }
}

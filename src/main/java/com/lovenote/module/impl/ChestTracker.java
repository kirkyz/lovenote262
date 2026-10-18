package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
/** Non-rendering port: counts nearby containers without guessing whether they were looted. */
public final class ChestTracker extends Module {
    private final NumberSetting radius=add(new NumberSetting("Radius",12,4,24,2));
    private int ticks,found;
    public ChestTracker(){super("ChestTracker",Category.MISC);alsoIn.add(Category.DONUT);describe("Display the number of nearby chests, barrels and shulker boxes.");}
    @Override public void onTick(){
        if(mc.player==null||mc.level==null){found=0;return;}
        if(++ticks%40!=0)return;
        int r=radius.getI(),count=0;
        BlockPos base=mc.player.blockPosition();
        for(BlockPos p:BlockPos.betweenClosed(base.offset(-r,-6,-r),base.offset(r,6,r))){
            var b=mc.level.getBlockState(p).getBlock();
            if(b instanceof ChestBlock||b instanceof BarrelBlock||b instanceof ShulkerBoxBlock)count++;
        }
        found=count;
    }
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(mc.player==null)return;
        String s="Nearby storage: "+found;int px=(int)(Gfx.guiW()*.02),py=(int)(Gfx.guiH()*.67);
        Ui.pill(g,px,py,Ui.textW(s)+12,19);Ui.text(g,s,px+6,py+5,Ui.textCol);
    }
}

package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
/** Local rare-ore alert. No hidden/unloaded chunk or remote data access. */
public final class BlockNotifier extends Module {
 private final NumberSetting radius=add(new NumberSetting("Range",6,2,9,1));
 private final BoolSetting diamonds=add(new BoolSetting("Diamond ore",true));
 private final BoolSetting ancient=add(new BoolSetting("Ancient debris",true));
 private final BoolSetting emeralds=add(new BoolSetting("Emerald ore",false));
 private BlockPos located;private String description="";private int delay;
 public BlockNotifier(){super("BlockNotifier",Category.VISUALS);alsoIn.add(Category.HUD);describe("Show a small local alert for selected ore blocks in loaded chunks.");}
 @Override public void onTick(){
   if(mc.player==null||mc.level==null){located=null;return;}
   if(--delay>0)return;delay=120;int r=radius.getI();BlockPos base=mc.player.blockPosition();
   located=null;description="";
   for(int dy=-r;dy<=r;dy++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
     BlockPos pos=base.offset(dx,dy,dz);
     if(!mc.level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4))continue;
     var s=mc.level.getBlockState(pos);
     String name=null;
     if(diamonds.get()&&(s.is(Blocks.DIAMOND_ORE)||s.is(Blocks.DEEPSLATE_DIAMOND_ORE)))name="DIAMOND";
     else if(ancient.get()&&s.is(Blocks.ANCIENT_DEBRIS))name="ANCIENT DEBRIS";
     else if(emeralds.get()&&(s.is(Blocks.EMERALD_ORE)||s.is(Blocks.DEEPSLATE_EMERALD_ORE)))name="EMERALD";
     if(name!=null){located=pos.immutable();description=name;return;}
   }
 }
 @Override public void onDisable(){located=null;}
 @Override public void onHud(GuiGraphicsExtractor g,float dt){
   if(mc.player==null||located==null)return;
   String s=description+"  "+located.getX()+" "+located.getY()+" "+located.getZ();int width=Ui.textW(s)+14;
   int x=Math.max(2,(Gfx.guiW()-width)/2),y=Gfx.guiH()/4+25;
   Ui.pill(g,x,y,width,20);Ui.text(g,s,x+7,y+6,0xFF86F6D6);
 }
}

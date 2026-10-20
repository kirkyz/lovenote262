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
/** On-screen spawner notification. Throttled, local, loaded chunks only. */
public final class SpawnerNotifier extends Module {
 private final NumberSetting radius=add(new NumberSetting("Range",7,2,10,1));
 private final NumberSetting seconds=add(new NumberSetting("Display seconds",7,2,15,1));
 private final BoolSetting coords=add(new BoolSetting("Show coordinates",true));
 private int timer;private long expires;private BlockPos located;
 public SpawnerNotifier(){super("SpawnerNotifier",Category.VISUALS);alsoIn.add(Category.HUD);describe("Visual notice when a nearby, loaded spawner block is found; no server scans.");}
 @Override public void onTick(){
  if(mc.player==null||mc.level==null){located=null;return;}
  if(--timer>0)return;timer=100;int r=radius.getI();BlockPos base=mc.player.blockPosition();
  // Only check a modest cube, capped at 21^3 samples every five seconds.
  for(int dy=-r;dy<=r;dy++)for(int dz=-r;dz<=r;dz++)for(int dx=-r;dx<=r;dx++){
    BlockPos pos=base.offset(dx,dy,dz);
    if(!mc.level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4))continue;
    if(mc.level.getBlockState(pos).is(Blocks.SPAWNER)){
      if(located==null||!located.equals(pos))expires=System.nanoTime()+seconds.getI()*1_000_000_000L;
      located=pos.immutable();return;
    }
  }
  located=null;
 }
 @Override public void onDisable(){located=null;}
 @Override public void onHud(GuiGraphicsExtractor g,float dt){
    if(mc.player==null||located==null||System.nanoTime()>expires)return;
    String s="SPAWNER NEARBY"+(coords.get()?" · "+located.getX()+" "+located.getY()+" "+located.getZ():"");
    int width=Ui.textW(s)+14,x=Math.max(2,(Gfx.guiW()-width)/2),y=Gfx.guiH()/4;
    Ui.pill(g,x,y,width,20);Ui.text(g,s,x+7,y+6,0xFFFFD48A);
 }
}

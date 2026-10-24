package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
/** Loaded-player and chunk-relative schematic-style region grid; no third-party map tiles. */
public final class RegionMap extends Module {
 private final NumberSetting x=add(new NumberSetting("X",.02,0,.9,.01));
 private final NumberSetting y=add(new NumberSetting("Y",.48,0,.85,.01));
 private final NumberSetting range=add(new NumberSetting("Radius (blocks)",64,16,256,16));
 private final BoolSetting players=add(new BoolSetting("Nearby player dots",true));
 private final BoolSetting coords=add(new BoolSetting("Chunk coordinates",true));
 public RegionMap(){super("RegionMap",Category.HUD);describe("Compact chunk grid with relative player markers, region/chunk label and coordinates. Not terrain mapping.");}
 @Override public void onHud(GuiGraphicsExtractor g,float dt){
  if(mc.player==null||mc.level==null)return;
  int w=120,h=138,px=Math.max(0,Math.min((int)(x.get()*Gfx.guiW()),Gfx.guiW()-w)),py=Math.max(0,Math.min((int)(y.get()*Gfx.guiH()),Gfx.guiH()-h));
  Ui.panel(g,px,py,w,h);Ui.text(g,"REGION / CHUNKS",px+7,py+6,Ui.accent1);
  int centerX=px+60,centerY=py+75,half=47;
  Ui.rect(g,centerX-half,centerY-half,half*2,half*2,0x40121526);
  for(int i=-3;i<=3;i++){int xx=centerX+i*14,yy=centerY+i*14;
    Ui.rect(g,xx,centerY-half,1,half*2,0x307F8FA8);Ui.rect(g,centerX-half,yy,half*2,1,0x307F8FA8);}
  Ui.rect(g,centerX-2,centerY-2,5,5,Ui.accent1);
  if(players.get()){double s=half/range.get();for(Player p:mc.level.players()){
    if(p==mc.player)continue;int xx=centerX+(int)((p.getX()-mc.player.getX())*s),yy=centerY+(int)((p.getZ()-mc.player.getZ())*s);
    if(Math.abs(xx-centerX)<half&&Math.abs(yy-centerY)<half)Ui.rect(g,xx-2,yy-2,4,4,Ui.accent2);
  }}
  if(coords.get()){String label="Chunk "+(mc.player.blockPosition().getX()>>4)+", "+(mc.player.blockPosition().getZ()>>4);
     Ui.text(g,label,px+8,py+123,Ui.textDim);}
 }
}

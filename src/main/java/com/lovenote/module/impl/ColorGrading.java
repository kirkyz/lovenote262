package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
/** Deliberately lightweight colour wash/vignette, not GPU contrast or saturation grading. */
public final class ColorGrading extends Module {
 private final ModeSetting preset=add(new ModeSetting("Colour tone","Cinematic","Cinematic","Warm","Cool","Rose","Noir","None"));
 private final NumberSetting tint=add(new NumberSetting("Tint strength",19,0,75,1));
 private final NumberSetting vignette=add(new NumberSetting("Vignette strength",38,0,120,5));
 private final BoolSetting hideGui=add(new BoolSetting("Hide during menus",true));
 public ColorGrading(){super("ColorGrading",Category.VISUALS);describe("Subtle screen-space colour wash and vignette; GPU shader colour grading requires a separate render pass.");}
 @Override public void onHud(GuiGraphicsExtractor g,float dt){
   if(mc.player==null||mc.level==null||(hideGui.get()&&mc.gui.screen()!=null))return;
   int w=Gfx.guiW(),h=Gfx.guiH();
   int c=switch(preset.get()){case "Warm"->0xFFFFB26F;case "Cool"->0xFF75BDFB;case "Rose"->0xFFFF83B6;
     case "Noir"->0xFF0D111A;case "None"->0x00000000;default->Colors.C3;};
   if(!preset.is("None")&&tint.getI()>0)Ui.rect(g,0,0,w,h,Colors.alpha(c,tint.getI()));
   int edge=Math.max(4,Math.min(w,h)/7);
   for(int i=0;i<edge;i+=4){int a=(int)(vignette.get()*.33*Math.pow(1-i/(double)edge,2));int col=Colors.alpha(0xFF03030D,a);
      Ui.rect(g,0,i,w,4,col);Ui.rect(g,0,h-i-4,w,4,col);
      Ui.rect(g,i,0,4,h,col);Ui.rect(g,w-i-4,0,4,h,col);}
 }
}

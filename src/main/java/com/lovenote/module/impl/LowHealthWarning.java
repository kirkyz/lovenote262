package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
/** Screen-space health vignette. No raw GL shaders or framebuffer copies. */
public final class LowHealthWarning extends Module {
 private final NumberSetting threshold=add(new NumberSetting("Trigger HP %",30,5,85,5));
 private final NumberSetting strength=add(new NumberSetting("Max opacity",100,20,180,5));
 private final BoolSetting pulse=add(new BoolSetting("Pulse",true));
 private final BoolSetting number=add(new BoolSetting("HP readout",true));
 public LowHealthWarning(){super("LowHealthWarning",Category.HUD);describe("Soft red animated edge warning when health drops below the selected percentage.");}
 @Override public void onHud(GuiGraphicsExtractor g,float dt){
   if(mc.player==null||mc.gui.screen()!=null)return;
   double max=Math.max(1,mc.player.getMaxHealth()),hp=Math.max(0,mc.player.getHealth());
   double ratio=hp/max,limit=threshold.get()/100.;if(ratio>=limit)return;
   double urgency=(limit-ratio)/limit;
   int alpha=(int)(strength.get()*urgency*(pulse.get()? .75+.25*Math.sin(System.nanoTime()*1e-9*5):1));
   int w=Gfx.guiW(),h=Gfx.guiH(),edge=Math.max(5,Math.min(w,h)/12);
   for(int i=0;i<edge;i+=3){int a=(int)(alpha*Math.pow(1-i/(double)edge,2));int c=Colors.alpha(0xFFFF303F,a);
     Ui.rect(g,0,i,w,3,c);Ui.rect(g,0,h-i-3,w,3,c);Ui.rect(g,i,0,3,h,c);Ui.rect(g,w-i-3,0,3,h,c);}
   if(number.get()){String t="LOW HEALTH  "+Math.round(hp)+" / "+Math.round(max);int width=Ui.textW(t)+12;
      Ui.pill(g,(w-width)/2,(int)(h*.20),width,19);Ui.text(g,t,(w-width)/2+6,(int)(h*.20)+5,0xFFFFB3BD);}
 }
}

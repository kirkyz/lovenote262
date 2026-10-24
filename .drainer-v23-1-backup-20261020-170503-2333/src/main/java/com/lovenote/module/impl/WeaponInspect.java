package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
/** First-person item inspection overlay; actual 3D model manipulation requires renderer hooks. */
public final class WeaponInspect extends Module {
 private final NumberSetting x=add(new NumberSetting("X",.77,0,.9,.01));
 private final NumberSetting y=add(new NumberSetting("Y",.78,0,.9,.01));
 private final BoolSetting showDurability=add(new BoolSetting("Durability",true));
 public WeaponInspect(){super("WeaponInspect",Category.HUD);describe("Compact held-item preview with name and durability; does not alter 3D hand animations.");}
 @Override public void onHud(GuiGraphicsExtractor g,float delta){
   if(mc.player==null||mc.gui.screen()!=null)return;
   ItemStack held=mc.player.getMainHandItem();if(held.isEmpty())return;
   String s=held.getHoverName().getString();if(s.length()>25)s=s.substring(0,25)+"…";
   int w=Math.min(180,Math.max(112,Ui.textW(s)+32)),h=showDurability.get()?53:36;
   int px=Math.min((int)(x.get()*Gfx.guiW()),Math.max(0,Gfx.guiW()-w)),py=Math.min((int)(y.get()*Gfx.guiH()),Math.max(0,Gfx.guiH()-h));
   Ui.panel(g,px,py,w,h);g.item(held,px+8,py+9);Ui.text(g,s,px+30,py+11,Ui.textCol);
   if(showDurability.get()){
     String d=held.isDamageableItem()?"Durability  "+(held.getMaxDamage()-held.getDamageValue())+" / "+held.getMaxDamage():"Ready";
     Ui.text(g,d,px+10,py+37,Ui.textDim);
   }
 }
}

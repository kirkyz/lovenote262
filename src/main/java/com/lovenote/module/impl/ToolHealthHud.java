package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/** Low-cost held-tool durability warning, useful during Garden and Mining sessions. */
public final class ToolHealthHud extends Module {
    private final NumberSetting warnAt=add(new NumberSetting("Warn below %",20,1,90,1));
    private final NumberSetting x=add(new NumberSetting("X",.03,0,.95,.005));
    private final NumberSetting y=add(new NumberSetting("Y",.76,0,.95,.005));
    public ToolHealthHud(){super("ToolHealthHud",Category.HUD);alsoIn.add(Category.HYPIXEL);
        describe("Held item durability warning without scanning the world. Only appears for damageable tools.");}
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(mc.player==null)return;
        ItemStack stack=mc.player.getMainHandItem();
        if(stack.isEmpty()||!stack.isDamageableItem()||stack.getMaxDamage()<=0)return;
        int pct=Math.max(0,Math.round(100f*(stack.getMaxDamage()-stack.getDamageValue())/stack.getMaxDamage()));
        if(pct>warnAt.getI())return;
        String s="TOOL LOW  "+pct+"%";
        int w=Ui.textW(s)+14,px=Math.max(0,Math.min((int)(x.get()*Gfx.guiW()),Gfx.guiW()-w));
        int py=Math.max(0,Math.min((int)(y.get()*Gfx.guiH()),Gfx.guiH()-19));
        Ui.pill(g,px,py,w,19);Ui.text(g,s,px+7,py+5,0xFFFFBEA9);
    }
}

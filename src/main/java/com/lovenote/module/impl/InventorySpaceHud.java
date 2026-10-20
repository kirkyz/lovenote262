package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Lightweight 27-slot main-inventory availability widget, intentionally not server inventory prediction. */
public final class InventorySpaceHud extends Module {
    private final BoolSetting always=add(new BoolSetting("Always show",false));
    private final NumberSetting alert=add(new NumberSetting("Warn when free slots <=",3,0,20,1));
    private final NumberSetting x=add(new NumberSetting("X",.03,0,.95,.005));
    private final NumberSetting y=add(new NumberSetting("Y",.81,0,.95,.005));
    public InventorySpaceHud(){super("InventorySpaceHud",Category.HUD);alsoIn.add(Category.HYPIXEL);
        describe("Alerts when the 27 non-hotbar inventory slots are nearly full; lightweight and manual.");}
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(mc.player==null)return;
        int filled=0;
        for(int i=9;i<36;i++)if(!mc.player.getInventory().getItem(i).isEmpty())filled++;
        int free=27-filled;
        if(!always.get()&&free>alert.getI())return;
        String s="BAG SPACE  "+free+"/27 FREE";
        int w=Ui.textW(s)+14,px=Math.max(0,Math.min((int)(x.get()*Gfx.guiW()),Gfx.guiW()-w));
        int py=Math.max(0,Math.min((int)(y.get()*Gfx.guiH()),Gfx.guiH()-19));
        Ui.pill(g,px,py,w,19);Ui.text(g,s,px+7,py+5,free<=2?0xFFFFB3B3:0xFFD8C4EF);
    }
}

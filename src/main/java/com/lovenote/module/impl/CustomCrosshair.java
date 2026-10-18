package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
/** Custom themed crosshair overlay; vanilla crosshair may still be visible. */
public final class CustomCrosshair extends Module {
    private final ModeSetting style=add(new ModeSetting("Style","Cross","Cross","Dot","Circle"));
    private final NumberSetting size=add(new NumberSetting("Size",6,2,16,1));
    private final NumberSetting thickness=add(new NumberSetting("Thickness",1,1,3,1));
    private final BoolSetting animate=add(new BoolSetting("Expand on attack",true));
    private int pop;
    public CustomCrosshair(){super("CustomCrosshair",Category.VISUALS);describe("Colour-matched crosshair overlay with animation.");}
    @Override public void onAttack(Entity target){if(target!=null&&animate.get())pop=6;}
    @Override public void onTick(){if(pop>0)pop--;}
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(mc.player==null||mc.gui.screen()!=null)return;
        int cx=Gfx.guiW()/2,cy=Gfx.guiH()/2,t=thickness.getI(),s=size.getI()+(pop>0?pop/2:0),col=Ui.accent1;
        if(style.is("Dot")){Ui.rect(g,cx-t,cy-t,2*t+1,2*t+1,col);return;}
        if(style.is("Circle")){
            for(int i=0;i<40;i++){double a=i*Math.PI*2/40;Ui.rect(g,cx+(int)(Math.cos(a)*s),cy+(int)(Math.sin(a)*s),t,t,col);}return;
        }
        Ui.rect(g,cx-s-3,cy,t>1?s:Math.max(1,s),t,col);
        Ui.rect(g,cx+3,cy,s,t,col);
        Ui.rect(g,cx,cy-s-3,t,s,col);
        Ui.rect(g,cx,cy+3,t,s,col);
    }
}

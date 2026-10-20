package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;

/** Port: short, inexpensive on-screen pulse on a local attack action, not a server-confirmed hit. */
public final class Shockwave extends Module {
    private final NumberSetting intensity=add(new NumberSetting("Pulse radius",16,8,35,1));
    private final NumberSetting duration=add(new NumberSetting("Pulse duration (ms)",260,100,800,20));
    private long began;
    public Shockwave(){super("Shockwave",Category.RENDER);describe("Small crosshair pulse on an attack action; no fullscreen effect or shader.");}
    @Override public void onAttack(Entity target){ if(target!=null) began=System.nanoTime(); }
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(mc.player==null||began==0)return;
        double progress=(System.nanoTime()-began)/(duration.get()*1_000_000.0);
        if(progress>=1.0||progress<0)return;
        int radius=(int)(5+intensity.get()*progress),cx=Gfx.guiW()/2,cy=Gfx.guiH()/2;
        int alpha=Math.max(0,Math.min(150,(int)(140*(1-progress))));
        int c=(alpha<<24)|0xC9A3F0;
        Ui.rect(g,cx-radius,cy-radius,radius*2,1,c);
        Ui.rect(g,cx-radius,cy+radius,radius*2,1,c);
        Ui.rect(g,cx-radius,cy-radius,1,radius*2,c);
        Ui.rect(g,cx+radius,cy-radius,1,radius*2,c);
    }
}

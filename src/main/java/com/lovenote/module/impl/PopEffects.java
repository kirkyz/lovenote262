package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;

/** Port: lightweight totem-pop activity readout from already wired client events. */
public final class PopEffects extends Module {
    private final BoolSetting hud=add(new BoolSetting("Show pop notice",true));
    private final NumberSetting seconds=add(new NumberSetting("Notice seconds",3,1,8,1));
    private String recent="";
    private long until;
    private int sessionPops;
    public PopEffects(){super("PopEffects",Category.RENDER);alsoIn.add(Category.HUD);
        describe("Shows the most recent observed totem pop and a session count; no extra particles or remote data.");}
    @Override public void onTotemPop(Entity entity){
        if(entity==null)return;
        sessionPops++;
        String name=entity.getName().getString();
        recent=name.length()>32?name.substring(0,32):name;
        until=System.nanoTime()+seconds.getI()*1_000_000_000L;
    }
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(mc.player==null||!hud.get()||System.nanoTime()>until||recent.isEmpty())return;
        String message="Totem: "+recent+" | "+sessionPops+" this session";
        if(message.length()>57)message=message.substring(0,54)+"...";
        int width=Ui.textW(message)+12;
        int x=Math.max(0,Math.min((Gfx.guiW()-width)/2,Gfx.guiW()-width));
        int y=Math.max(0,(int)(Gfx.guiH()*.16));
        Ui.pill(g,x,y,width,19);Ui.text(g,message,x+6,y+5,0xFFD7B8F7);
    }
}

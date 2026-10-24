package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.HudWidgetBounds;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Lightweight configurable Drainer status bar. */
public final class DrainerInfoBar extends Module implements HudWidgetBounds {
    private final NumberSetting x=add(new NumberSetting("X",.012,0,.98,.005));
    private final NumberSetting y=add(new NumberSetting("Y",.012,0,.98,.005));
    private final NumberSetting scale=add(new NumberSetting("Scale",1,.5,1.5,.05));
    private final BoolSetting logo=add(new BoolSetting("Brand",true));
    private final BoolSetting fps=add(new BoolSetting("FPS",true));
    private final BoolSetting time=add(new BoolSetting("Clock",true));
    private final BoolSetting server=add(new BoolSetting("Server",true));
    private final BoolSetting separated=add(new BoolSetting("Segmented",true));
    private static final DateTimeFormatter CLOCK=DateTimeFormatter.ofPattern("HH:mm");
    public DrainerInfoBar(){super("DrainerInfoBar",Category.HUD);describe("Modular compact status bar: brand, FPS, clock and server.");}
    private int drawnX,drawnY,drawnW=80,drawnH=17;
    @Override public int editorX(){return drawnX;}
    @Override public int editorY(){return drawnY;}
    @Override public int editorWidth(){return drawnW;}
    @Override public int editorHeight(){return drawnH;}
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(mc.player==null)return;
        String[] data={logo.get()?"✦ DRAINER":null,fps.get()?mc.getFps()+" FPS":null,
            time.get()?LocalTime.now().format(CLOCK):null,
            server.get()?(mc.getCurrentServer()==null?"Singleplayer":mc.getCurrentServer().ip):null};
        int pad=6,gap=separated.get()?4:1,h=17;
        int total=0,count=0;
        for(String item:data)if(item!=null){total+=Ui.textW(item)+pad*2;count++;}
        if(count==0)return;
        total+=gap*(count-1);
        int px=Math.max(0,Math.min((int)(x.get()*Gfx.guiW()),Math.max(0,Gfx.guiW()-total)));
        int py=Math.max(0,Math.min((int)(y.get()*Gfx.guiH()),Math.max(0,Gfx.guiH()-h)));
        drawnX=px;drawnY=py;drawnW=total;drawnH=h;
        int left=px;
        for(String item:data){
            if(item==null)continue;
            int w=Ui.textW(item)+pad*2;
            Ui.rrect(g,left,py,w,h,4,0xE90D0B16);
            Ui.rect(g,left+3,py,w-6,1,0x805A3477);
            Ui.text(g,item,left+pad,py+4,item.startsWith("✦")?Ui.accent1:0xFFE7DDED);
            left+=w+gap;
        }
    }
}

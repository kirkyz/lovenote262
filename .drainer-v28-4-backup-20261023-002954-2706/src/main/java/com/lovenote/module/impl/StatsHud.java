package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.HudGlass;
import com.lovenote.util.HudWidgetBounds;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Honest live FPS, connection ping and computer time (no fabricated TPS). */
public final class StatsHud extends Module implements HudWidgetBounds {
    private final NumberSetting x = add(new NumberSetting("X", .015, 0, .98, .005));
    private final NumberSetting y = add(new NumberSetting("Y", .15, 0, .98, .005));
    private final BoolSetting frame = add(new BoolSetting("Frame", true));
    private final NumberSetting opacity = add(new NumberSetting("Panel opacity", 205, 30, 255, 5));
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private int px, py;
    private long nextClockUpdate;
    private String clock = "--:--";
    private static final int W = 151, H = 44;

    public StatsHud() {
        super("StatsHud", Category.HUD);
        describe("Native Drainer stats panel: real FPS, server ping and local clock.");
    }
    @Override public int editorX() {return px;}
    @Override public int editorY() {return py;}
    @Override public int editorWidth() {return W;}
    @Override public int editorHeight() {return H;}

    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null) return;
        px = Math.max(0,Math.min((int)(x.get()*Gfx.guiW()),Math.max(0,Gfx.guiW()-W)));
        py = Math.max(0,Math.min((int)(y.get()*Gfx.guiH()),Math.max(0,Gfx.guiH()-H)));
        if(frame.get()) HudGlass.card(g,px,py,W,H,opacity.getI());
        long now=System.currentTimeMillis();
        if(now>=nextClockUpdate) {clock=LocalTime.now().format(CLOCK);nextClockUpdate=now+1000;}
        String fps=Integer.toString(mc.getFps());
        String ping="—";
        if(mc.getCurrentServer()!=null && mc.getConnection()!=null) {
            PlayerInfo info=mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if(info!=null) ping=Integer.toString(Math.max(0,info.getLatency()));
        }
        metric(g,px+9,py+9,"FPS",fps,HudGlass.accent());
        metric(g,px+59,py+9,"PING",ping,0xFFB5D8EF);
        metric(g,px+103,py+9,"TIME",clock,0xFFE2E9FD);
        // Slim baseline, not another full-height framed card.
        HudGlass.progress(g,px+9,py+36,133,2,Math.min(1.0,mc.getFps()/240.0),HudGlass.accent());
    }
    private static void metric(GuiGraphicsExtractor g,int x,int y,String title,String value,int color) {
        Ui.text(g,HudGlass.fit(value,53),x,y,color);
        Ui.text(g,title,x,y+14,HudGlass.muted());
    }
}

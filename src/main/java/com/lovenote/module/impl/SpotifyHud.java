package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.spotify.SpotifyManager;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Drainer gothic floating media card. */
public final class SpotifyHud extends Module {
    private final NumberSetting x = add(new NumberSetting("X", 0.02, 0, 0.98, 0.005));
    private final NumberSetting y = add(new NumberSetting("Y", 0.72, 0, 0.98, 0.005));
    private final NumberSetting size = add(new NumberSetting("Scale", 1.0, 0.35, 1.6, 0.05));
    private final BoolSetting compact = add(new BoolSetting("Compact", false));
    private final BoolSetting hideIdle = add(new BoolSetting("Hide when idle", true));
    private final BoolSetting showTime = add(new BoolSetting("Timestamps", true));
    private final BoolSetting showControls = add(new BoolSetting("Control hints", true));

    public SpotifyHud() {
        super("SpotifyHud", Category.HUD);
        describe("Drainer gothic floating Now Playing card. X, Y and Scale are independent.");
    }
    @Override public void onEnable() { SpotifyManager.I.start(); }

    private static String fit(String s,int width){if(s==null)return "";if(Ui.textW(s)<=width)return s;while(!s.isEmpty()&&Ui.textW(s+"...")>width)s=s.substring(0,s.length()-1);return s+"...";}
    private static String clipped(String s, int max) {
        if (s == null || s.isBlank()) return "Unknown";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }
    private static String clock(long ms) {
        long sec = Math.max(0, ms / 1000);
        return (sec / 60) + ":" + String.format(java.util.Locale.ROOT, "%02d", sec % 60);
    }
    private static int blend(int a, int b, double t) {
        int r = (int)(((a >>> 16) & 255) * (1-t) + ((b >>> 16) & 255) * t);
        int g = (int)(((a >>> 8) & 255) * (1-t) + ((b >>> 8) & 255) * t);
        int bl = (int)((a & 255) * (1-t) + (b & 255) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }
    private final BoolSetting artwork = add(new BoolSetting("Album artwork", true));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 92, 30, 100, 5));
    private int px, py, w, h, controlY, controlX, barX, barY, barW;
    private boolean dragging;
    private double dragX, dragY;

    public boolean click(double mx, double my, boolean pressed) {
        if (!enabled) return false;
        if (!pressed) { dragging = false; return false; }
        if (mx < px || mx >= px+w || my < py || my >= py+h) return false;
        SpotifyManager m = SpotifyManager.I;
        if (my >= barY-4 && my <= barY+8 && mx >= barX && mx <= barX+barW && m.durationMs>0) {
            m.seek((long)(m.durationMs * (mx-barX)/Math.max(1,barW))); return true;
        }
        if (my >= controlY-7 && my <= controlY+13) {
            if (mx >= controlX-9 && mx < controlX+13) { m.previous(); return true; }
            if (mx >= controlX+15 && mx < controlX+39) { m.togglePlay(); return true; }
            if (mx >= controlX+40 && mx < controlX+65) { m.next(); return true; }
        }
        dragging = true; dragX = mx-px; dragY = my-py; return true;
    }
    public void move(double mx, double my, boolean down) {
        if (!down) { dragging = false; return; }
        if (dragging) {
            x.set(Math.max(0, mx-dragX)/Math.max(1,Gfx.guiW()));
            y.set(Math.max(0, my-dragY)/Math.max(1,Gfx.guiH()));
        }
    }
    @Override public void onHud(GuiGraphicsExtractor g, float tickDelta) {
        SpotifyManager m = SpotifyManager.I;
        if (!m.hasTrack && hideIdle.get()) return;
        double sc = Math.max(0.78, size.get() * Math.max(0.5, Ui.scale));
        boolean small = compact.get();
        w = (int)((small ? 246 : 315) * sc);
        h = (int)((small ? 65 : 94) * sc);
        px = Math.max(0, Math.min((int)(x.get()*Gfx.guiW()),Math.max(0,Gfx.guiW()-w)));
        py = Math.max(0, Math.min((int)(y.get()*Gfx.guiH()),Math.max(0,Gfx.guiH()-h)));
        int alpha = (int)(opacity.get()*2.55);
        int radius = Math.max(3,(int)(8*sc));
        Ui.rrect(g,px-1,py-1,w+2,h+2,radius+1, (Math.min(75,alpha/4)<<24)|(Ui.accent1&0xFFFFFF));
        Ui.rrect(g,px,py,w,h,radius,(alpha<<24)|0x0E0B15);
        Ui.rect(g,px+7,py+2,w-14,1,0x905E367B);
        // Thin outline only: no thick side accent.
        int art=(int)((small?45:65)*sc), ax=px+(int)(10*sc),ay=py+(h-art)/2;
        Ui.rrect(g,ax-1,ay-1,art+2,art+2,Math.max(2,(int)(5*sc)),0xFF6D388F);
        Ui.rrect(g,ax,ay,art,art,Math.max(2,(int)(4*sc)),0xFF201629);
        if (artwork.get() && m.artTexture != null) Gfx.blit(g,m.artTexture,ax,ay,0,0,art,art,art,art,art,art);
        else Ui.text(g,"✦",ax+art/2-4,ay+art/2-5,Ui.accent1);
        int tx=ax+art+(int)(11*sc);
        controlX=px+w-(int)((small?80:92)*sc);
        int max=small?20:28;
        Ui.text(g,fit(m.hasTrack?m.title:m.status,Math.max(30,controlX-tx-6)),tx,py+(int)(small?10*sc:13*sc),0xFFF5EDF9);
        Ui.text(g,fit(m.hasTrack?m.artist:"Windows media",Math.max(30,controlX-tx-6)),tx,py+(int)(small?24*sc:27*sc),0xFFAE95BB);
        controlX=px+w-(int)((small?80:92)*sc);
        controlY=py+(int)((small?25:32)*sc);
        if(showControls.get()) {
            Ui.text(g,"|<",controlX,controlY,0xFFC6A4D9);
            Ui.text(g,m.playing?"II":">",controlX+(int)(26*sc),controlY,Ui.accent1);
            Ui.text(g,">|",controlX+(int)(52*sc),controlY,0xFFC6A4D9);
        }
        barX=tx;barY=py+h-(int)((small?14:25)*sc);
        barW=Math.max(20,w-(tx-px)-(int)(12*sc));
        Ui.rrect(g,barX,barY,barW,Math.max(2,(int)(3*sc)),2,0xFF3B2B48);
        if(m.durationMs>0) Ui.rrect(g,barX,barY,(int)(barW*Math.min(1,(double)m.progress()/m.durationMs)),Math.max(2,(int)(3*sc)),2,Ui.accent1);
        if(!small && showTime.get()) {
            Ui.text(g,clock(m.progress()),barX,barY+(int)(7*sc),0xFF9B83AC);
            String t=clock(m.durationMs);
            Ui.text(g,t,barX+barW-Ui.textW(t),barY+(int)(7*sc),0xFF9B83AC);
        }
    }
}

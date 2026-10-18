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
    @Override public void onHud(GuiGraphicsExtractor g, float tickDelta) {
        SpotifyManager m = SpotifyManager.I;
        if (!m.hasTrack && hideIdle.get()) return;
        double sc = size.get() * Math.max(0.5, Ui.scale);
        boolean small = compact.get();
        int w = (int)((small ? 200 : 260) * sc);
        int h = (int)((small ? 55 : 88) * sc);
        int px = Math.max(0, Math.min((int)(x.get() * Gfx.guiW()), Math.max(0, Gfx.guiW()-w)));
        int py = Math.max(0, Math.min((int)(y.get() * Gfx.guiH()), Math.max(0, Gfx.guiH()-h)));
        int r = Math.max(2, (int)(7*sc));
        Ui.rrect(g, px-2, py-2, w+4, h+4, r+2, 0x503A1A53);
        Ui.rrect(g, px, py, w, h, r, 0xE90D0917);
        Ui.rect(g, px+3, py+2, w-6, 1, 0xAA713F94);
        Ui.rect(g, px+2, py+6, 1, h-12, 0xB07E3BAC);
        Ui.rect(g, px+w-3, py+6, 1, h-12, 0xB07E3BAC);
        int ax = px+(int)(10*sc), ay=py+(int)(small?9*sc:19*sc);
        int art = (int)((small?36:49)*sc);
        Ui.rrect(g, ax, ay, art, art, Math.max(2,(int)(3*sc)), 0xFF24162F);
        Ui.rect(g, ax+3, ay+3, art-6, 1, 0xFF874BB2);
        Ui.text(g, "\u2726", ax+Math.max(2,art/2-4), ay+Math.max(3,art/2-5), Ui.accent1);
        int tx = ax+art+(int)(10*sc);
        Ui.text(g, clipped(m.hasTrack?m.title:m.status, small?19:24), tx, py+(int)(small?11*sc:23*sc), 0xFFF0E6F6);
        Ui.text(g, clipped(m.hasTrack?m.artist:"Windows media bridge", small?21:29), tx, py+(int)(small?24*sc:37*sc), 0xFFAD91BC);
        int barX=tx, barY=py+h-(int)(small?11*sc:20*sc);
        int barW=Math.max(20,w-(tx-px)-(int)(11*sc));
        Ui.rect(g, barX,barY,barW,Math.max(2,(int)(3*sc)),0xFF38253F);
        if(m.durationMs>0) Ui.rect(g,barX,barY,(int)(barW*Math.min(1.0,(double)m.progress()/m.durationMs)),Math.max(2,(int)(3*sc)),Ui.accent1);
        if(!small && showTime.get()) {
            Ui.text(g,clock(m.progress()),barX,barY+(int)(5*sc),0xFF9E89AB);
            String duration=clock(m.durationMs);
            Ui.text(g,duration,barX+barW-Ui.textW(duration),barY+(int)(5*sc),0xFF9E89AB);
        }
        if(!small && showControls.get()) Ui.text(g,m.playing?"II":"\u25b6",px+w-(int)(21*sc),py+(int)(8*sc),Ui.accent1);
        // Album art is supplied by the bridge as base64; GPU texture upload is handled in a later stage.
    }
}

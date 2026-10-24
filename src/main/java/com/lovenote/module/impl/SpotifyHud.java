package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.spotify.SpotifyManager;
import com.lovenote.util.Gfx;
import com.lovenote.util.HudGlass;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Drainer's static, fully scaled Now Playing HUD. */
public final class SpotifyHud extends Module {
    private final NumberSetting x = add(new NumberSetting("X", 0.02, 0, .98, .005));
    private final NumberSetting y = add(new NumberSetting("Y", .72, 0, .98, .005));
    private final NumberSetting size = add(new NumberSetting("Scale", 1.0, .25, 1.6, .05));
    private final ModeSetting layout = add(new ModeSetting("Layout", "Player + Lyrics",
            "Mini Player", "Full Player", "Player + Lyrics"));
    private final BoolSetting hideIdle = add(new BoolSetting("Hide when idle", true));
    private final NumberSetting idleDelay = add(new NumberSetting("Hide after pause (s)", 30, 0, 120, 5));
    private final BoolSetting showTime = add(new BoolSetting("Timestamps", true));
    private final BoolSetting showControls = add(new BoolSetting("Control hints", true));
    private final BoolSetting artwork = add(new BoolSetting("Album artwork", true));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 92, 30, 100, 5));

    private int px, py, w, h, barX, barY, barW, controlX, controlY;
    private double scaleNow = 1;
    private static SpotifyHud instance;
    private boolean dragging;
    private double dragX, dragY;

    public SpotifyHud() {
        super("SpotifyHud", Category.SPOTIFY);
        instance = this;
        describe("One music player with Mini, Full, and Player + Lyrics layouts. Choose Layout in module settings.");
    }
    @Override public void onEnable() { SpotifyManager.I.start(); }
    /** True only while the integrated player is active; standalone Lyrics remains available in other layouts. */
    public static boolean isIntegratedLyricsActive() {
        return instance != null && instance.enabled && instance.layout.is("Player + Lyrics");
    }
    @Override public void onTick() {
        // The lyrics fetch/cache belongs to Lyrics, even when its standalone HUD is disabled.
        // Refreshing only while that module is disabled avoids duplicate work on a tick.
        if (isIntegratedLyricsActive()) {
            Lyrics lyrics = Lyrics.instance();
            if (lyrics != null && !lyrics.enabled) lyrics.refreshForEmbedded();
        }
    }

    private static String fit(String s, int max) {
        if (s == null || s.isBlank()) return "Unknown";
        if (Ui.textW(s) <= max) return s;
        while (!s.isEmpty() && Ui.textW(s + "…") > max) s = s.substring(0, s.length() - 1);
        return s + "…";
    }
    private static String clock(long ms) {
        long seconds = Math.max(0, ms / 1000);
        return seconds / 60 + ":" + String.format(java.util.Locale.ROOT, "%02d", seconds % 60);
    }
    public boolean contains(double mx, double my) { return enabled && mx >= px && mx < px+w && my >= py && my < py+h; }
    public int editorX() { return px; }
    public int editorY() { return py; }
    public int editorWidth() { return w; }
    public int editorHeight() { return h; }
    public void editorBeginDrag(double mx, double my) { dragging = true; dragX = mx - px; dragY = my - py; }
    public void editorStopDrag() { dragging = false; }
    public void editorMove(double mx, double my) {
        if (!dragging) return;
        x.set(Math.max(0, Math.min(Gfx.guiW() - w, mx - dragX)) / Math.max(1, Gfx.guiW()));
        y.set(Math.max(0, Math.min(Gfx.guiH() - h, my - dragY)) / Math.max(1, Gfx.guiH()));
    }
    // Legacy Meteor editor compatibility (new Drainer editor uses dedicated drag APIs).
    public boolean click(double mx, double my, boolean pressed) {
        if (!pressed) { editorStopDrag(); return false; }
        if (!contains(mx, my)) return false;
        SpotifyManager m = SpotifyManager.I;
        if (m.durationMs > 0 && my >= barY - 4 && my <= barY + 8 && mx >= barX && mx <= barX + barW) {
            m.seek((long) (m.durationMs * (mx-barX) / Math.max(1, barW)));
            return true;
        }
        if (!layout.is("Mini Player") && showControls.get() && my >= controlY - 5 * scaleNow && my <= controlY + 14 * scaleNow) {
            if (mx >= controlX && mx < controlX + 20 * scaleNow) { m.previous(); return true; }
            if (mx >= controlX + 26 * scaleNow && mx < controlX + 46 * scaleNow) { m.togglePlay(); return true; }
            if (mx >= controlX + 52 * scaleNow && mx < controlX + 74 * scaleNow) { m.next(); return true; }
        }
        editorBeginDrag(mx, my);
        return true;
    }
    public void move(double mx, double my, boolean down) { if (down) editorMove(mx, my); else editorStopDrag(); }

    @Override public void onHud(GuiGraphicsExtractor g, float tickDelta) {
        SpotifyManager m = SpotifyManager.I;
        boolean editing = net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof
            meteordevelopment.meteorclient.gui.screens.LovenoteModulesScreen screen && screen.isDrainerHudEditor();
        if (hideIdle.get() && (!m.hasTrack || m.idleFor(idleDelay.getI() * 1000L)) && !editing) return;

        double sc = Math.max(.25, Math.min(1.6, size.get()));
        scaleNow = sc;
        boolean mini = layout.is("Mini Player");
        boolean combined = layout.is("Player + Lyrics");
        int bw = mini ? 236 : combined ? 330 : 292;
        int bh = mini ? 72 : combined ? 227 : 124;
        w = (int) Math.round(bw * sc);
        h = (int) Math.round(bh * sc);
        px = Math.max(0, Math.min((int) (x.get() * Gfx.guiW()), Math.max(0, Gfx.guiW()-w)));
        py = Math.max(0, Math.min((int) (y.get() * Gfx.guiH()), Math.max(0, Gfx.guiH()-h)));
        int alpha = Math.max(0, Math.min(255, (int) (opacity.get() * 2.55)));
        int art = mini ? 42 : 58;
        int artX = 13, artY = mini ? 14 : 15;
        int titleX = mini ? 67 : 84;
        int progressY = mini ? 57 : 90;
        int playY = 107;
        int progressWidth = bw-26;
        barX = px + (int) Math.round(13 * sc);
        barY = py + (int) Math.round(progressY * sc);
        barW = (int) Math.round(progressWidth * sc);
        controlX = px + (int) Math.round((bw/2-40) * sc);
        controlY = py + (int) Math.round(playY * sc);

        long played = m.progress();
        double ratio = m.durationMs > 0 ? Math.max(0, Math.min(1.0,
            (double) played/m.durationMs)) : 0.0;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(px,py);
        pose.scale((float) sc,(float) sc);
        HudGlass.card(g,0,0,bw,bh,alpha);
        // Premium glass music control: album artwork, two text levels, minimal accent.
        HudGlass.tile(g,artX-1,artY-1,art+2,art+2,false);
        if (artwork.get() && m.artTexture != null) {
            Gfx.blit(g,m.artTexture,artX,artY,0,0,art,art,art,art,art,art);
        } else {
            Ui.text(g,"♫",artX+art/2-5,artY+art/2-5,HudGlass.accent());
        }
        String name = m.hasTrack ? m.title : "Nothing playing";
        String singer = m.hasTrack ? m.artist : "Connect Spotify to Drainer";
        Ui.text(g,"NOW PLAYING",titleX,mini ? 12 : 18,HudGlass.accent());
        Ui.text(g,HudGlass.fit(name,bw-titleX-14),titleX,mini ? 27 : 35,HudGlass.text());
        Ui.text(g,HudGlass.fit(singer,bw-titleX-14),titleX,mini ? 43 : 52,HudGlass.muted());

        if (showTime.get() && !mini) {
            Ui.text(g,clock(played),13,76,HudGlass.muted());
            String total=clock(m.durationMs);
            Ui.text(g,total,bw-13-Ui.textW(total),76,HudGlass.muted());
        }
        HudGlass.progress(g,13,progressY,progressWidth,3,ratio,HudGlass.accent());
        if (!mini && showControls.get()) {
            int cx = bw/2-40;
            Ui.text(g,"|<",cx,playY,HudGlass.text());
            Ui.text(g,m.playing ? "||" : ">",cx+28,playY,HudGlass.accent());
            Ui.text(g,">|",cx+55,playY,HudGlass.text());
        }
        // A single unified HUD surface: the Lyrics module supplies real synced text
        // and provider state, not a second independently rendered music panel.
        if (combined) {
            g.fill(13,133,bw-13,134,0x327A89C7);
            Lyrics lyrics = Lyrics.instance();
            if (lyrics != null) lyrics.renderEmbedded(g,13,139,bw-26,83);
            else Ui.text(g,"Lyrics are loading...",15,173,HudGlass.muted());
        }
        pose.popMatrix();
    }
}

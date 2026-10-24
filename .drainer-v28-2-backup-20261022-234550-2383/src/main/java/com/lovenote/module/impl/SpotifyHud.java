package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.spotify.SpotifyManager;
import com.lovenote.util.Gfx;
import com.lovenote.util.HudGlass;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Drainer's static, fully scaled Now Playing HUD. */
public final class SpotifyHud extends Module {
    private final NumberSetting x = add(new NumberSetting("X", 0.02, 0, .98, .005));
    private final NumberSetting y = add(new NumberSetting("Y", .72, 0, .98, .005));
    private final NumberSetting size = add(new NumberSetting("Scale", .65, .25, 1.6, .05));
    private final BoolSetting compact = add(new BoolSetting("Compact", true));
    private final BoolSetting hideIdle = add(new BoolSetting("Hide when idle", true));
    private final NumberSetting idleDelay = add(new NumberSetting("Hide after pause (s)", 30, 0, 120, 5));
    private final BoolSetting showTime = add(new BoolSetting("Timestamps", true));
    private final BoolSetting showControls = add(new BoolSetting("Control hints", true));
    private final BoolSetting artwork = add(new BoolSetting("Album artwork", true));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 92, 30, 100, 5));

    private int px, py, w, h, barX, barY, barW, controlX, controlY;
    private double scaleNow = 1;
    private boolean dragging;
    private double dragX, dragY;

    public SpotifyHud() {
        super("SpotifyHud", Category.SPOTIFY);
        describe("Clean fixed-size media layout with uniform font scaling, draggable in Drainer HUD Editor.");
    }
    @Override public void onEnable() { SpotifyManager.I.start(); }

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
        if (showControls.get() && my >= controlY - 5 * scaleNow && my <= controlY + 14 * scaleNow) {
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
        // Keep Spotify visible as a preview in the HUD editor when nothing is playing.
        boolean editing = net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof
            meteordevelopment.meteorclient.gui.screens.LovenoteModulesScreen screen && screen.isDrainerHudEditor();
        if (hideIdle.get() && (!m.hasTrack || m.idleFor(idleDelay.getI() * 1000L)) && !editing) return;
        double sc = Math.max(.25, Math.min(1.6, size.get()));
        scaleNow = sc;
        boolean small = compact.get();
        int bw = small ? 228 : 278, bh = small ? 60 : 93;
        w = (int) Math.round(bw * sc); h = (int) Math.round(bh * sc);
        px = Math.max(0, Math.min((int) (x.get() * Gfx.guiW()), Math.max(0, Gfx.guiW() - w)));
        py = Math.max(0, Math.min((int) (y.get() * Gfx.guiH()), Math.max(0, Gfx.guiH() - h)));
        int alpha = Math.max(0, Math.min(255, (int) (opacity.get() * 2.55)));
        int art = small ? 36 : 50;
        int ax = 8, ay = small ? 11 : 16;
        int tx = small ? 54 : 70;
        barX = px + (int) Math.round(tx * sc);
        barY = py + (int) Math.round((small ? 53 : 72) * sc);
        barW = (int) Math.round((bw - tx - 11) * sc);
        controlX = px + (int) Math.round((small ? 54 : 70) * sc);
        controlY = py + (int) Math.round((small ? 40 : 52) * sc);

        // Matrix transform scales artwork, text, spacing and bars TOGETHER. Geometry never pulses.
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(px, py);
        pose.scale((float) sc, (float) sc);
        // Reuse the working media bridge, progress clock and click controls.
        // Only the native visual chrome changes to match the Claude glass card.
        HudGlass.card(g, 0, 0, bw, bh, alpha);
        HudGlass.pill(g, ax - 1, ay - 1, art + 2, art + 2, 0x87546B96);
        HudGlass.pill(g, ax, ay, art, art, 0xE724263D);
        if (artwork.get() && m.artTexture != null)
            Gfx.blit(g, m.artTexture, ax, ay, 0, 0, art, art, art, art, art, art);
        else Ui.text(g, "♫", ax + art / 2 - 5, ay + art / 2 - 5, HudGlass.accent());
        Ui.text(g, "NOW PLAYING", tx, 6, HudGlass.accent());
        Ui.text(g, HudGlass.fit(m.hasTrack ? m.title : m.status, bw-tx-12),
                tx, small ? 19 : 23, 0xFFF4F1FF);
        Ui.text(g, HudGlass.fit(m.hasTrack ? m.artist : "Windows media", bw-tx-12),
                tx, small ? 30 : 36, HudGlass.muted());
        if (showControls.get()) {
            int cx = small ? 54 : 70, cy = small ? 40 : 52;
            Ui.text(g, "|<", cx, cy, 0xFFC9CDE7);
            Ui.text(g, m.playing ? "||" : ">", cx+26, cy, HudGlass.accent());
            Ui.text(g, ">|", cx+52, cy, 0xFFC9CDE7);
        }
        int localBarY = small ? 53 : 72;
        int localBarW = bw - tx - 11;
        long playbackTime = m.progress();
        double ratio = m.durationMs > 0 ? Math.max(0, Math.min(1,
                (double)playbackTime / m.durationMs)) : 0;
        HudGlass.progress(g, tx, localBarY, localBarW, 3, ratio, HudGlass.accent());
        if (!small && showTime.get()) {
            Ui.text(g, clock(playbackTime), tx, 81, HudGlass.muted());
            String duration = clock(m.durationMs);
            Ui.text(g, duration, bw - 11 - Ui.textW(duration), 81, HudGlass.muted());
        }
        pose.popMatrix();
    }
}

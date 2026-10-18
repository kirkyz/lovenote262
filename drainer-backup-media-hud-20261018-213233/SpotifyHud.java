package com.lovenote.module.impl;

import com.lovenote.spotify.SpotifyManager;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** First-stage Windows media bridge HUD for Minecraft 26.2. */
public final class SpotifyHud extends LegacyTextWidget {
    public SpotifyHud() {
        super("SpotifyHud", 0.012, 0.72);
        describe("Windows Spotify now playing: song, artist, playback and progress.");
    }
    @Override public void onEnable() { SpotifyManager.I.start(); }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        SpotifyManager m = SpotifyManager.I;
        if (!m.hasTrack) {
            drawText(g, "Spotify: " + m.status);
            return;
        }
        String song = m.title + (m.artist.isBlank() ? "" : "  -  " + m.artist);
        drawText(g, (m.playing ? "\u25b6 " : "\u275a\u275a ") + song);
        if (m.durationMs <= 0) return;
        int width = Math.max(80, Math.min(260, Ui.textW(song) + 12));
        int px = Math.max(0, Math.min((int)(x.get() * Gfx.guiW()), Math.max(0, Gfx.guiW() - width)));
        int py = Math.max(0, Math.min((int)(y.get() * Gfx.guiH()) + 21, Gfx.guiH() - 4));
        Ui.rect(g, px, py, width, 3, 0x77444455);
        Ui.rect(g, px, py, (int)(width * Math.min(1.0, (double)m.progress() / m.durationMs)), 3, Ui.accent1);
    }
}

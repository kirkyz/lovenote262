package com.lovenote.util;

import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteDraw;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared native HUD primitives inspired by the user's Drainer widgets HTML.
 * Uses the same palette and opacity settings as the existing HUD; no texture
 * uploads, shaders, blur passes or per-widget offscreen framebuffers.
 */
public final class HudGlass {
    private HudGlass() {}

    private static int rgba(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    public static int accent() { return Ui.accent1; }
    public static int muted() { return 0xFFAAAAC4; }

    /** Slim, translucent HUD surface shared by every supported native widget. */
    public static void card(GuiGraphicsExtractor g, int x, int y, int w, int h, int opacity) {
        if (w < 5 || h < 5) return;
        int a = (int) (Math.max(0, Math.min(255, opacity))
                * Math.max(0, Math.min(100, LovenoteMenuConfig.widgetOpacity)) / 100f);
        int border = Math.max(0, Math.min(255, LovenoteMenuConfig.widgetBorder));
        int radius = Math.min(13, Math.min(w, h) / 3);
        if (border > 0) LovenoteDraw.rounded(g, x, y, w, h, radius,
                rgba(Ui.accent2, Math.min(96, border)));
        LovenoteDraw.rounded(g, x + 1, y + 1, w - 2, h - 2,
                Math.max(1, radius - 1), rgba(0x0B1223, a));
        // A narrow top accent and a low-contrast edge, as in the HTML prototype.
        if (LovenoteMenuConfig.widgetGlass && LovenoteMenuConfig.widgetSheen > 0 && w > 24) {
            int shine = Math.min(115, LovenoteMenuConfig.widgetSheen / 3);
            g.fill(x + radius, y + 2, x + w - radius, y + 3,
                    rgba(Ui.accent1, shine));
        }
    }

    public static void pill(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill) {
        if (w <= 0 || h <= 0) return;
        LovenoteDraw.rounded(g, x, y, w, h, Math.min(7, h / 2), fill);
    }

    public static void progress(GuiGraphicsExtractor g, int x, int y, int width,
                                int height, double progress, int highlight) {
        if (width <= 0) return;
        pill(g, x, y, width, height, rgba(0x4E5574, 108));
        int filled = Math.max(0, Math.min(width, (int) Math.round(width * progress)));
        if (filled > 0) pill(g, x, y, filled, height, highlight);
    }

    public static String fit(String text, int maxWidth) {
        if (text == null || text.isBlank()) return "—";
        if (Ui.textW(text) <= maxWidth) return text;
        int end = text.length();
        while (end > 0 && Ui.textW(text.substring(0, end) + "…") > maxWidth) end--;
        return end <= 0 ? "…" : text.substring(0, end) + "…";
    }
}

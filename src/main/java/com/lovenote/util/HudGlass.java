package com.lovenote.util;

import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteDraw;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared, lightweight, user-theme-aware chrome for Drainer's draggable native widgets. */
public final class HudGlass {
    private HudGlass() {}

    private static int rgba(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xFFFFFF);
    }

    public static int accent() { return Ui.accent1; }
    public static int secondary() { return Ui.accent2; }
    public static int muted() { return 0xFFABB2CF; }
    public static int text() { return 0xFFF4F4FF; }

    /** Soft neon glass, not a solid square; honours opacity, glow, edge and glass settings. */
    public static void card(GuiGraphicsExtractor g, int x, int y, int w, int h, int opacity) {
        if (w < 5 || h < 5) return;
        int a = Math.round(Math.max(0, Math.min(255, opacity))
                * Math.max(0, Math.min(100, LovenoteMenuConfig.widgetOpacity)) / 100f);
        int edge = Math.max(0, Math.min(255, LovenoteMenuConfig.widgetBorder));
        int glow = Math.max(0, Math.min(255, LovenoteMenuConfig.widgetGlow));
        int radius = Math.min(16, Math.min(w, h) / 3);
        // At most one optional outer glow pass; no large blurry framebuffer or per-row gradients.
        if (glow > 0 && a > 0) {
            int glowAlpha = Math.min(52, (glow / 5) * a / 255);
            if (glowAlpha > 0) LovenoteDraw.rounded(g, x-2, y-2, w+4, h+4,
                    radius+2, rgba(secondary(), glowAlpha));
        }
        if (edge > 0 && a > 0) {
            LovenoteDraw.rounded(g, x, y, w, h, radius,
                    rgba(secondary(), Math.min(137, edge) * a / 255));
        }
        int inset = edge > 0 ? 1 : 0;
        LovenoteDraw.rounded(g, x+inset, y+inset, w-2*inset, h-2*inset,
                Math.max(1, radius-inset), rgba(0x111934, a));
        if (LovenoteMenuConfig.widgetGlass && LovenoteMenuConfig.widgetSheen > 0 && a > 0) {
            int shine = Math.min(86, LovenoteMenuConfig.widgetSheen / 3) * a / 255;
            if (shine > 0 && w > radius * 2 + 2)
                g.fill(x + radius, y + 2, x + w - radius, y + 3, rgba(0xFFFFFF, shine));
        }
    }

    /** Inset glass tile used for keys, album art and armour slots. */
    public static void tile(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean active) {
        if (w < 4 || h < 4) return;
        int radius = Math.min(11, h / 3);
        LovenoteDraw.rounded(g, x, y, w, h, radius,
                rgba(active ? accent() : secondary(), active ? 85 : 35));
        LovenoteDraw.rounded(g, x+1, y+1, w-2, h-2, Math.max(1,radius-1),
                active ? 0x9E2D295A : 0x79212A49);
    }

    public static void pill(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill) {
        if (w <= 0 || h <= 0) return;
        LovenoteDraw.rounded(g, x, y, w, h, Math.min(11, h / 2), fill);
    }

    public static void progress(GuiGraphicsExtractor g, int x, int y, int width,
                                int height, double progress, int highlight) {
        if (width <= 0 || height <= 0) return;
        pill(g, x, y, width, height, rgba(0x7885B6, 85));
        int filled = Math.max(0, Math.min(width, (int)Math.round(width *
                Math.max(0, Math.min(1, progress)))));
        if (filled > 0) pill(g, x, y, filled, height, highlight);
        // A tiny glass thumb; deliberately static so it doesn't cost per-frame shader work.
        if (filled > 0 && filled < width && width > 32 && height >= 3)
            pill(g, x+Math.max(0,filled-2), y-1, 4, height+2, 0xFFF5F4FF);
    }

    public static String fit(String text, int maxWidth) {
        if (text == null || text.isBlank()) return "—";
        if (Ui.textW(text) <= maxWidth) return text;
        int end = text.length();
        while (end > 0 && Ui.textW(text.substring(0,end) + "…") > maxWidth) end--;
        return end <= 0 ? "…" : text.substring(0,end) + "…";
    }
}

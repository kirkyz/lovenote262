package meteordevelopment.meteorclient.gui.themes.lovenote;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared Drainer 26.2 dashboard styling. No gameplay or module state. */
public final class DrainerSkin {
    private DrainerSkin() {}
    // Low-contrast graphite surfaces, restrained violet accent, consistent typography.
    // These values are intentionally shared by Guides, Schematics, HUD and SkyBlock.
    public static final int WINDOW = 0xB20A1020;
    public static final int PANEL = 0xA8171E30;
    public static final int PANEL_RAISED = 0xC2212A41;
    public static final int FIELD = 0xAE101A2A;
    public static final int TEXT = 0xFFF5F3FD;
    public static final int SUBTEXT = 0xFFB8C2D8;
    public static final int MUTED = 0xFF8E9DB7;
    public static final int DIVIDER = 0x345D6788;

    public static int accent() { return LovenotePalette.C2; }
    public static int accentText() { return LovenotePalette.C3; }
    public static int alpha(int color, int a) { return LovenotePalette.alpha(color, a); }

    /** Frosted 2026-style surface: translucent fill + low-contrast light edge. */
    public static void surface(GuiGraphicsExtractor g, int x, int y, int w, int h,
                               int radius, boolean selected) {
        if (w <= 3 || h <= 3) return;
        int r = Math.min(Math.max(4, radius), Math.min(w, h) / 2);
        LovenoteDraw.rounded(g, x, y, w, h, r,
            selected ? alpha(accent(), 100) : 0x31596587);
        LovenoteDraw.rounded(g, x + 1, y + 1, w - 2, h - 2,
            Math.max(2, r - 1), selected ? PANEL_RAISED : PANEL);
        // Restrained glass reflection, rather than thick borders or solid panels.
        if (w > 38 && h > 12) {
            g.fill(x + r + 7, y + 2, x + w - r - 7, y + 3,
                selected ? alpha(accentText(), 60) : 0x19FFFFFF);
        }
        if (selected && h > 16) {
            g.fill(x + 2, y + r, x + 4, y + h - r,
                alpha(accent(), 135));
        }
    }

    /** Small glowing glass controls; no new renderer or shader dependencies. */
    public static void control(GuiGraphicsExtractor g, int x, int y, int w, int h,
                               boolean active) {
        if (w <= 3 || h <= 3) return;
        int r = Math.max(5, Math.min(10, h / 3));
        LovenoteDraw.rounded(g, x, y, w, h, r,
            active ? alpha(accent(), 100) : 0x345E6684);
        LovenoteDraw.rounded(g, x + 1, y + 1, w - 2, h - 2,
            Math.max(2, r - 1), active ? 0xBD282443 : FIELD);
        if (w > 26) g.fill(x + r + 4, y + 2, x + w - r - 4, y + 3,
            active ? alpha(accentText(), 53) : 0x15FFFFFF);
        if (active && w > 16) g.fill(x + 8, y + h - 2, x + w - 8, y + h - 1,
            alpha(accent(), 135));
    }

    /** Inset text/edit field with a readable subtle stroke. */
    public static void field(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        control(g, x, y, w, h, false);
    }
}

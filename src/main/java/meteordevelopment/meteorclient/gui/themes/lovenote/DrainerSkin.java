package meteordevelopment.meteorclient.gui.themes.lovenote;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared Drainer 26.2 dashboard styling. No gameplay or module state. */
public final class DrainerSkin {
    private DrainerSkin() {}
    // Low-contrast graphite surfaces, restrained violet accent, consistent typography.
    // These values are intentionally shared by Guides, Schematics, HUD and SkyBlock.
    public static final int WINDOW = 0xC10B1020;
    public static final int PANEL = 0xD1161D2D;
    public static final int PANEL_RAISED = 0xDA27243E;
    public static final int FIELD = 0xC8141B2C;
    public static final int TEXT = 0xFFF5F3FD;
    public static final int SUBTEXT = 0xFFB8C2D8;
    public static final int MUTED = 0xFF8E9DB7;
    public static final int DIVIDER = 0x355D6788;

    public static int accent() { return LovenotePalette.C2; }
    public static int accentText() { return LovenotePalette.C3; }
    public static int alpha(int color, int a) { return LovenotePalette.alpha(color, a); }

    /** Frosted 2026-style surface: translucent fill + low-contrast light edge. */
    public static void surface(GuiGraphicsExtractor g, int x, int y, int w, int h,
                               int radius, boolean selected) {
        if (w <= 3 || h <= 3) return;
        int r = Math.min(Math.max(4, radius), Math.min(w, h) / 2);
        // Separate depth from opacity: low-cost drop shadow + bright thin rim.
        if (h > 14 && w > 28) {
            LovenoteDraw.rounded(g, x - 1, y + 2, w + 2, h, r + 1, 0x48040813);
        }
        LovenoteDraw.rounded(g, x, y, w, h, r,
            selected ? alpha(accent(), 116) : 0x417584A4);
        LovenoteDraw.rounded(g, x + 1, y + 1, w - 2, h - 2,
            Math.max(2, r - 1), selected ? PANEL_RAISED : PANEL);
        if (w > 38 && h > 12) {
            g.fill(x + r + 7, y + 2, x + w - r - 7, y + 3,
                selected ? alpha(accentText(), 106) : 0x31FFFFFF);
            g.fill(x + r + 9, y + h - 3, x + w - r - 9, y + h - 2, 0x2E050812);
        }
        if (selected && h > 16) {
            g.fill(x + 2, y + r, x + 4, y + h - r,
                alpha(accent(), 170));
        }
    }

    /** Small glowing glass controls; no new renderer or shader dependencies. */
    public static void control(GuiGraphicsExtractor g, int x, int y, int w, int h,
                               boolean active) {
        if (w <= 3 || h <= 3) return;
        int r = Math.max(5, Math.min(10, h / 3));
        LovenoteDraw.rounded(g, x, y, w, h, r,
            active ? alpha(accent(), 132) : 0x44727D99);
        LovenoteDraw.rounded(g, x + 1, y + 1, w - 2, h - 2,
            Math.max(2, r - 1), active ? 0xDA302443 : FIELD);
        if (w > 26) g.fill(x + r + 4, y + 2, x + w - r - 4, y + 3,
            active ? alpha(accentText(), 81) : 0x33FFFFFF);
        if (active && w > 16) g.fill(x + 8, y + h - 2, x + w - 8, y + h - 1,
            alpha(accent(), 135));
    }


/** One-pixel shared section separator; zero shader passes and no allocations. */
public static void section(GuiGraphicsExtractor g, int x, int y, int w) {
    if (w <= 16) return;
    g.fill(x, y, x + w, y + 1, DIVIDER);
    g.fill(x + 1, y + 1, x + Math.min(28, w - 1), y + 2,
        alpha(accent(), 110));
}

/** Selected list-row indicator shared by macro, schematic and SkyBlock menus. */
public static void selectionMark(GuiGraphicsExtractor g, int x, int y, int h) {
    if (h <= 8) return;
    g.fill(x, y + 4, x + 2, y + h - 4, alpha(accent(), 200));
}

    /** Inset text/edit field with a readable subtle stroke. */
    public static void field(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        control(g, x, y, w, h, false);
    }
}

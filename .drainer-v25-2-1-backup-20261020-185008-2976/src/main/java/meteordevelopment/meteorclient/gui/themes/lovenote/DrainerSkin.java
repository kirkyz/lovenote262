package meteordevelopment.meteorclient.gui.themes.lovenote;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared Drainer 26.2 dashboard styling. No gameplay or module state. */
public final class DrainerSkin {
    private DrainerSkin() {}
    // Low-contrast graphite surfaces, restrained violet accent, consistent typography.
    // These values are intentionally shared by Guides, Schematics, HUD and SkyBlock.
    public static final int WINDOW = 0xFC0B101D;
    public static final int PANEL = 0xF3131B2B;
    public static final int PANEL_RAISED = 0xF51C2639;
    public static final int FIELD = 0xF00E1625;
    public static final int TEXT = 0xFFF2F4FC;
    public static final int SUBTEXT = 0xFFAFB9CD;
    public static final int MUTED = 0xFF818DA8;
    public static final int DIVIDER = 0x554A5776;

    public static int accent() { return LovenotePalette.C2; }
    public static int accentText() { return LovenotePalette.C3; }
    public static int alpha(int color, int a) { return LovenotePalette.alpha(color, a); }

    /** Panel/chrome renderer. No shaders, texture uploads or allocations per frame. */
    public static void surface(GuiGraphicsExtractor g, int x, int y, int w, int h,
                               int radius, boolean selected) {
        if (w <= 3 || h <= 3) return;
        int r = Math.min(Math.max(3, radius), Math.min(w, h) / 2);
        // One-pixel edge, graphite body, restrained bevel and contextual accent.
        LovenoteDraw.rounded(g, x, y, w, h, r,
            selected ? alpha(accent(), 168) : DIVIDER);
        LovenoteDraw.rounded(g, x + 1, y + 1, w - 2, h - 2,
            Math.max(1, r - 1), selected ? PANEL_RAISED : PANEL);
        if (w > 38) g.fill(x + r + 8, y + 1, x + w - r - 8, y + 2,
            selected ? alpha(accentText(), 105) : 0x1AFFFFFF);
        if (selected) g.fill(x + 2, y + r, x + 4, y + h - r,
            alpha(accent(), 205));
    }

    /** Compact interactive chips. Uses the same palette as every workspace. */
    public static void control(GuiGraphicsExtractor g, int x, int y, int w, int h,
                               boolean active) {
        if (w <= 3 || h <= 3) return;
        int r = Math.min(7, h / 3);
        LovenoteDraw.rounded(g, x, y, w, h, r,
            active ? alpha(accent(), 152) : 0x57445A78);
        LovenoteDraw.rounded(g, x + 1, y + 1, w - 2, h - 2,
            Math.max(1, r - 1), active ? 0xF02A2948 : FIELD);
        if (active) g.fill(x + 8, y + h - 2, x + w - 8, y + h - 1,
            alpha(accentText(), 188));
    }

    /** Inset text/edit field with a readable subtle stroke. */
    public static void field(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        control(g, x, y, w, h, false);
    }
}

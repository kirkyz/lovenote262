package meteordevelopment.meteorclient.gui.themes.lovenote;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared Drainer 26.2 dashboard styling. No gameplay or module state. */
public final class DrainerSkin {
    private DrainerSkin() {}
    public static final int WINDOW = 0xFA0C101B;
    public static final int PANEL = 0xF0151A2A;
    public static final int PANEL_RAISED = 0xF01B2133;
    public static final int FIELD = 0xEC171D2C;
    public static final int TEXT = 0xFFF3F1FB;
    public static final int SUBTEXT = 0xFFB6BAD0;
    public static final int MUTED = 0xFF929AB4;
    public static final int DIVIDER = 0x66454A65;

    public static int accent() { return LovenotePalette.C2; }
    public static int accentText() { return LovenotePalette.C3; }
    public static int alpha(int color, int a) { return LovenotePalette.alpha(color, a); }

    public static void surface(GuiGraphicsExtractor g, int x, int y, int w, int h,
                               int radius, boolean selected) {
        if (w <= 3 || h <= 3) return;
        int r = Math.min(Math.max(3, radius), Math.min(w, h) / 2);
        LovenoteDraw.rounded(g, x, y, w, h, r, selected ? alpha(accent(), 125) : DIVIDER);
        LovenoteDraw.rounded(g, x + 1, y + 1, w - 2, h - 2,
            Math.max(1, r - 1), selected ? PANEL_RAISED : PANEL);
        if (selected) g.fill(x + Math.max(6, r), y + h - 2,
            x + w - Math.max(6, r), y + h - 1, alpha(accent(), 145));
    }

    public static void field(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        if (w <= 3 || h <= 3) return;
        LovenoteDraw.rounded(g, x, y, w, h, 7, DIVIDER);
        LovenoteDraw.rounded(g, x + 1, y + 1, w - 2, h - 2, 6, FIELD);
    }
}

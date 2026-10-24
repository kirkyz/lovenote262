package meteordevelopment.meteorclient.gui.themes.lovenote;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Port of the old client's draw helpers, on vanilla GuiGraphicsExtractor. */
public final class LovenoteDraw {
    private LovenoteDraw() {}

    public static void rounded(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int r, int c) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) { ctx.fill(x, y, x + w, y + h, c); return; }
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0.0, (double) r * r - dy * dy)));
            int x1 = Math.min(x + inset, x + w);
            int x2 = Math.max(x + w - inset, x1);
            ctx.fill(x1, y + i, x2, y + i + 1, c);
            ctx.fill(x1, y + h - 1 - i, x2, y + h - i, c);
        }
        ctx.fill(x, y + r, x + w, y + h - r, c);
    }

    public static void gradientH(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int c1, int c2) {
        if (x2 <= x1 || y2 <= y1) return;
        int w = x2 - x1;
        for (int i = 0; i < w; i++) {
            float t = w == 1 ? 0f : i / (float) (w - 1);
            ctx.fill(x1 + i, y1, x1 + i + 1, y2, LovenotePalette.lerp(c1, c2, t));
        }
    }

    public static void circle(GuiGraphicsExtractor ctx, int cx, int cy, int r, int c) {
        for (int dy = -r; dy <= r; dy++) {
            int hw = (int) Math.round(Math.sqrt(Math.max(0.0, (double) r * r - (double) dy * dy)));
            ctx.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, c);
        }
    }

    public static void skeetBar(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        int mid = x + w / 2;
        gradientH(ctx, x, y, mid, y + h, LovenotePalette.C1, LovenotePalette.C2);
        gradientH(ctx, mid, y, x + w, y + h, LovenotePalette.C2, LovenotePalette.C3);
    }
}

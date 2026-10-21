package meteordevelopment.meteorclient.gui.themes.lovenote;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Port of the old client's draw helpers, on vanilla GuiGraphicsExtractor. */
public final class LovenoteDraw {
    private LovenoteDraw() {}

    // Radius bands are built once and reused by every UI panel. The original
    // renderer submitted two rectangles for every corner pixel row, plus one
    // centre rectangle. Repeated in the card grid that is a lot of geometry.
    // Grouping equal-width rows preserves the exact existing silhouette while
    // avoiding repeated sqrt calculations and many fill submissions per frame.
    private static final int MAX_CACHED_RADIUS = 64;
    private static final int[][][] CORNER_BANDS = new int[MAX_CACHED_RADIUS + 1][][];
    private static final int[] FULL_WIDTH_START = new int[MAX_CACHED_RADIUS + 1];

    static {
        for (int radius = 1; radius <= MAX_CACHED_RADIUS; radius++) {
            int[] offsets = new int[radius];
            int full = radius;
            for (int row = 0; row < radius; row++) {
                double dy = radius - row - 0.5;
                int inset = (int) Math.round(radius - Math.sqrt(
                        Math.max(0.0, (double) radius * radius - dy * dy)));
                offsets[row] = inset;
                if (inset == 0 && full == radius) full = row;
            }
            FULL_WIDTH_START[radius] = full;
            java.util.ArrayList<int[]> bands = new java.util.ArrayList<>();
            for (int row = 0; row < full;) {
                int next = row + 1;
                while (next < full && offsets[next] == offsets[row]) next++;
                bands.add(new int[] { row, next, offsets[row] });
                row = next;
            }
            CORNER_BANDS[radius] = bands.toArray(new int[0][]);
        }
    }

    public static void rounded(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int r, int c) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) { ctx.fill(x, y, x + w, y + h, c); return; }

        if (r > MAX_CACHED_RADIUS) {
            // Rare oversized corners retain the exact original implementation.
            for (int i = 0; i < r; i++) {
                double dy = r - i - 0.5;
                int inset = (int) Math.round(r - Math.sqrt(
                        Math.max(0.0, (double) r * r - dy * dy)));
                ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, c);
                ctx.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, c);
            }
            ctx.fill(x, y + r, x + w, y + h - r, c);
            return;
        }

        int fullWidthStart = FULL_WIDTH_START[r];
        ctx.fill(x, y + fullWidthStart, x + w, y + h - fullWidthStart, c);
        for (int[] band : CORNER_BANDS[r]) {
            int inset = band[2];
            ctx.fill(x + inset, y + band[0], x + w - inset, y + band[1], c);
            ctx.fill(x + inset, y + h - band[1], x + w - inset, y + h - band[0], c);
        }
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

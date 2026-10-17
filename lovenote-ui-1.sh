#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

T=src/main/java/meteordevelopment/meteorclient/gui/themes/lovenote
mkdir -p "$T"

cat > "$T/LovenotePalette.java" << 'EOF'
package meteordevelopment.meteorclient.gui.themes.lovenote;

import net.minecraft.util.Mth;

/** Port of the old client's Colors. Accent gradient C1 -> C2 -> C3. */
public final class LovenotePalette {
    private LovenotePalette() {}

    public static int C1 = 0xFFFF9ACB, C2 = 0xFFD97BC0, C3 = 0xFFB28DFF;
    public static boolean animated = true;
    public static boolean dark = true;

    public static final String[] PRESET_NAMES = {
        "Lovenote", "Purple", "Red", "Orange", "Yellow", "Green", "Cyan", "Blue", "Pink",
        "Sunset", "Dawn", "Mint", "Forest", "Ocean", "Ice", "Sky", "Lavender", "Magenta",
        "Rose", "Coral", "Amber", "Gold", "Lime", "Emerald", "Teal", "Aqua", "Azure",
        "Indigo", "Violet", "Plum", "Crimson", "Ember", "Neon", "Vapor", "Matrix", "Candy",
        "Midnight", "Blood", "Frost", "Haze", "Sakura", "Toxic"
    };

    public static final int[][] PRESETS = {
        {0xFF9ACB, 0xB28DFF}, {0xA855F7, 0x9647FF}, {0xEF4444, 0xF97316}, {0xF97316, 0xFACC15},
        {0xFACC15, 0xA3E635}, {0x22C55E, 0x10B981}, {0x06B6D4, 0x3B82F6}, {0x3B82F6, 0x6366F1},
        {0xEC4899, 0xF472B6},
        {0xFF7A45, 0xFFB84D}, {0xFFC48A, 0xFF7AB0}, {0x3BE8B0, 0x2BD9C4}, {0x2E7D32, 0x66BB6A},
        {0x0E7490, 0x22D3EE}, {0xA5F3FC, 0x67E8F9}, {0x38BDF8, 0x818CF8}, {0xC4B5FD, 0xE9D5FF},
        {0xE935C1, 0xFF5FD2},
        {0xFB7185, 0xF472B6}, {0xFF7F50, 0xFFA36B}, {0xFFB020, 0xFFD166}, {0xFFD700, 0xFFB800},
        {0xA3E635, 0x84CC16}, {0x10B981, 0x34D399}, {0x14B8A6, 0x0D9488}, {0x22D3EE, 0x38BDF8},
        {0x2563EB, 0x60A5FA},
        {0x4F46E5, 0x7C3AED}, {0x8B5CF6, 0xA78BFA}, {0xA21CAF, 0xD946EF}, {0xDC143C, 0xFF4D6D},
        {0xFF4500, 0xFF8C00}, {0x39FF14, 0x00E5FF}, {0xFF71CE, 0x01CDFE}, {0x00FF41, 0x008F11},
        {0xFF9ACB, 0x9BF6FF},
        {0x191970, 0x4B0082}, {0x8A0303, 0xDC143C}, {0xB3E5FC, 0x4FC3F7}, {0x9D8DF1, 0x6FD1E8},
        {0xFFB7C5, 0xFF8FAB}, {0xB6FF00, 0x00FF9C}
    };

    public static void setAccent(int primary, int secondary) {
        C1 = 0xFF000000 | primary;
        C3 = 0xFF000000 | secondary;
        C2 = lerp(C1, C3, 0.5f);
    }

    public static int bg()        { return dark ? 0x101015 : 0xF2F2F6; }
    public static int card()      { return dark ? 0xFF17171E : 0xFFFFFFFF; }
    public static int cardHover() { return dark ? 0xFF1D1D26 : 0xFFF4F4F9; }
    public static int text()      { return dark ? 0xFFE8E8F0 : 0xFF1A1A22; }
    public static int dim()       { return dark ? 0xFF8A8A9A : 0xFF6A6A78; }
    public static int faint()     { return dark ? 0xFF5E5E70 : 0xFF9A9AA8; }
    public static int track()     { return dark ? 0xFF2A2A36 : 0xFFDADAE4; }
    public static int chip()      { return dark ? 0xFF24242E : 0xFFE6E6EE; }

    public static int alpha(int c, int a) {
        return (Mth.clamp(a, 0, 255) << 24) | (c & 0xFFFFFF);
    }

    public static int lerp(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int aa = (int) (((a >>> 24) & 255) + ((((b >>> 24) & 255) - ((a >>> 24) & 255)) * t));
        int r  = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
        int g  = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
        int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * t));
        return (aa << 24) | (r << 16) | (g << 8) | bl;
    }

    public static int darken(int c) { return lerp(c, 0xFF000000, 0.45f); }

    public static int skeet(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return t < 0.5f ? lerp(C1, C2, t * 2f) : lerp(C2, C3, (t - 0.5f) * 2f);
    }

    public static int wave(float offset) {
        if (!animated) return skeet(Mth.clamp(0.25f + offset * 0.5f, 0f, 1f));
        float t = (System.currentTimeMillis() % 3000L) / 3000f + offset;
        t = Math.abs((t % 1f) * 2f - 1f);
        return skeet(t);
    }
}
EOF

cat > "$T/LovenoteDraw.java" << 'EOF'
package meteordevelopment.meteorclient.gui.themes.lovenote;

import net.minecraft.client.gui.GuiGraphics;

/** Port of the old client's draw helpers, on vanilla GuiGraphics. */
public final class LovenoteDraw {
    private LovenoteDraw() {}

    public static void rounded(GuiGraphics ctx, int x, int y, int w, int h, int r, int c) {
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

    public static void gradientH(GuiGraphics ctx, int x1, int y1, int x2, int y2, int c1, int c2) {
        if (x2 <= x1 || y2 <= y1) return;
        int w = x2 - x1;
        for (int i = 0; i < w; i++) {
            float t = w == 1 ? 0f : i / (float) (w - 1);
            ctx.fill(x1 + i, y1, x1 + i + 1, y2, LovenotePalette.lerp(c1, c2, t));
        }
    }

    public static void circle(GuiGraphics ctx, int cx, int cy, int r, int c) {
        for (int dy = -r; dy <= r; dy++) {
            int hw = (int) Math.round(Math.sqrt(Math.max(0.0, (double) r * r - (double) dy * dy)));
            ctx.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, c);
        }
    }

    public static void skeetBar(GuiGraphics ctx, int x, int y, int w, int h) {
        int mid = x + w / 2;
        gradientH(ctx, x, y, mid, y + h, LovenotePalette.C1, LovenotePalette.C2);
        gradientH(ctx, mid, y, x + w, y + h, LovenotePalette.C2, LovenotePalette.C3);
    }
}
EOF

cat > "$T/LovenoteMenuConfig.java" << 'EOF'
package meteordevelopment.meteorclient.gui.themes.lovenote;

/** Look/layout knobs. Stage 2 wires the Themes page to these. */
public final class LovenoteMenuConfig {
    private LovenoteMenuConfig() {}

    public static int menuScale = 65;
    public static int windowWidth = 600;
    public static int windowHeight = 372;
    public static int corner = 10;
    public static int railWidth = 118;
    public static boolean railRight = false;
    public static int columns = 2;
    public static boolean compact = false;
    public static boolean descriptions = true;
    public static boolean footer = true;
    public static boolean tooltips = true;

    public static boolean logo = true;
    public static boolean logoGlow = true;
    public static int logoSize = 26;
    public static long logoSpin = 2600L;

    public static String style = "Classic";
    public static String cardStyle = "Solid";
    public static int opacity = 100;
    public static int glow = 5;
    public static boolean shadow = true;
    public static boolean headerLine = true;
    public static boolean sheen = true;

    public static String animation = "Fade";
    public static int animSpeed = 300;
    public static int railSpeed = 300;
    public static boolean stagger = true;
    public static int staggerSpeed = 20;
    public static int hoverLift = 2;
    public static boolean ripples = true;

    public static int particles = 45;
    public static int particleSpeed = 100;

    public static int accentIndex = 0;
    public static String customPrimary = "A855F7";
    public static String customSecondary = "9647FF";

    public static void apply() {
        LovenotePalette.animated = true;
        int i = accentIndex;
        if (i >= 0 && i < LovenotePalette.PRESETS.length) {
            LovenotePalette.setAccent(LovenotePalette.PRESETS[i][0], LovenotePalette.PRESETS[i][1]);
        } else {
            LovenotePalette.setAccent(parse(customPrimary, 0xA855F7), parse(customSecondary, 0x9647FF));
        }
    }

    public static int parse(String hex, int fallback) {
        try { return Integer.parseInt(hex.replace("#", "").trim(), 16) & 0xFFFFFF; }
        catch (Exception e) { return fallback; }
    }
}
EOF

./gradlew compileJava --console=plain 2>&1 | tee /tmp/build.log >/dev/null || true
echo "errors: $(grep -c 'error:' /tmp/build.log || true)"
grep -B2 -A3 "error:" /tmp/build.log | head -30

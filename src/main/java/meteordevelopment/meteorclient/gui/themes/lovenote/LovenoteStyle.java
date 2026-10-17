package meteordevelopment.meteorclient.gui.themes.lovenote;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

/** Modern glass draw helpers. Renderer-native: quad / triangle / text only. */
public final class LovenoteStyle {
    private LovenoteStyle() {}

    public static final double HAIRLINE = 1.0;
    public static final double TRACKING = 1.0;

    // ---------------------------------------------------------------- colour

    private static double clamp(double v) { return v < 0 ? 0 : v > 255 ? 255 : v; }

    public static Color mul(Color c, double m) {
        return new Color(c.r, c.g, c.b, (int) clamp(c.a * m));
    }

    public static Color withA(Color c, int a) {
        return new Color(c.r, c.g, c.b, (int) clamp(a));
    }

    public static Color mix(Color a, Color b, double t) {
        t = t < 0 ? 0 : t > 1 ? 1 : t;
        return new Color(
            (int) (a.r + (b.r - a.r) * t),
            (int) (a.g + (b.g - a.g) * t),
            (int) (a.b + (b.b - a.b) * t),
            (int) (a.a + (b.a - a.a) * t));
    }

    // ---------------------------------------------------------------- shapes

    public static void circle(GuiRenderer r, double cx, double cy, double rad, Color c) {
        int seg = 14;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            r.triangle(cx, cy,
                cx + Math.cos(a0) * rad, cy + Math.sin(a0) * rad,
                cx + Math.cos(a1) * rad, cy + Math.sin(a1) * rad, c);
        }
    }

    private static void corner(GuiRenderer r, double cx, double cy, double rad, double a0, double a1, Color c) {
        int seg = 5;
        for (int i = 0; i < seg; i++) {
            double t0 = a0 + (a1 - a0) * i / seg;
            double t1 = a0 + (a1 - a0) * (i + 1) / seg;
            r.triangle(cx, cy,
                cx + Math.cos(t0) * rad, cy + Math.sin(t0) * rad,
                cx + Math.cos(t1) * rad, cy + Math.sin(t1) * rad, c);
        }
    }

    private static void ringCorner(GuiRenderer r, double cx, double cy, double rad, double t, double a0, double a1, Color c) {
        int seg = 6;
        for (int i = 0; i < seg; i++) {
            double t0 = a0 + (a1 - a0) * i / seg;
            double t1 = a0 + (a1 - a0) * (i + 1) / seg;
            double ox0 = cx + Math.cos(t0) * rad, oy0 = cy + Math.sin(t0) * rad;
            double ox1 = cx + Math.cos(t1) * rad, oy1 = cy + Math.sin(t1) * rad;
            double ix0 = cx + Math.cos(t0) * (rad - t), iy0 = cy + Math.sin(t0) * (rad - t);
            double ix1 = cx + Math.cos(t1) * (rad - t), iy1 = cy + Math.sin(t1) * (rad - t);
            r.triangle(ox0, oy0, ox1, oy1, ix1, iy1, c);
            r.triangle(ox0, oy0, ix1, iy1, ix0, iy0, c);
        }
    }

    public static void roundedQuad(GuiRenderer r, double x, double y, double w, double h, double rad, Color c) {
        if (rad <= 0 || w < rad * 2 || h < rad * 2) { r.quad(x, y, w, h, c); return; }
        r.quad(x + rad, y, w - rad * 2, h, c);
        r.quad(x, y + rad, rad, h - rad * 2, c);
        r.quad(x + w - rad, y + rad, rad, h - rad * 2, c);
        corner(r, x + rad, y + rad, rad, Math.PI, Math.PI * 1.5, c);
        corner(r, x + w - rad, y + rad, rad, Math.PI * 1.5, Math.PI * 2, c);
        corner(r, x + w - rad, y + h - rad, rad, 0, Math.PI * 0.5, c);
        corner(r, x + rad, y + h - rad, rad, Math.PI * 0.5, Math.PI, c);
    }

    public static void roundedQuadTop(GuiRenderer r, double x, double y, double w, double h, double rad, Color c) {
        if (rad <= 0 || w < rad * 2 || h < rad) { r.quad(x, y, w, h, c); return; }
        r.quad(x, y + rad, w, h - rad, c);
        r.quad(x + rad, y, w - rad * 2, rad, c);
        corner(r, x + rad, y + rad, rad, Math.PI, Math.PI * 1.5, c);
        corner(r, x + w - rad, y + rad, rad, Math.PI * 1.5, Math.PI * 2, c);
    }

    public static void gradientQuad(GuiRenderer r, double x, double y, double w, double h, Color top, Color bottom) {
        r.quad(x, y, w, h, top, top, bottom, bottom);
    }

    /** Rounded rect filled with a vertical gradient. */
    public static void roundedGradient(GuiRenderer r, double x, double y, double w, double h,
                                       double rad, Color top, Color bottom) {
        if (rad <= 0 || w < rad * 2 || h < rad * 2) { gradientQuad(r, x, y, w, h, top, bottom); return; }
        Color midTop = mix(top, bottom, rad / h);
        Color midBot = mix(top, bottom, (h - rad) / h);
        gradientQuad(r, x, y + rad, w, h - rad * 2, midTop, midBot);
        gradientQuad(r, x + rad, y, w - rad * 2, rad, top, midTop);
        gradientQuad(r, x + rad, y + h - rad, w - rad * 2, rad, midBot, bottom);
        corner(r, x + rad, y + rad, rad, Math.PI, Math.PI * 1.5, midTop);
        corner(r, x + w - rad, y + rad, rad, Math.PI * 1.5, Math.PI * 2, midTop);
        corner(r, x + w - rad, y + h - rad, rad, 0, Math.PI * 0.5, midBot);
        corner(r, x + rad, y + h - rad, rad, Math.PI * 0.5, Math.PI, midBot);
    }

    // ---------------------------------------------------------------- edges

    /** 1px hairline outline. */
    public static void outline(GuiRenderer r, double x, double y, double w, double h, double rad, double t, Color c) {
        if (w <= 0 || h <= 0) return;
        rad = Math.min(rad, Math.min(w, h) / 2);
        if (rad <= t) { r.quad(x, y, w, t, c); r.quad(x, y + h - t, w, t, c); r.quad(x, y, t, h, c); r.quad(x + w - t, y, t, h, c); return; }
        r.quad(x + rad, y, w - rad * 2, t, c);
        r.quad(x + rad, y + h - t, w - rad * 2, t, c);
        r.quad(x, y + rad, t, h - rad * 2, c);
        r.quad(x + w - t, y + rad, t, h - rad * 2, c);
        ringCorner(r, x + rad, y + rad, rad, t, Math.PI, Math.PI * 1.5, c);
        ringCorner(r, x + w - rad, y + rad, rad, t, Math.PI * 1.5, Math.PI * 2, c);
        ringCorner(r, x + w - rad, y + h - rad, rad, t, 0, Math.PI * 0.5, c);
        ringCorner(r, x + rad, y + h - rad, rad, t, Math.PI * 0.5, Math.PI, c);
    }

    /** 1px lighter line just inside the top edge. */
    public static void innerHighlight(GuiRenderer r, double x, double y, double w, double rad, Color c) {
        r.quad(x + rad, y, w - rad * 2, HAIRLINE, c);
    }

    /** Animated accent sweep along a horizontal edge. */
    public static void edgeGradient(GuiRenderer r, double x, double y, double w, double t, Color left, Color right) {
        r.quad(x, y, w, t, left, right);
    }

    /** Expanding bloom behind a shape. Draw before the shape itself. */
    public static void glow(GuiRenderer r, double x, double y, double w, double h, double rad, Color c, double spread) {
        int layers = 4;
        for (int i = layers; i >= 1; i--) {
            double e = spread * i / layers;
            roundedQuad(r, x - e, y - e, w + e * 2, h + e * 2, rad + e, withA(c, (int) (c.a * 0.10)));
        }
    }

    /** Soft drop shadow, offset downward. Draw before the shape itself. */
    public static void shadow(GuiRenderer r, double x, double y, double w, double h, double rad, Color c) {
        int layers = 6;
        for (int i = layers; i >= 1; i--) {
            double e = i * 1.6;
            roundedQuad(r, x - e, y - e + i * 0.9, w + e * 2, h + e * 2, rad + e, withA(c, (int) (c.a / (i + 2))));
        }
    }

    public static void divider(GuiRenderer r, double x, double y, double w, Color c) {
        r.quad(x, y, w, HAIRLINE, c);
    }

    // ---------------------------------------------------------------- type

    /** Vanilla font has no tracking, so draw char by char. */
    public static void tracked(GuiRenderer r, GuiTheme theme, String s, double x, double y,
                               Color c, double spacing, boolean title) {
        double cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            r.text(ch, cx, y, c, title);
            cx += theme.textWidth(ch) + spacing;
        }
    }

    public static void micro(GuiRenderer r, GuiTheme theme, String s, double x, double y, Color c) {
        tracked(r, theme, s.toUpperCase(), x, y, c, TRACKING, false);
    }

    // ---------------------------------------------------------------- widgets

    /** Card: gradient fill + hairline + inner highlight. */
    public static void card(GuiRenderer r, double x, double y, double w, double h, double rad,
                            Color fillTop, Color fillBottom, Color edge) {
        roundedGradient(r, x, y, w, h, rad, fillTop, fillBottom);
        outline(r, x, y, w, h, rad, HAIRLINE, edge);
        innerHighlight(r, x + 1, y + 1, w - 2, rad, mul(edge, 1.6));
    }

    /** Slim pill toggle. anim 0..1. */
    public static void toggle(GuiRenderer r, double x, double y, double w, double h,
                              double anim, Color track, Color on, Color knob) {
        double rad = h / 2;
        roundedQuad(r, x, y, w, h, rad, mix(track, on, anim));
        if (anim > 0.02) outline(r, x, y, w, h, rad, HAIRLINE, withA(on, (int) (60 * anim)));
        double pad = 2;
        double kx = x + pad + (w - h) * anim;
        circle(r, kx + (h - pad * 2) / 2, y + h / 2, (h - pad * 2) / 2, knob);
    }

    /** Single rounded track with a raised active pill. */
    public static void segmented(GuiRenderer r, GuiTheme theme, double x, double y, double w, double h,
                                 String[] options, int active, Color track, Color on, Color onText, Color offText) {
        roundedQuad(r, x, y, w, h, h / 2, track);
        double seg = w / options.length;
        if (active >= 0) {
            roundedQuad(r, x + seg * active + 2, y + 2, seg - 4, h - 4, (h - 4) / 2, on);
        }
        for (int i = 0; i < options.length; i++) {
            String s = options[i];
            double tx = x + seg * i + (seg - theme.textWidth(s)) / 2;
            r.text(s, tx, y + (h - theme.textHeight()) / 2, i == active ? onText : offText, false);
        }
    }

    /** Ultra-thin gradient slider with a crisp knob. */
    public static void slider(GuiRenderer r, double x, double y, double w, double value,
                              Color track, Color left, Color right, Color knob) {
        double th = 3;
        roundedQuad(r, x, y, w, th, th / 2, track);
        double fill = Math.max(th, w * value);
        roundedQuad(r, x, y, fill, th, th / 2, mix(left, right, value));
        circle(r, x + w * value, y + th / 2, 4, knob);
    }

    /** Vertical gradient accent bar. */
    public static void accentBar(GuiRenderer r, double x, double y, double w, double h, Color top, Color bottom) {
        roundedQuad(r, x, y, w, h, w / 2, mix(top, bottom, 0.5));
        gradientQuad(r, x, y, w, h, top, bottom);
    }
}

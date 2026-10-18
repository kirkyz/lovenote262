#!/usr/bin/env bash
set -euo pipefail

# ---- flags: --scanline | --no-push | --build
SCANLINE=0; PUSH=1; BUILD=0
for a in "$@"; do case "$a" in
  --scanline) SCANLINE=1 ;;
  --no-push)  PUSH=0 ;;
  --build)    BUILD=1 ;;
  *) echo "unknown flag: $a" >&2; exit 2 ;;
esac; done

# ---- must be at repo root
[ -d .git ] || { echo "!! run me from the repo root" >&2; exit 1; }

# ---- locate the screen and derive src root + package
SCREEN=$(find src -name 'LovenoteScreen.java' -print -quit 2>/dev/null || true)
if [ -n "$SCREEN" ]; then
  case "$SCREEN" in
    src/client/java/*) SRCROOT="src/client/java"; PKG="${SCREEN#src/client/java/}" ;;
    src/main/java/*)   SRCROOT="src/main/java";   PKG="${SCREEN#src/main/java/}" ;;
    *) echo "!! unexpected path: $SCREEN" >&2; exit 1 ;;
  esac
  PKG="${PKG%/*}"
else
  SRCROOT="${SRCROOT:-src/client/java}"
  PKG="${PKG:-}"
  [ -n "$PKG" ] || { echo "!! LovenoteScreen.java not found — rerun with PKG=your/pkg/path" >&2; exit 1; }
fi

PKGDOT="${PKG//\//.}"
BASE="$SRCROOT/$PKG"
mkdir -p "$BASE/util"

echo "repo   : $(pwd)"
echo "src    : $SRCROOT"
echo "package: $PKGDOT  (+ .util)"
echo

# ============================================================ Palette.java
cat > "$BASE/util/Palette.java" << 'EOF'
package dev.lovenote.util;

/** Modern glass design tokens. Point A1/A2/A3 at your theme accent to let Themes drive it. */
public final class Palette {
    private Palette() {}

    // accent sweep (cyan -> violet -> magenta)
    public static int A1 = 0xFF45C4FF;
    public static int A2 = 0xFFA855F7;
    public static int A3 = 0xFFFF4FD8;

    // surfaces
    public static int GLASS      = 0xC70B0B10;
    public static int GLASS_DEEP = 0xE607070B;
    public static int GLASS_RAIL = 0xD20E0E14;
    public static int CARD       = 0x9E121218;
    public static int CARD_DEEP  = 0xB00A0A0E;
    public static int CARD_HOVER = 0xB0161620;
    public static int TRACK      = 0x26FFFFFF;
    public static int BADGE      = 0x14FFFFFF;

    // edges
    public static int HAIRLINE   = 0x14FFFFFF;
    public static int INNER_TOP  = 0x1AFFFFFF;

    // type
    public static int TEXT       = 0xFFF4F4F8;
    public static int TEXT_DIM   = 0x9AA0A0AC;
    public static int TEXT_FAINT = 0x5E5E5E6C;

    // radii
    public static float R_WINDOW = 24f;
    public static float R_CARD   = 16f;
    public static float R_PANEL  = 18f;
    public static float MARGIN   = 0.045f;   // fraction of the smaller screen axis

    public static final int[] PRESETS = {
        0xFFEF5350, 0xFFFF9800, 0xFFFFC107, 0xFFCDDC39, 0xFF66BB6A, 0xFF26A69A,
        0xFF29B6F6, 0xFF5C6BC0, 0xFF7E57C2, 0xFFEC407A, 0xFFF5F5F5
    };

    public static int lerp(int a, int b, float t) {
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24)
             | ((int) (ar + (br - ar) * t) << 16)
             | ((int) (ag + (bg - ag) * t) << 8)
             |  (int) (ab + (bb - ab) * t);
    }

    public static int accent(float t) {
        t = ((t % 1f) + 1f) % 1f;
        return t < 0.5f ? lerp(A1, A2, t * 2f) : lerp(A2, A3, (t - 0.5f) * 2f);
    }

    /** Wall-clock phase so every widget animates in sync. */
    public static float phase() { return (System.currentTimeMillis() % 8000L) / 8000f; }

    public static int edge(float x, float w) { return accent(phase() + x / Math.max(1f, w)); }
}
EOF

# ============================================================ Glass.java
cat > "$BASE/util/Glass.java" << 'EOF'
package dev.lovenote.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tessellator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Rounded glass primitives.
 * BUFFERS = true  -> one vertex buffer per shape (needs Tessellator/BufferBuilder healthy)
 * BUFFERS = false -> pure ctx.fill() scanlines (works while the port is mid-flight)
 */
public final class Glass {
    private Glass() {}

    public static boolean BUFFERS = true;

    /** ONE place to fix if your mappings expose the GUI matrix differently. */
    private static Matrix4f mat(GuiGraphics ctx) { return new Matrix4f().set(ctx.pose()); }

    // ---------------------------------------------------------- geometry

    private static List<float[]> perimeter(float x, float y, float w, float h, float r, int seg) {
        List<float[]> p = new ArrayList<>();
        p.add(new float[]{x + r, y});
        p.add(new float[]{x + w - r, y});
        arc(p, x + w - r, y + r, r, -90, 0, seg);
        p.add(new float[]{x + w, y + h - r});
        arc(p, x + w - r, y + h - r, r, 0, 90, seg);
        p.add(new float[]{x + r, y + h});
        arc(p, x + r, y + h - r, r, 90, 180, seg);
        p.add(new float[]{x, y + r});
        arc(p, x + r, y + r, r, 180, 270, seg);
        return p;
    }

    private static void arc(List<float[]> p, float cx, float cy, float r, float a0, float a1, int seg) {
        for (int i = 1; i < seg; i++) {
            double a = Math.toRadians(a0 + (a1 - a0) * i / (double) seg);
            p.add(new float[]{cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r});
        }
    }

    private static void vtx(BufferBuilder bb, Matrix4f m, float x, float y, int c) {
        bb.addVertex(m, x, y, 0f).setColor(c);
    }

    private static BufferBuilder begin() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        return Tessellator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
    }

    private static void end(BufferBuilder bb) { BufferUploader.drawWithShader(bb.buildOrThrow()); }

    // ---------------------------------------------------------- fills

    public static void roundRect(GuiGraphics ctx, float x, float y, float w, float h, float r, int color) {
        roundRectGrad(ctx, x, y, w, h, r, color, color);
    }

    public static void roundRectGrad(GuiGraphics ctx, float x, float y, float w, float h,
                                     float r, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2f);
        if (!BUFFERS) { scanV(ctx, x, y, w, h, r, top, bottom); return; }
        Matrix4f m = mat(ctx);
        BufferBuilder bb = begin();
        int seg = Math.max(2, (int) Math.ceil(r));
        List<float[]> pts = perimeter(x, y, w, h, r, seg);
        float cx = x + w / 2f, cy = y + h / 2f;
        int mid = Palette.lerp(top, bottom, 0.5f);
        for (int i = 0; i < pts.size(); i++) {
            float[] a = pts.get(i), b = pts.get((i + 1) % pts.size());
            vtx(bb, m, cx, cy, mid);
            vtx(bb, m, a[0], a[1], Palette.lerp(top, bottom, (a[1] - y) / h));
            vtx(bb, m, b[0], b[1], Palette.lerp(top, bottom, (b[1] - y) / h));
        }
        end(bb);
    }

    public static void roundRectGradH(GuiGraphics ctx, float x, float y, float w, float h,
                                      float r, int left, int right) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2f);
        if (!BUFFERS) {
            int rows = (int) Math.ceil(h);
            for (int i = 0; i < rows; i++) {
                float inset = insetFor(i + 0.5f, h, r);
                int col = Palette.lerp(left, right, 0.5f);
                ctx.fill((int) (x + inset), (int) (y + i), (int) (x + w - inset + 1), (int) (y + i + 1), col);
            }
            return;
        }
        Matrix4f m = mat(ctx);
        BufferBuilder bb = begin();
        int seg = Math.max(2, (int) Math.ceil(r));
        List<float[]> pts = perimeter(x, y, w, h, r, seg);
        float cx = x + w / 2f, cy = y + h / 2f;
        int mid = Palette.lerp(left, right, 0.5f);
        for (int i = 0; i < pts.size(); i++) {
            float[] a = pts.get(i), b = pts.get((i + 1) % pts.size());
            vtx(bb, m, cx, cy, mid);
            vtx(bb, m, a[0], a[1], Palette.lerp(left, right, (a[0] - x) / w));
            vtx(bb, m, b[0], b[1], Palette.lerp(left, right, (b[0] - x) / w));
        }
        end(bb);
    }

    private static float insetFor(float dy, float h, float r) {
        if (dy < r) { float d = r - dy; return r - (float) Math.sqrt(Math.max(0, r * r - d * d)); }
        if (dy > h - r) { float d = dy - (h - r); return r - (float) Math.sqrt(Math.max(0, r * r - d * d)); }
        return 0f;
    }

    private static void scanV(GuiGraphics ctx, float x, float y, float w, float h, float r, int top, int bottom) {
        int rows = (int) Math.ceil(h);
        for (int i = 0; i < rows; i++) {
            float inset = insetFor(i + 0.5f, h, r);
            int col = Palette.lerp(top, bottom, (i + 0.5f) / h);
            ctx.fill((int) (x + inset), (int) (y + i), (int) (x + w - inset + 1), (int) (y + i + 1), col);
        }
    }

    // ---------------------------------------------------------- edges

    public static void hairline(GuiGraphics ctx, float x, float y, float w, float h,
                                float r, float t, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2f);
        if (!BUFFERS) {
            int rows = (int) Math.ceil(h);
            for (int i = 0; i < rows; i++) {
                float inset = insetFor(i + 0.5f, h, r);
                ctx.fill((int) (x + inset), (int) (y + i), (int) (x + inset + t), (int) (y + i + 1), color);
                ctx.fill((int) (x + w - inset - t), (int) (y + i), (int) (x + w - inset), (int) (y + i + 1), color);
            }
            ctx.fill((int) (x + r), (int) y, (int) (x + w - r), (int) (y + t), color);
            ctx.fill((int) (x + r), (int) (y + h - t), (int) (x + w - r), (int) (y + h), color);
            return;
        }
        Matrix4f m = mat(ctx);
        BufferBuilder bb = begin();
        int seg = Math.max(2, (int) Math.ceil(r));
        List<float[]> outer = perimeter(x, y, w, h, r, seg);
        List<float[]> inner = perimeter(x + t, y + t, w - 2 * t, h - 2 * t, Math.max(0, r - t), seg);
        for (int i = 0; i < outer.size(); i++) {
            int j = (i + 1) % outer.size();
            quad(bb, m, outer.get(i), outer.get(j), inner.get(j), inner.get(i), color);
        }
        end(bb);
    }

    private static void quad(BufferBuilder bb, Matrix4f m, float[] a, float[] b, float[] c, float[] d, int col) {
        vtx(bb, m, a[0], a[1], col); vtx(bb, m, b[0], b[1], col); vtx(bb, m, c[0], c[1], col);
        vtx(bb, m, a[0], a[1], col); vtx(bb, m, c[0], c[1], col); vtx(bb, m, d[0], d[1], col);
    }

    public static void gradientEdge(GuiGraphics ctx, float x, float y, float w, float t) {
        int steps = Math.max(1, (int) (w / 3f));
        float seg = w / steps;
        for (int i = 0; i < steps; i++) {
            float f = i / (float) steps;
            ctx.fill((int) (x + seg * i), (int) y, (int) (x + seg * i + seg + 1), (int) (y + t),
                     Palette.accent(Palette.phase() + f));
        }
    }

    // ---------------------------------------------------------- depth

    public static void shadow(GuiGraphics ctx, float x, float y, float w, float h, float r, int strength) {
        for (int i = 8; i >= 1; i--) {
            float e = i * 1.6f;
            int a = ((strength >>> 24) & 0xFF) / (i + 2);
            roundRect(ctx, x - e, y - e + i * 0.9f, w + e * 2, h + e * 2, r + e,
                      (a << 24) | (strength & 0xFFFFFF));
        }
    }

    public static void glow(GuiGraphics ctx, float x, float y, float w, float h, float r, int color, float spread) {
        for (int i = 5; i >= 1; i--) {
            float e = spread * i / 5f;
            roundRect(ctx, x - e, y - e, w + e * 2, h + e * 2, r + e, ((26 / i) << 24) | (color & 0xFFFFFF));
        }
    }

    public static void innerHighlight(GuiGraphics ctx, float x, float y, float w, float r) {
        hairline(ctx, x, y, w, 2f, r, 1f, Palette.INNER_TOP);
    }

    // ---------------------------------------------------------- type

    /** Vanilla font has no tracking, so draw char by char. */
    public static void tracked(GuiGraphics ctx, Font font, String s, float x, float y, int color, float spacing) {
        float cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            ctx.drawString(font, ch, (int) cx, (int) y, color, false);
            cx += font.width(ch) + spacing;
        }
    }

    public static void micro(GuiGraphics ctx, Font font, String s, float x, float y) {
        tracked(ctx, font, s.toUpperCase(), x, y, Palette.TEXT_FAINT, 1f);
    }

    // ---------------------------------------------------------- widgets

    public static void toggle(GuiGraphics ctx, float x, float y, float w, float h, float anim, float accentT) {
        float r = h / 2f;
        roundRect(ctx, x, y, w, h, r, Palette.lerp(Palette.TRACK, Palette.accent(accentT), anim));
        float pad = 2f;
        roundRect(ctx, x + pad + (w - h) * anim, y + pad, h - pad * 2, h - pad * 2, (h - pad * 2) / 2f, 0xFFFFFFFF);
    }

    public static void segmented(GuiGraphics ctx, Font font, float x, float y,
                                 float w, float h, String[] options, int active, float accentT) {
        roundRect(ctx, x, y, w, h, h / 2f, Palette.TRACK);
        float seg = w / options.length;
        if (active >= 0) {
            roundRectGradH(ctx, x + seg * active + 2, y + 2, seg - 4, h - 4, (h - 4) / 2f,
                           Palette.accent(accentT), Palette.accent(accentT + 0.25f));
        }
        for (int i = 0; i < options.length; i++) {
            float tx = x + seg * i + (seg - font.width(options[i])) / 2f;
            ctx.drawString(font, options[i], (int) tx, (int) (y + (h - 8) / 2f),
                           i == active ? 0xFFFFFFFF : Palette.TEXT_DIM, false);
        }
    }

    public static void slider(GuiGraphics ctx, float x, float y, float w, float value, float accentT) {
        float th = 4f;
        roundRect(ctx, x, y, w, th, th / 2f, Palette.TRACK);
        roundRectGradH(ctx, x, y, Math.max(th, w * value), th, th / 2f,
                       Palette.accent(accentT), Palette.accent(accentT + 0.25f));
        roundRect(ctx, x + w * value - 4f, y - 2f, 8f, 8f, 4f, 0xFFFFFFFF);
    }

    public static float badge(GuiGraphics ctx, Font font, String s, float x, float y, float h) {
        float w = font.width(s) + 12f;
        roundRect(ctx, x, y, w, h, h / 2f, Palette.BADGE);
        ctx.drawString(font, s, (int) (x + 6f), (int) (y + (h - 8) / 2f), Palette.TEXT_DIM, false);
        return w;
    }
}
EOF

# ============================================================ ModernMenu.java
cat > "$BASE/util/ModernMenu.java" << 'EOF'
package dev.lovenote.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Full-screen modern menu layout. Pure drawing — no state, no module coupling. */
public final class ModernMenu {
    private ModernMenu() {}

    public record Tab(String label) {}
    public record Row(String name, String desc, boolean enabled, float anim) {}

    /** Hitboxes returned so the screen can route clicks. */
    public static final class Layout {
        public final List<float[]> tabs = new ArrayList<>();
        public final List<float[]> rows = new ArrayList<>();
        public final float[] search = new float[4];
        public final float[] drawer = new float[4];
    }

    public static Layout draw(GuiGraphics ctx, Font font, int mx, int my, int sw, int sh,
                              String query, int activeTab, List<Tab> tabs, List<Row> rows,
                              float drawerAnim, boolean drawerOpen, float scroll) {
        Layout L = new Layout();
        float m  = Math.max(18f, Math.min(sw, sh) * Palette.MARGIN);
        float wx = m, wy = m, ww = sw - m * 2, wh = sh - m * 2;
        float headerH = 52f;

        // ---- window
        Glass.shadow(ctx, wx, wy, ww, wh, Palette.R_WINDOW, 0x99000000);
        Glass.roundRectGrad(ctx, wx, wy, ww, wh, Palette.R_WINDOW, Palette.GLASS, Palette.GLASS_DEEP);
        Glass.hairline(ctx, wx, wy, ww, wh, Palette.R_WINDOW, 1f, Palette.HAIRLINE);
        Glass.gradientEdge(ctx, wx + 1, wy + 0.5f, ww - 2, 1f);
        Glass.innerHighlight(ctx, wx + 1, wy + 1, ww - 2, Palette.R_WINDOW);

        // ---- header
        float hx = wx + 18f, hy = wy + 16f;
        Glass.roundRect(ctx, hx, hy, 20f, 20f, 7f, Palette.lerp(Palette.accent(Palette.phase()), 0xFFFFFFFF, 0.15f));
        ctx.drawString(font, "lovenote", (int) (hx + 28f), (int) (hy + 6f), Palette.TEXT, true);

        float swid = Math.min(340f, ww * 0.34f), sx = wx + (ww - swid) / 2f;
        Glass.roundRect(ctx, sx, hy, swid, 24f, 12f, Palette.TRACK);
        Glass.hairline(ctx, sx, hy, swid, 24f, 12f, 1f, Palette.HAIRLINE);
        Glass.tracked(ctx, font, "/", sx + 12f, hy + 8f, Palette.TEXT_FAINT, 0f);
        Glass.tracked(ctx, font, query.isEmpty() ? "Search modules..." : query,
                      sx + 30f, hy + 8f, query.isEmpty() ? Palette.TEXT_FAINT : Palette.TEXT, 0.4f);
        System.arraycopy(new float[]{sx, hy, swid, 24f}, 0, L.search, 0, 4);

        float rx = wx + ww - 18f;
        for (int i = 0; i < 3; i++) Glass.roundRect(ctx, rx - 34f + i * 11f, hy + 10f, 6f, 6f, 3f, Palette.TEXT_FAINT);
        Glass.roundRect(ctx, rx - 58f, hy + 2f, 20f, 20f, 10f, Palette.TRACK);

        // ---- tabs
        float tx = wx + 18f, ty = wy + headerH + 2f;
        for (int i = 0; i < tabs.size(); i++) {
            String label = tabs.get(i).label();
            float tw = font.width(label) + 34f;
            if (tx + tw > wx + ww - 18f) break;
            boolean on = i == activeTab;
            if (on) {
                Glass.glow(ctx, tx, ty, tw, 26f, 13f, Palette.accent(Palette.phase() + i * 0.08f), 5f);
                Glass.roundRectGradH(ctx, tx, ty, tw, 26f, 13f,
                                     Palette.accent(Palette.phase()), Palette.accent(Palette.phase() + 0.25f));
            } else {
                Glass.roundRect(ctx, tx, ty, tw, 26f, 13f, Palette.TRACK);
            }
            ctx.drawString(font, label, (int) (tx + 17f), (int) (ty + 9f),
                           on ? 0xFFFFFFFF : Palette.TEXT_DIM, false);
            L.tabs.add(new float[]{tx, ty, tw, 26f});
            tx += tw + 8f;
        }

        // ---- heading
        float cy = ty + 26f + 20f;
        String title = activeTab >= 0 && activeTab < tabs.size() ? tabs.get(activeTab).label() : "All";
        Glass.micro(ctx, font, title, wx + 18f, cy);
        ctx.drawString(font, title, (int) (wx + 18f), (int) (cy + 12f), Palette.TEXT, true);
        Glass.badge(ctx, font, String.valueOf(rows.size()),
                    wx + 18f + font.width(title) + 10f, cy + 12f, 14f);

        // ---- grid
        float drawerW = 236f, gridX = wx + 18f;
        float gridW = ww - 36f - drawerW - 14f;
        float cardW = (gridW - 24f) / 3f, cardH = 62f;
        float gridTop = cy + 34f, gridBottom = wy + wh - 16f;

        ctx.enableScissor((int) gridX, (int) gridTop, (int) (gridX + gridW), (int) gridBottom);
        int cols = 3;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            float cxx = gridX + (i % cols) * (cardW + 12f);
            float cyy = gridTop + (i / cols) * (cardH + 12f) - scroll;
            if (cyy + cardH < gridTop - 8 || cyy > gridBottom + 8) continue;

            boolean hov = mx >= cxx && mx <= cxx + cardW && my >= cyy && my <= cyy + cardH;
            Glass.roundRectGrad(ctx, cxx, cyy, cardW, cardH, Palette.R_CARD,
                                hov ? Palette.CARD_HOVER : Palette.CARD, Palette.CARD_DEEP);
            Glass.hairline(ctx, cxx, cyy, cardW, cardH, Palette.R_CARD, 1f, Palette.HAIRLINE);
            if (row.enabled()) {
                Glass.roundRectGradH(ctx, cxx + 1.5f, cyy + 8f, 2f, cardH - 16f, 1f,
                                     Palette.accent(Palette.phase()), Palette.accent(Palette.phase() + 0.3f));
            }
            ctx.drawString(font, row.name(), (int) (cxx + 14f), (int) (cyy + 16f), Palette.TEXT, true);
            ctx.drawString(font, row.desc(), (int) (cxx + 14f), (int) (cyy + 30f), Palette.TEXT_DIM, false);
            Glass.toggle(ctx, cxx + cardW - 42f, cyy + cardH - 26f, 28f, 14f,
                         row.anim(), Palette.phase() + (i % 5) * 0.1f);
            L.rows.add(new float[]{cxx, cyy, cardW, cardH});
        }
        ctx.disableScissor();

        // ---- drawer (drawn last: wins on visuals and on input order)
        float dx = wx + ww - drawerW - 14f + (drawerOpen ? 0f : drawerW * 0.6f);
        float dy = wy + headerH + 2f, dh = wh - headerH - 20f;
        Glass.shadow(ctx, dx, dy, drawerW, dh, Palette.R_PANEL, 0x88000000);
        Glass.roundRectGrad(ctx, dx, dy, drawerW, dh, Palette.R_PANEL, Palette.GLASS_RAIL, Palette.GLASS_DEEP);
        Glass.hairline(ctx, dx, dy, drawerW, dh, Palette.R_PANEL, 1f, Palette.HAIRLINE);
        Glass.gradientEdge(ctx, dx + 1, dy + 0.5f, drawerW - 2, 1f);
        System.arraycopy(new float[]{dx, dy, drawerW, dh}, 0, L.drawer, 0, 4);

        ctx.drawString(font, "Settings", (int) (dx + 16f), (int) (dy + 16f), Palette.TEXT, true);
        Glass.micro(ctx, font, "TOGGLES", dx + 16f, dy + 40f);
        Glass.micro(ctx, font, "VALUES", dx + 16f, dy + 76f);
        Glass.micro(ctx, font, "MODES", dx + 16f, dy + 128f);
        Glass.toggle(ctx, dx + drawerW - 44f, dy + 36f, 28f, 14f, 1f, Palette.phase());
        Glass.slider(ctx, dx + 16f, dy + 92f, drawerW - 32f, 0.62f, Palette.phase());
        Glass.segmented(ctx, font, dx + 16f, dy + 142f, drawerW - 32f, 20f,
                        new String[]{"A", "B", "C"}, 0, Palette.phase());

        return L;
    }
}
EOF

# ---- retarget the package line to whatever we detected
for f in Palette Glass ModernMenu; do
  sed -i "s|^package dev\.lovenote\.util;|package ${PKGDOT}.util;|" "$BASE/util/$f.java"
done

if [ "$SCANLINE" -eq 1 ]; then
  sed -i 's/public static boolean BUFFERS = true;/public static boolean BUFFERS = false;/' "$BASE/util/Glass.java"
  echo "-> scanline mode (no vertex buffers)"
fi

git add -A
git commit -m "feat(ui): modern glass tokens, primitives and full-screen menu layout" || true
[ "$PUSH" -eq 1 ] && git push

echo
echo "done. wrote:"
echo "  $BASE/util/Palette.java"
echo "  $BASE/util/Glass.java"
echo "  $BASE/util/ModernMenu.java"
echo
[ "$BUILD" -eq 1 ] && ./gradlew compileJava --console=plain || true

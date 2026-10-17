#!/usr/bin/env bash
# =====================================================================
#  lovenote262 - full client content pack
#  UI/HUD layer (logo, ripples, style) + visual pack (cubes, vortex,
#  arcs, prism rain, sky extras). Self-pruning: only registers modules
#  whose source file actually exists, so a partial state still builds.
#  Safe to run twice.
# =====================================================================
set -u

cd "$(dirname "${BASH_SOURCE[0]}")" 2>/dev/null || true
[ -f settings.gradle ] || cd /workspaces/lovenote262 2>/dev/null || true
if [ ! -f settings.gradle ]; then
  echo "!! run me from the repo root (settings.gradle not found)"; exit 1
fi

mkdir -p src/main/java/com/lovenote/util
mkdir -p src/main/java/com/lovenote/module/impl
mkdir -p run src/main/resources/assets/lovenote/textures

echo "== writing sources =="

# ------------------------------------------------------------------ Gfx
cat > src/main/java/com/lovenote/util/Gfx.java <<'EOF'
package com.lovenote.util;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Reflection bridge for members reshuffled by the 26.x retained-mode GUI rewrite. */
public final class Gfx {
	private Gfx() {}

	private static boolean resolved;
	private static Method mText, mBlit, mGuiW, mGuiH;
	private static Object pipeline;

	public static void resolve() {
		if (resolved) return;
		resolved = true;

		Object font = null;
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc != null) font = mc.font;
		} catch (Throwable ignored) {}

		for (Method m : GuiGraphicsExtractor.class.getMethods()) {
			String n = m.getName();
			Class<?>[] p = m.getParameterTypes();
			if (mText == null && font != null && (n.equals("text") || n.equals("drawString"))
					&& p.length >= 5 && p[0].isInstance(font)
					&& p[1] == String.class && p[2] == int.class && p[3] == int.class && p[4] == int.class) {
				mText = m;
			}
			if (mBlit == null && n.equals("blit") && p.length == 12
					&& named(p[0], "RenderPipeline") && named(p[1], "Identifier", "ResourceLocation")) {
				mBlit = m;
			}
		}

		String[] pipes = {
				"net.minecraft.client.renderer.RenderPipelines",
				"net.minecraft.client.gl.RenderPipelines",
				"com.mojang.blaze3d.pipeline.RenderPipelines"
		};
		for (String cn : pipes) {
			if (pipeline != null) break;
			try { pipeline = Class.forName(cn).getField("GUI_TEXTURED").get(null); }
			catch (Throwable ignored) {}
		}

		try { mGuiW = Window.class.getMethod("getGuiScaledWidth"); } catch (Throwable ignored) {}
		try { mGuiH = Window.class.getMethod("getGuiScaledHeight"); } catch (Throwable ignored) {}

		System.out.println("[lovenote] gfx bridge: text=" + (mText != null)
				+ " blit=" + (mBlit != null) + " pipeline=" + (pipeline != null)
				+ " guiSize=" + (mGuiW != null));
	}

	private static boolean named(Class<?> c, String... names) {
		String s = c.getSimpleName();
		for (String n : names) if (s.equals(n)) return true;
		return false;
	}

	public static void text(GuiGraphicsExtractor g, String s, int x, int y, int col) {
		resolve();
		if (mText == null) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.font == null) return;
		try {
			Class<?>[] p = mText.getParameterTypes();
			if (p.length >= 6) {
				Object extra = p[5] == boolean.class ? Boolean.TRUE
						: p[5] == int.class ? Integer.valueOf(0) : null;
				if (extra != null) mText.invoke(g, mc.font, s, x, y, col, extra);
				else mText.invoke(g, mc.font, s, x, y, col);
			} else {
				mText.invoke(g, mc.font, s, x, y, col);
			}
		} catch (Throwable ignored) {}
	}

	public static void blit(GuiGraphicsExtractor g, Identifier tex, int x, int y, float u, float v,
							int w, int h, int rw, int rh, int tw, int th) {
		resolve();
		if (mBlit == null || pipeline == null) return;
		try {
			Class<?>[] p = mBlit.getParameterTypes();
			Object uu = p[4] == float.class ? (Object) u : (Object) (int) u;
			Object vv = p[5] == float.class ? (Object) v : (Object) (int) v;
			mBlit.invoke(g, pipeline, tex, x, y, uu, vv, w, h, rw, rh, tw, th);
		} catch (Throwable ignored) {}
	}

	public static int guiW() {
		resolve();
		Minecraft mc = Minecraft.getInstance();
		if (mc == null) return 320;
		try {
			if (mGuiW != null) return (int) mGuiW.invoke(mc.getWindow());
			return mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getGuiScale());
		} catch (Throwable t) { return 320; }
	}

	public static int guiH() {
		resolve();
		Minecraft mc = Minecraft.getInstance();
		if (mc == null) return 240;
		try {
			if (mGuiH != null) return (int) mGuiH.invoke(mc.getWindow());
			return mc.getWindow().getHeight() / Math.max(1, mc.getWindow().getGuiScale());
		} catch (Throwable t) { return 240; }
	}

	public static File gameDir() {
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc != null) {
				Field f = Minecraft.class.getField("gameDirectory");
				Object o = f.get(mc);
				if (o instanceof File file) return file;
			}
		} catch (Throwable ignored) {}
		return new File(".");
	}
}
EOF

# ------------------------------------------------------------------ Ui
cat > src/main/java/com/lovenote/util/Ui.java <<'EOF'
package com.lovenote.util;

import com.lovenote.module.impl.Interface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Global UI style + 2D primitives. Values are pushed from the Interface module. */
public final class Ui {
	private Ui() {}

	private static Minecraft mc() { return Minecraft.getInstance(); }

	public static int accent1 = 0xFFFF9ACB, accent2 = 0xFFB28DFF;
	public static int bg = 0xE60E0E14, bg2 = 0xE61A1A24;
	public static int border = 0x40FFFFFF;
	public static int radius = 6;
	public static int borderWidth = 1;
	public static int shadowSize = 4, shadowAlpha = 70;
	public static int glowSize = 5, glowAlpha = 45;
	public static boolean glass = true, shadowOn = true, glowOn = true;
	public static boolean gradientOn = true;
	public static float scale = 1f;
	public static int textCol = 0xFFE8E8F0, textDim = 0xFF8A8A9A;
	public static boolean textShadow = true;
	public static boolean pillStyle = true;
	public static boolean accentBorder = true;

	private static int lastAccent = -1;

	public static void push(Interface i) {
		if (i == null) return;
		int idx = i.accent.index();
		if (idx != lastAccent) {
			lastAccent = idx;
			int[] pr = Colors.PRESETS[Math.min(idx, Colors.PRESETS.length - 1)];
			Colors.setAccent(pr[0], pr[1]);
			Logos.invalidate();
		}
		Colors.animated = i.animated.get();
		Colors.dark = i.theme.is("Dark");

		int a = i.bgOpacity.getI();
		int tint = i.bgTint.is("Black") ? 0x000000 : i.bgTint.is("Accent") ? Colors.C3 : 0x0E0E14;
		int tint2 = i.bgTint.is("Black") ? 0x000000 : i.bgTint.is("Accent") ? Colors.C1 : 0x1A1A24;
		bg = Colors.alpha(tint, a);
		bg2 = Colors.alpha(tint2, a);

		glass = i.glass.get();
		gradientOn = i.gradientPanel.get();
		radius = i.panelRadius.getI();
		borderWidth = i.borderWidth.getI();
		border = Colors.alpha(i.accentBorder.get() ? Colors.C3 : 0xFFFFFF, i.borderAlpha.getI());
		shadowOn = i.shadowOn.get();
		shadowSize = i.shadowSize.getI();
		shadowAlpha = i.shadowAlpha.getI();
		glowOn = i.glowOn.get();
		glowSize = i.glowSize.getI();
		glowAlpha = i.glowAlpha.getI();
		scale = i.widgetScale.getF();
		textShadow = i.textShadow.get();
		pillStyle = i.pillStyle.is("Pill");
		accentBorder = i.accentBorder.get();
		textCol = Colors.dark ? 0xFFE8E8F0 : 0xFF1A1A22;
		textDim = Colors.dark ? 0xFF8A8A9A : 0xFF6A6A78;
		accent1 = Colors.C1;
		accent2 = Colors.C3;

		Logos.setTint(i.logoColour.is("White") ? 1 : i.logoColour.is("Rainbow") ? 2
				: i.logoColour.is("Original") ? 3 : 0);
		Logos.setShape(i.logoCropTop.getF() / 100f, i.logoCropBottom.getF() / 100f,
				i.logoZoom.getF(), i.logoKeyBlack.get());

		MenuFx.enabled = i.menuFx.get();
		MenuFx.onlyMeteor = i.menuFxMeteorOnly.get();
		MenuFx.rippleOn = i.rippleOn.get();
		MenuFx.edge = i.rippleEdge.getI();
		MenuFx.maxSize = i.rippleSize.getI();
		MenuFx.life = i.rippleLife.getI();
		MenuFx.rippleAlpha = i.rippleAlpha.getI();
		MenuFx.width = i.rippleWidth.getI();
		MenuFx.vignette = i.menuVignette.get();
		MenuFx.scanlines = i.menuScanlines.get();
		MenuFx.grain = i.menuGrain.getI();
		MenuFx.edgeGlow = i.menuEdgeGlow.get();
		MenuFx.edgeGlowAlpha = i.menuEdgeGlowAlpha.getI();
		MenuFx.logoInMenu = i.logoInMenu.get();
		MenuFx.logoMenuSize = i.logoMenuSize.getI();
		MenuFx.logoMenuCorner = i.logoMenuCorner.get();
		MenuFx.logoSpin = i.logoMenuSpin.getF();
	}

	public static void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int col) {
		if (w <= 0 || h <= 0) return;
		g.fill(x, y, x + w, y + h, col);
	}

	public static void rrect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int col) {
		if (w <= 0 || h <= 0) return;
		if (r <= 1 || w < 4 || h < 4) { g.fill(x, y, x + w, y + h, col); return; }
		r = Math.min(r, Math.min(w, h) / 2);
		for (int dy = 0; dy < h; dy++) {
			int inset = 0;
			if (dy < r) {
				double d = r - dy - 0.5;
				inset = r - (int) Math.round(Math.sqrt(Math.max(0, r * r - d * d)));
			} else if (dy >= h - r) {
				double d = r - (h - dy) + 0.5;
				inset = r - (int) Math.round(Math.sqrt(Math.max(0, r * r - d * d)));
			}
			g.fill(x + inset, y + dy, x + w - inset, y + dy + 1, col);
		}
	}

	public static void rrectGrad(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int c1, int c2) {
		if (w <= 0 || h <= 0) return;
		if (r <= 1 || w < 4 || h < 4) { gradV(g, x, y, w, h, c1, c2); return; }
		int rr = Math.min(r, Math.min(w, h) / 2);
		for (int dy = 0; dy < h; dy++) {
			int inset = 0;
			if (dy < rr) {
				double d = rr - dy - 0.5;
				inset = rr - (int) Math.round(Math.sqrt(Math.max(0, rr * rr - d * d)));
			} else if (dy >= h - rr) {
				double d = rr - (h - dy) + 0.5;
				inset = rr - (int) Math.round(Math.sqrt(Math.max(0, rr * rr - d * d)));
			}
			float t = h == 1 ? 0f : dy / (float) (h - 1);
			g.fill(x + inset, y + dy, x + w - inset, y + dy + 1, Colors.lerp(c1, c2, t));
		}
	}

	public static void gradV(GuiGraphicsExtractor g, int x, int y, int w, int h, int c1, int c2) {
		if (w <= 0 || h <= 0) return;
		for (int dy = 0; dy < h; dy++) {
			float t = h == 1 ? 0f : dy / (float) (h - 1);
			g.fill(x, y + dy, x + w, y + dy + 1, Colors.lerp(c1, c2, t));
		}
	}

	public static void gradH(GuiGraphicsExtractor g, int x, int y, int w, int h, int c1, int c2) {
		if (w <= 0 || h <= 0) return;
		for (int dx = 0; dx < w; dx++) {
			float t = w == 1 ? 0f : dx / (float) (w - 1);
			g.fill(x + dx, y, x + dx + 1, y + h, Colors.lerp(c1, c2, t));
		}
	}

	public static void glowRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int col, int alpha) {
		if (!glowOn || glowSize <= 0 || alpha <= 0) return;
		for (int i = glowSize; i >= 1; i--) {
			int a = (int) (alpha * (1f - i / (float) (glowSize + 1)));
			if (a < 2) continue;
			rrect(g, x - i, y - i, w + i * 2, h + i * 2, r + i, Colors.alpha(col, a));
		}
	}

	public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		int r = pillStyle ? Math.max(2, h / 2) : radius;
		if (shadowOn && shadowSize > 0) {
			for (int i = shadowSize; i >= 1; i--) {
				int a = (int) (shadowAlpha * (1f - i / (float) (shadowSize + 1)));
				if (a < 2) continue;
				rrect(g, x - i, y - i + 1, w + i * 2, h + i * 2, r + i, Colors.alpha(0x000000, a));
			}
		}
		if (glowOn) glowRect(g, x, y, w, h, r, Colors.C3, glowAlpha);
		if (gradientOn) rrectGrad(g, x, y, w, h, r, bg, bg2);
		else rrect(g, x, y, w, h, r, bg);
		if (glass) rrect(g, x + 1, y + 1, w - 2, Math.max(1, h / 3), r, Colors.alpha(0xFFFFFF, 14));
		for (int i = 0; i < borderWidth; i++)
			rrect(g, x - i, y - i, w + i * 2, h + i * 2, r + i, border);
	}

	public static void pill(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		int r = Math.max(2, h / 2);
		if (shadowOn && shadowSize > 0) {
			for (int i = shadowSize; i >= 1; i--) {
				int a = (int) (shadowAlpha * (1f - i / (float) (shadowSize + 1)));
				if (a < 2) continue;
				rrect(g, x - i, y - i + 1, w + i * 2, h + i * 2, r + i, Colors.alpha(0x000000, a));
			}
		}
		if (glowOn) glowRect(g, x, y, w, h, r, Colors.C3, glowAlpha);
		rrectGrad(g, x, y, w, h, r, bg, bg2);
		rrect(g, x + 1, y + 1, w - 2, Math.max(1, h / 3), r, Colors.alpha(0xFFFFFF, 14));
		for (int i = 0; i < borderWidth; i++)
			rrect(g, x - i, y - i, w + i * 2, h + i * 2, r + i, border);
	}

	public static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		if (w <= 0) return;
		gradH(g, x, y, w, h, Colors.C1, Colors.C3);
	}

	public static void text(GuiGraphicsExtractor g, String s, int x, int y, int col) {
		Gfx.text(g, s, x, y, col);
	}

	public static int textW(String s) {
		Minecraft m = mc();
		if (m == null || m.font == null) return s.length() * 6;
		return m.font.width(s);
	}
}
EOF

# ------------------------------------------------------------------ Logos
cat > src/main/java/com/lovenote/util/Logos.java <<'EOF'
package com.lovenote.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/** Runtime logo atlas. "Image" uses your PNG, baked into 24 pre-rotated frames. */
public final class Logos {
	private Logos() {}

	public static final int CELL = 64;
	public static final int FRAMES = 24;
	public static final String[] STYLES = {
			"Image", "Heart", "Monogram", "Star", "Crystal", "Infinity", "Sparkle", "Crown", "Wing"
	};
	private static final int IMAGE_STYLE = 0;
	private static final int FALLBACK_STYLE = 2;

	private static final Identifier ID = Identifier.fromNamespaceAndPath("lovenote", "gen/logo");

	public static float cropTop = 0.24f, cropBottom = 0f, zoom = 1f;
	public static boolean keyBlack = true;
	public static int tintMode = 0;

	private static NativeImage src;
	private static boolean srcTried;
	private static String srcFrom = "none";

	private static boolean built;
	private static int builtStyle = -1, builtTint = -1, builtC1, builtC3;
	private static float builtCropT = -1f, builtCropB = -1f, builtZoom = -1f;
	private static boolean builtKey;

	public static void invalidate() { built = false; }
	public static void setStyle(int i) { if (i != builtStyle) built = false; }
	public static void setTint(int t) { if (t != tintMode) { tintMode = t; built = false; } }
	public static void setShape(float ct, float cb, float z, boolean key) {
		if (ct != cropTop || cb != cropBottom || z != zoom || key != keyBlack) built = false;
		cropTop = ct; cropBottom = cb; zoom = z; keyBlack = key;
	}

	private static void loadSource() {
		if (srcTried) return;
		srcTried = true;
		try {
			File f = new File(Gfx.gameDir(), "lovenote-logo.png");
			if (f.isFile()) {
				try (InputStream in = new FileInputStream(f)) { src = NativeImage.read(in); }
				if (src != null) srcFrom = f.getAbsolutePath();
			}
			if (src == null) {
				InputStream in = Logos.class.getResourceAsStream("/assets/lovenote/textures/logo.png");
				if (in != null) {
					try (InputStream i2 = in) { src = NativeImage.read(i2); }
					if (src != null) srcFrom = "assets/lovenote/textures/logo.png";
				}
			}
		} catch (Throwable t) {
			src = null;
		}
		if (src == null) {
			srcTried = false;
			System.out.println("[lovenote] no lovenote-logo.png found, using the procedural fallback mark");
		} else {
			System.out.println("[lovenote] logo loaded from " + srcFrom
					+ " (" + src.getWidth() + "x" + src.getHeight() + ")");
		}
	}

	public static void ensure(int style, int tint) {
		if (built && builtStyle == style && builtTint == tint
				&& builtC1 == Colors.C1 && builtC3 == Colors.C3
				&& builtCropT == cropTop && builtCropB == cropBottom
				&& builtZoom == zoom && builtKey == keyBlack) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc == null) return;

		try {
			RenderSystem.assertOnRenderThread();

			int eff = style;
			if (style == IMAGE_STYLE) {
				loadSource();
				if (src == null) eff = FALLBACK_STYLE;
			}

			NativeImage img = (eff == IMAGE_STYLE) ? bakeImage(tint) : bakeShape(eff, tint);
			if (img == null) return;

			mc.getTextureManager().register(ID, new DynamicTexture(() -> "lovenote_logo", img));

			built = true;
			builtStyle = style;
			builtTint = tint;
			builtC1 = Colors.C1;
			builtC3 = Colors.C3;
			builtCropT = cropTop;
			builtCropB = cropBottom;
			builtZoom = zoom;
			builtKey = keyBlack;
		} catch (Throwable t) {
			built = false;
			t.printStackTrace();
		}
	}

	private static NativeImage bakeImage(int tint) {
		int sw = src.getWidth(), sh = src.getHeight();
		int[] pix = src.getPixels();
		int y0 = (int) (sh * Math.max(0f, Math.min(0.8f, cropTop)));
		int y1 = (int) (sh * (1f - Math.max(0f, Math.min(0.8f, cropBottom))));
		int ch = Math.max(1, y1 - y0);

		double diag = Math.sqrt((double) sw * sw + (double) ch * ch);
		double k = CELL / diag * Math.max(0.2f, zoom);
		int cy = y0 + ch / 2;

		NativeImage img = new NativeImage(CELL * FRAMES, CELL, false);
		int c1 = tintColour(tint, 0f), c3 = tintColour(tint, 1f);

		for (int f = 0; f < FRAMES; f++) {
			double ang = f * Math.PI * 2 / FRAMES;
			double ca = Math.cos(ang), sa = Math.sin(ang);

			for (int py = 0; py < CELL; py++) {
				for (int px = 0; px < CELL; px++) {
					int accA = 0, accR = 0, accG = 0, accB = 0, hits = 0;
					for (int sy = 0; sy < 3; sy++) {
						for (int sx = 0; sx < 3; sx++) {
							double ox = px + (sx + 0.5) / 3.0 - CELL * 0.5;
							double oy = py + (sy + 0.5) / 3.0 - CELL * 0.5;
							double ux = ox * ca + oy * sa;
							double uy = -ox * sa + oy * ca;
							int sxp = (int) (ux / k + sw * 0.5);
							int syp = (int) (uy / k + cy);
							if (sxp < 0 || syp < y0 || sxp >= sw || syp >= y1) continue;
							int v = pix[syp * sw + sxp];
							int a = (v >>> 24) & 255;
							int b = (v >>> 16) & 255;
							int g = (v >>> 8) & 255;
							int r = v & 255;
							if (keyBlack) a = Math.min(a, (r * 30 + g * 59 + b * 11) / 100);
							accA += a; accR += r; accG += g; accB += b; hits++;
						}
					}
					if (hits == 0) { img.setPixelABGR(px + f * CELL, py, 0); continue; }
					int a = accA / hits;
					if (a <= 0) { img.setPixelABGR(px + f * CELL, py, 0); continue; }

					int col;
					if (tint == 3) col = ((accR / hits) << 16) | ((accG / hits) << 8) | (accB / hits);
					else col = Colors.lerp(c1, c3, py / (float) (CELL - 1)) & 0xFFFFFF;

					img.setPixelABGR(px + f * CELL, py,
							argb(a, (col >> 16) & 255, (col >> 8) & 255, col & 255));
				}
			}
		}
		return img;
	}

	private static int tintColour(int tint, float t) {
		if (tint == 1) return 0xFFFFFF;
		if (tint == 2) return Colors.rainbow(t) & 0xFFFFFF;
		return Colors.lerp(Colors.C1, Colors.C3, t) & 0xFFFFFF;
	}

	private static int argb(int a, int r, int g, int b) { return (a << 24) | (b << 16) | (g << 8) | r; }

	private static NativeImage bakeShape(int style, int tint) {
		NativeImage img = new NativeImage(CELL * FRAMES, CELL, false);
		int c1 = tintColour(tint, 0f), c3 = tintColour(tint, 1f);

		for (int f = 0; f < FRAMES; f++) {
			double ang = f * Math.PI * 2 / FRAMES;
			double ca = Math.cos(-ang), sa = Math.sin(-ang);

			for (int py = 0; py < CELL; py++) {
				for (int px = 0; px < CELL; px++) {
					double cov = 0, halo = 0;
					for (int sy = 0; sy < 2; sy++) {
						for (int sx = 0; sx < 2; sx++) {
							double nx = ((px + 0.25 + sx * 0.5) / CELL) * 2 - 1;
							double ny = ((py + 0.25 + sy * 0.5) / CELL) * 2 - 1;
							double rx = nx * ca - ny * sa;
							double ry = nx * sa + ny * ca;
							if (inside(style, rx, ry)) cov++;
							else if (inside(style, rx / 1.22, ry / 1.22)) halo++;
						}
					}
					cov /= 4.0; halo /= 4.0;
					if (cov <= 0.001 && halo <= 0.001) { img.setPixelABGR(px + f * CELL, py, 0); continue; }
					int col = Colors.lerp(c1, c3, py / (float) (CELL - 1));
					int core = cov > 0.02 ? col : Colors.lerp(col, 0xFFFFFF, 0.4f);
					int a = (int) Math.min(255, cov * 255 + halo * 95);
					if (a <= 0) { img.setPixelABGR(px + f * CELL, py, 0); continue; }
					img.setPixelABGR(px + f * CELL, py, argb(a, (core >> 16) & 255, (core >> 8) & 255, core & 255));
				}
			}
		}
		return img;
	}

	public static void draw(GuiGraphicsExtractor g, int x, int y, int size, double angle, int style) {
		ensure(style, tintMode);
		int f = (int) Math.round((angle / (Math.PI * 2)) * FRAMES) % FRAMES;
		if (f < 0) f += FRAMES;
		Gfx.blit(g, ID, x, y, f * (float) CELL, 0f, size, size, CELL, CELL, CELL * FRAMES, CELL);
	}

	public static boolean imageAvailable() { loadSource(); return src != null; }

	private static boolean inside(int style, double x, double y) {
		switch (style) {
			case 1: {
				double X = x * 1.32, Y = -y * 1.32 + 0.18;
				double a = X * X + Y * Y - 1.0;
				return a * a * a - X * X * Y * Y * Y <= 0.0;
			}
			case 2: {
				if (x >= -0.66 && x <= -0.26 && y >= -0.74 && y <= 0.74) return true;
				if (x >= -0.66 && x <= 0.50 && y >= 0.34 && y <= 0.74) return true;
				double X = (x - 0.36) * 2.3, Y = (-y - 0.36) * 2.3;
				double a = X * X + Y * Y - 1.0;
				return a * a * a - X * X * Y * Y * Y <= 0.0;
			}
			case 3: return star(x, y, 5, 0.95, 0.42);
			case 4: return hex(x, y, 0.86) || Math.abs(x) + Math.abs(y) <= 0.72;
			case 5: {
				double X = x / 0.98, Y = y / 0.98;
				double q = X * X + Y * Y;
				return q * q - (X * X - Y * Y) * 0.92 <= 0.0;
			}
			case 6: return Math.pow(Math.abs(x), 0.62) + Math.pow(Math.abs(y), 0.62) <= 1.0;
			case 7: {
				if (y >= 0.12 && y <= 0.64 && Math.abs(x) <= 0.80) return true;
				if (y >= -0.66 && y <= 0.12) {
					for (int k = -1; k <= 1; k++) {
						double cx = k * 0.52;
						double h = 0.78 - Math.abs(k) * 0.24;
						double half = 0.28 * (1.0 - (y + 0.66) / (h + 0.66));
						if (half > 0.01 && Math.abs(x - cx) <= half) return true;
					}
				}
				return false;
			}
			case 8: {
				boolean a = ell(x + 0.18, y, 0.88, 0.52);
				boolean b = ell(x - 0.40, y + 0.12, 0.72, 0.36);
				return a && !b;
			}
			default: return star(x, y, 4, 0.95, 0.30);
		}
	}

	private static boolean ell(double x, double y, double rx, double ry) {
		double a = x / rx, b = y / ry;
		return a * a + b * b <= 1.0;
	}

	private static boolean hex(double x, double y, double r) {
		double[] px = new double[6], py = new double[6];
		for (int i = 0; i < 6; i++) {
			double a = Math.PI / 6 + i * Math.PI / 3;
			px[i] = Math.cos(a) * r;
			py[i] = Math.sin(a) * r;
		}
		return poly(x, y, px, py);
	}

	private static boolean star(double x, double y, int points, double outer, double inner) {
		int n = points * 2;
		double[] px = new double[n], py = new double[n];
		for (int i = 0; i < n; i++) {
			double r = (i % 2 == 0) ? outer : inner;
			double a = -Math.PI / 2 + i * Math.PI / points;
			px[i] = Math.cos(a) * r;
			py[i] = Math.sin(a) * r;
		}
		return poly(x, y, px, py);
	}

	private static boolean poly(double x, double y, double[] px, double[] py) {
		boolean in = false;
		for (int i = 0, j = px.length - 1; i < px.length; j = i++) {
			if (((py[i] > y) != (py[j] > y))
					&& (x < (px[j] - px[i]) * (y - py[i]) / (py[j] - py[i]) + px[i])) in = !in;
		}
		return in;
	}
}
EOF

# ------------------------------------------------------------------ MenuFx
cat > src/main/java/com/lovenote/util/MenuFx.java <<'EOF'
package com.lovenote.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;

/** Screen-level menu effects. Draw-only, so it can never swallow clicks. */
public final class MenuFx {
	private MenuFx() {}

	public static boolean enabled = true;
	public static boolean onlyMeteor = true;
	public static boolean rippleOn = true;
	public static int edge = 26;
	public static int maxSize = 54;
	public static int life = 720;
	public static int rippleAlpha = 85;
	public static int width = 2;
	public static boolean vignette = true;
	public static boolean scanlines = false;
	public static int grain = 0;
	public static boolean edgeGlow = true;
	public static int edgeGlowAlpha = 70;

	public static boolean logoInMenu = true;
	public static int logoMenuSize = 24;
	public static String logoMenuCorner = "Top Left";
	public static int logoStyle = 0;
	public static float logoSpin = 0.7f;

	private static final class Ripple { int x, y; long t0; }
	private static final ArrayList<Ripple> ripples = new ArrayList<>();
	private static long lastSpawn;
	private static int lastX = -1, lastY = -1;

	private static float hash(int n) {
		int h = n * 0x9E3779B9;
		h ^= h >>> 16;
		h *= 0x85EBCA6B;
		h ^= h >>> 13;
		h *= 0xC2B2AE35;
		h ^= h >>> 16;
		return (h & 0xFFFFFF) / (float) 0xFFFFFF;
	}

	public static void onScreen(GuiGraphicsExtractor g, int mx, int my, float delta) {
		if (!enabled) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.gui == null) return;
		var screen = mc.gui.screen();
		if (screen == null) return;
		if (onlyMeteor) {
			String n = screen.getClass().getName();
			if (!n.startsWith("meteordevelopment.meteorclient") && !n.startsWith("com.lovenote")) return;
		}

		int w = Gfx.guiW();
		int h = Gfx.guiH();
		long now = System.currentTimeMillis();

		if (vignette) vignette(g, w, h);
		if (scanlines) scanlines(g, w, h);
		if (grain > 0) grain(g, w, h, now);
		if (edgeGlow) edgeGlow(g, w, h, mx, my);
		if (logoInMenu) menuLogo(g, w, h, now);

		if (!rippleOn) return;

		boolean near = mx <= edge || my <= edge || mx >= w - edge || my >= h - edge;
		if (near && now - lastSpawn > 110 && (mx != lastX || my != lastY)) {
			Ripple r = new Ripple();
			r.x = mx; r.y = my; r.t0 = now;
			ripples.add(r);
			if (ripples.size() > 28) ripples.remove(0);
			lastSpawn = now;
		}
		lastX = mx; lastY = my;

		ripples.removeIf(r -> now - r.t0 > life);
		int half = Math.max(1, width / 2);
		int steps = 44;
		for (int k = ripples.size() - 1; k >= 0; k--) {
			Ripple r = ripples.get(k);
			float t = (now - r.t0) / (float) life;
			float rad = 2f + t * maxSize;
			int a = (int) (rippleAlpha * (1f - t) * (1f - t));
			if (a < 3) continue;
			int col = Colors.alpha(Colors.skeet(t), a);
			for (int i = 0; i < steps; i++) {
				double ang = i * Math.PI * 2 / steps;
				int px = (int) (r.x + Math.cos(ang) * rad);
				int py = (int) (r.y + Math.sin(ang) * rad);
				if (px < -4 || py < -4 || px > w + 4 || py > h + 4) continue;
				g.fill(px - half, py - half, px + half, py + half, col);
			}
		}
	}

	private static void menuLogo(GuiGraphicsExtractor g, int w, int h, long now) {
		int s = logoMenuSize;
		int pad = 8;
		int x = pad, y = pad;
		if (logoMenuCorner.contains("Right")) x = w - s - pad;
		if (logoMenuCorner.contains("Bottom")) y = h - s - pad;
		Logos.draw(g, x, y, s, (now / 1000.0) * logoSpin, logoStyle);
	}

	private static void vignette(GuiGraphicsExtractor g, int w, int h) {
		int steps = 26;
		for (int i = 0; i < steps; i++) {
			int a = (int) (52 * (1f - i / (float) steps));
			if (a < 2) continue;
			int c = Colors.alpha(0x000000, a);
			g.fill(0, i, w, i + 1, c);
			g.fill(0, h - i - 1, w, h - i, c);
			g.fill(i, 0, i + 1, h, c);
			g.fill(w - i - 1, 0, w - i, h, c);
		}
	}

	private static void scanlines(GuiGraphicsExtractor g, int w, int h) {
		int c = Colors.alpha(0x000000, 26);
		for (int y = 0; y < h; y += 3) g.fill(0, y, w, y + 1, c);
	}

	private static void grain(GuiGraphicsExtractor g, int w, int h, long now) {
		int frame = (int) (now / 60);
		int count = Math.min(900, grain);
		for (int i = 0; i < count; i++) {
			int x = (int) (hash(i * 3 + 1 + frame) * w);
			int y = (int) (hash(i * 3 + 2 + frame) * h);
			int a = (int) (hash(i + frame * 7) * 42);
			if (a < 4) continue;
			g.fill(x, y, x + 1, y + 1, Colors.alpha(0xFFFFFF, a));
		}
	}

	private static void edgeGlow(GuiGraphicsExtractor g, int w, int h, int mx, int my) {
		int steps = 12;
		float left = 1f - Math.min(1f, mx / (float) (steps * 4));
		float right = 1f - Math.min(1f, (w - mx) / (float) (steps * 4));
		float top = 1f - Math.min(1f, my / (float) (steps * 4));
		float bottom = 1f - Math.min(1f, (h - my) / (float) (steps * 4));
		for (int i = 0; i < steps; i++) {
			float f = 1f - i / (float) steps;
			int al = (int) (edgeGlowAlpha * f * Math.max(0.25f, left));
			int ar = (int) (edgeGlowAlpha * f * Math.max(0.25f, right));
			int at = (int) (edgeGlowAlpha * f * Math.max(0.25f, top));
			int ab = (int) (edgeGlowAlpha * f * Math.max(0.25f, bottom));
			int col = Colors.C3;
			if (al > 2) g.fill(i, 0, i + 1, h, Colors.alpha(col, al));
			if (ar > 2) g.fill(w - i - 1, 0, w - i, h, Colors.alpha(col, ar));
			if (at > 2) g.fill(0, i, w, i + 1, Colors.alpha(col, at));
			if (ab > 2) g.fill(0, h - i - 1, w, h - i, Colors.alpha(col, ab));
		}
	}
}
EOF

# ------------------------------------------------------------------ Interface
cat > src/main/java/com/lovenote/module/impl/Interface.java <<'EOF'
package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Logos;
import com.lovenote.util.Ui;

/** Master style module. Every UI/HUD surface reads from here. */
public class Interface extends Module {
	public static Interface INSTANCE;

	public final ModeSetting accent = add(new ModeSetting("Accent", "Lovenote", Colors.PRESET_NAMES));
	public final ModeSetting theme = add(new ModeSetting("Theme", "Dark", "Dark", "Light"));
	public final BoolSetting animated = add(new BoolSetting("Animated accent", true));

	public final ModeSetting bgTint = add(new ModeSetting("Panel tint", "Dark", "Dark", "Black", "Accent"));
	public final NumberSetting bgOpacity = add(new NumberSetting("Panel opacity", 225, 0, 255, 5));
	public final BoolSetting gradientPanel = add(new BoolSetting("Panel gradient", true));
	public final BoolSetting glass = add(new BoolSetting("Glass sheen", true));
	public final ModeSetting pillStyle = add(new ModeSetting("Corner style", "Pill", "Pill", "Rounded"));
	public final NumberSetting panelRadius = add(new NumberSetting("Corner radius", 7, 0, 18, 1));
	public final NumberSetting widgetScale = add(new NumberSetting("Widget scale", 1.0, 0.6, 1.8, 0.05));

	public final BoolSetting accentBorder = add(new BoolSetting("Accent border", true));
	public final NumberSetting borderWidth = add(new NumberSetting("Border width", 1, 0, 3, 1));
	public final NumberSetting borderAlpha = add(new NumberSetting("Border opacity", 70, 0, 255, 5));
	public final BoolSetting shadowOn = add(new BoolSetting("Drop shadow", true));
	public final NumberSetting shadowSize = add(new NumberSetting("Shadow size", 4, 0, 12, 1));
	public final NumberSetting shadowAlpha = add(new NumberSetting("Shadow opacity", 80, 0, 255, 5));
	public final BoolSetting glowOn = add(new BoolSetting("Accent glow", true));
	public final NumberSetting glowSize = add(new NumberSetting("Glow size", 5, 0, 14, 1));
	public final NumberSetting glowAlpha = add(new NumberSetting("Glow opacity", 45, 0, 200, 5));

	public final BoolSetting textShadow = add(new BoolSetting("Text shadow", true));

	public final BoolSetting menuFx = add(new BoolSetting("Menu effects", true));
	public final BoolSetting menuFxMeteorOnly = add(new BoolSetting("Menu only", true));
	public final BoolSetting rippleOn = add(new BoolSetting("Edge ripple", true));
	public final NumberSetting rippleEdge = add(new NumberSetting("Ripple reach", 26, 4, 120, 2));
	public final NumberSetting rippleSize = add(new NumberSetting("Ripple size", 54, 12, 180, 2));
	public final NumberSetting rippleLife = add(new NumberSetting("Ripple life (ms)", 720, 200, 2500, 20));
	public final NumberSetting rippleAlpha = add(new NumberSetting("Ripple opacity", 85, 10, 255, 5));
	public final NumberSetting rippleWidth = add(new NumberSetting("Ripple width", 2, 1, 6, 1));
	public final BoolSetting menuVignette = add(new BoolSetting("Vignette", true));
	public final BoolSetting menuScanlines = add(new BoolSetting("Scanlines", false));
	public final NumberSetting menuGrain = add(new NumberSetting("Grain", 0, 0, 900, 25));
	public final BoolSetting menuEdgeGlow = add(new BoolSetting("Edge glow", true));
	public final NumberSetting menuEdgeGlowAlpha = add(new NumberSetting("Edge glow opacity", 70, 0, 200, 5));

	public final ModeSetting logoStyle = add(new ModeSetting("Logo", "Image", Logos.STYLES));
	public final NumberSetting logoSize = add(new NumberSetting("Logo size", 16, 6, 96, 1));
	public final NumberSetting logoSpin = add(new NumberSetting("Logo spin", 1.0, 0.0, 6.0, 0.05));
	public final BoolSetting logoInWidgets = add(new BoolSetting("Logo in widgets", true));
	public final ModeSetting logoColour = add(new ModeSetting("Logo colour", "Accent", "Accent", "White", "Rainbow", "Original"));
	public final NumberSetting logoCropTop = add(new NumberSetting("Logo crop top %", 24, 0, 60, 1));
	public final NumberSetting logoCropBottom = add(new NumberSetting("Logo crop bottom %", 0, 0, 60, 1));
	public final NumberSetting logoZoom = add(new NumberSetting("Logo zoom", 1.0, 0.5, 1.8, 0.05));
	public final BoolSetting logoKeyBlack = add(new BoolSetting("Logo key out black", true));
	public final BoolSetting logoInMenu = add(new BoolSetting("Logo in menu", true));
	public final ModeSetting logoMenuCorner = add(new ModeSetting("Logo corner", "Top Left", "Top Left", "Top Right", "Bottom Left", "Bottom Right"));
	public final NumberSetting logoMenuSize = add(new NumberSetting("Menu logo size", 24, 10, 72, 1));
	public final NumberSetting logoMenuSpin = add(new NumberSetting("Menu logo spin", 0.7, 0.0, 4.0, 0.05));

	public final BoolSetting hudFrame = add(new BoolSetting("Widget frame", true));
	public final BoolSetting hudAccentBar = add(new BoolSetting("Widget accent bar", true));
	public final NumberSetting hudPadding = add(new NumberSetting("Widget padding", 5, 1, 14, 1));

	public Interface() {
		super("Interface", Category.THEMES);
		describe("Master style: accent, glass, glow, ripples, logo, HUD frame.");
		alsoIn.add(Category.HUD);
		INSTANCE = this;
		enabled = true;
		Logos.setStyle(logoStyle.index());
		Ui.push(this);
	}

	@Override
	public void onEnable() { Ui.push(this); }

	@Override
	public void onTick() {
		Logos.setStyle(logoStyle.index());
		Ui.push(this);
	}
}
EOF

# ------------------------------------------------------------------ LogoWidget
cat > src/main/java/com/lovenote/module/impl/LogoWidget.java <<'EOF'
package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Gfx;
import com.lovenote.util.Logos;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Spinning client logo next to your name, with optional live info. */
public class LogoWidget extends Module {
	private final ModeSetting style = add(new ModeSetting("Logo", "Image", Logos.STYLES));
	private final NumberSetting size = add(new NumberSetting("Logo size", 16, 6, 64, 1));
	private final NumberSetting spin = add(new NumberSetting("Spin speed", 1.0, 0.0, 6.0, 0.05));
	private final ModeSetting spinMode = add(new ModeSetting("Spin", "Spin", "Spin", "Pulse", "Spin + Pulse", "Static"));
	private final NumberSetting x = add(new NumberSetting("X", 0.012, 0.0, 0.95, 0.002));
	private final NumberSetting y = add(new NumberSetting("Y", 0.012, 0.0, 0.95, 0.002));
	private final BoolSetting frame = add(new BoolSetting("Frame", true));
	private final NumberSetting pad = add(new NumberSetting("Padding", 4, 1, 16, 1));
	private final BoolSetting showName = add(new BoolSetting("Show name", true));
	private final ModeSetting nameText = add(new ModeSetting("Name", "lovenote", "lovenote", "Lovenote", "Custom"));
	private final ModeSetting customName = add(new ModeSetting("Custom", "lovenote", "lovenote", "Custom"));
	private final ModeSetting nameColour = add(new ModeSetting("Name colour", "Gradient", "Gradient", "Accent", "White"));
	private final BoolSetting info = add(new BoolSetting("Show info", true));
	private final BoolSetting showFps = add(new BoolSetting("FPS", true));
	private final BoolSetting showTime = add(new BoolSetting("Time", true));
	private final BoolSetting showServer = add(new BoolSetting("Server", false));
	private final NumberSetting gap = add(new NumberSetting("Gap", 5, 0, 24, 1));

	public LogoWidget() {
		super("LogoWidget", Category.HUD);
		describe("Spinning logo and client name.");
		enabled = true;
	}

	private String label() {
		return nameText.is("Custom") ? customName.get() : nameText.get();
	}

	@Override
	public void onHud(GuiGraphicsExtractor g, float td) {
		if (mc.player == null) return;
		Logos.setStyle(style.index());
		Ui.push(Interface.INSTANCE);

		int px = (int) (x.get() * Gfx.guiW());
		int py = (int) (y.get() * Gfx.guiH());

		float t = System.currentTimeMillis() / 1000f;
		double angle = 0;
		if (spinMode.is("Spin") || spinMode.is("Spin + Pulse")) angle = t * spin.get();
		int sz = size.getI();
		if (spinMode.is("Pulse") || spinMode.is("Spin + Pulse"))
			sz = (int) (sz * (0.9 + 0.1 * Math.sin(t * 2.2)));

		String name = label();
		String infoStr = info.get() ? buildInfo() : "";
		int nameW = showName.get() ? Ui.textW(name) : 0;
		int infoW = infoStr.isEmpty() ? 0 : Ui.textW(infoStr);
		int inner = sz + (nameW > 0 ? gap.getI() + nameW : 0) + (infoW > 0 ? gap.getI() + infoW : 0);

		int p = pad.getI();
		int bw = inner + p * 2, bh = Math.max(sz, 11) + p * 2;
		if (frame.get()) Ui.pill(g, px, py, bw, bh);

		int tx = px + p;
		Logos.draw(g, tx, py + (bh - sz) / 2, sz, angle, style.index());
		tx += sz + gap.getI();

		if (showName.get()) {
			int col = nameColour.is("White") ? 0xFFFFFFFF
					: nameColour.is("Accent") ? Colors.C1
					: Colors.skeet((t * 0.25f) % 1f);
			Ui.text(g, name, tx, py + (bh - 8) / 2, col);
			tx += nameW + gap.getI();
		}
		if (infoW > 0) Ui.text(g, infoStr, tx, py + (bh - 8) / 2, Ui.textDim);
	}

	private String buildInfo() {
		StringBuilder sb = new StringBuilder();
		if (showFps.get()) sb.append(mc.getFps()).append(" fps");
		if (showTime.get()) {
			if (sb.length() > 0) sb.append("  ");
			long secs = (System.currentTimeMillis() / 1000L) % 86400L;
			sb.append(String.format("%02d:%02d", secs / 3600, (secs % 3600) / 60));
		}
		if (showServer.get() && mc.getCurrentServer() != null) {
			if (sb.length() > 0) sb.append("  ");
			sb.append(mc.getCurrentServer().ip);
		}
		return sb.toString();
	}
}
EOF

# ------------------------------------------------------------------ FunnyCrosshair
cat > src/main/java/com/lovenote/module/impl/FunnyCrosshair.java <<'EOF'
package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Gfx;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Fully custom crosshair with deliberately silly shapes. */
public class FunnyCrosshair extends Module {
	public static FunnyCrosshair INSTANCE;

	private static final String[] SHAPES = {
			"Star of David", "Pentagram", "Heart", "Smiley", "Cat", "Skull", "Snowflake",
			"Yin Yang", "Anarchy", "Lightning", "Diamond", "Flower", "Paw", "Crown",
			"Cross", "Chevron", "Arrow", "Dot Ring"
	};

	private final ModeSetting shape = add(new ModeSetting("Shape", "Star of David", SHAPES));
	private final NumberSetting size = add(new NumberSetting("Size", 7, 2, 24, 0.5));
	private final NumberSetting thick = add(new NumberSetting("Thickness", 1, 1, 4, 1));
	private final NumberSetting gap = add(new NumberSetting("Gap", 2, 0, 10, 0.5));
	private final NumberSetting spin = add(new NumberSetting("Spin speed", 0.0, -3.0, 3.0, 0.05));
	private final NumberSetting pulse = add(new NumberSetting("Pulse", 0.0, 0.0, 1.0, 0.05));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 235, 20, 255, 5));
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "White", "Red", "Green"));
	private final BoolSetting dot = add(new BoolSetting("Centre dot", true));
	private final NumberSetting dotSize = add(new NumberSetting("Dot size", 1, 1, 4, 1));
	private final BoolSetting hideVanilla = add(new BoolSetting("Hide vanilla", true));

	private int cachedShape = -1;
	private double[][] segs = new double[0][];
	private double[][] dots = new double[0][];

	public FunnyCrosshair() {
		super("FunnyCrosshair", Category.HUD);
		describe("Custom animated crosshair with silly shapes.");
		INSTANCE = this;
		enabled = true;
	}

	public static boolean hideVanilla() {
		return INSTANCE != null && INSTANCE.enabled && INSTANCE.hideVanilla.get();
	}

	@Override
	public void onHud(GuiGraphicsExtractor g, float td) {
		if (mc.player == null) return;
		if (mc.options != null && mc.options.hideGui) return;

		int idx = shape.index();
		if (idx != cachedShape) { cachedShape = idx; build(idx); }

		float cx = Gfx.guiW() / 2f;
		float cy = Gfx.guiH() / 2f;
		float t = System.currentTimeMillis() / 1000f;
		double rot = t * spin.get();
		double s = size.get() * (1.0 + pulse.get() * 0.35 * Math.sin(t * 4.0));
		int th = thick.getI();
		int a = opacity.getI();

		int col = switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(t * 0.05f);
			case "White" -> 0xFFFFFFFF;
			case "Red" -> 0xFFFF3B3B;
			case "Green" -> 0xFF3BFF6B;
			default -> Colors.skeet((t * 0.2f) % 1f);
		};

		double ca = Math.cos(rot), sa = Math.sin(rot);
		int c = Colors.alpha(col, a);
		for (double[] sg : segs) {
			double x0 = (sg[0] * ca - sg[1] * sa) * s;
			double y0 = (sg[0] * sa + sg[1] * ca) * s;
			double x1 = (sg[2] * ca - sg[3] * sa) * s;
			double y1 = (sg[2] * sa + sg[3] * ca) * s;
			line(g, cx + x0, cy + y0, cx + x1, cy + y1, th, c);
		}
		for (double[] d : dots) {
			double x = (d[0] * ca - d[1] * sa) * s;
			double y = (d[0] * sa + d[1] * ca) * s;
			blob(g, cx + x, cy + y, d[2] * s, c);
		}
		if (dot.get()) blob(g, cx, cy, dotSize.getI(), Colors.alpha(col, a));
	}

	private static void line(GuiGraphicsExtractor g, double x0, double y0, double x1, double y1, int th, int col) {
		double dx = x1 - x0, dy = y1 - y0;
		int n = (int) Math.ceil(Math.hypot(dx, dy));
		int h = th / 2;
		if (n <= 0) { g.fill((int) x0 - h, (int) y0 - h, (int) x0 - h + th, (int) y0 - h + th, col); return; }
		for (int i = 0; i <= n; i++) {
			double t = i / (double) n;
			int px = (int) Math.round(x0 + dx * t);
			int py = (int) Math.round(y0 + dy * t);
			g.fill(px - h, py - h, px - h + th, py - h + th, col);
		}
	}

	private static void blob(GuiGraphicsExtractor g, double x, double y, double r, int col) {
		int ir = (int) Math.max(1, Math.round(r));
		for (int dy = -ir; dy <= ir; dy++) {
			for (int dx = -ir; dx <= ir; dx++) {
				if (dx * dx + dy * dy > ir * ir + ir) continue;
				g.fill((int) x + dx, (int) y + dy, (int) x + dx + 1, (int) y + dy + 1, col);
			}
		}
	}

	private void build(int idx) {
		List<double[]> s = new ArrayList<>();
		List<double[]> d = new ArrayList<>();
		double g0 = gap.get() / 14.0;

		switch (SHAPES[Math.min(idx, SHAPES.length - 1)]) {
			case "Star of David" -> {
				for (int k = 0; k < 2; k++) {
					double[] p = new double[6];
					for (int i = 0; i < 3; i++) {
						double a = -Math.PI / 2 + k * Math.PI / 3 + i * 2 * Math.PI / 3;
						p[i * 2] = Math.cos(a);
						p[i * 2 + 1] = Math.sin(a);
					}
					for (int i = 0; i < 3; i++) {
						int j = (i + 1) % 3;
						s.add(new double[]{p[i * 2], p[i * 2 + 1], p[j * 2], p[j * 2 + 1]});
					}
				}
			}
			case "Pentagram" -> {
				double[] p = new double[10];
				for (int i = 0; i < 5; i++) {
					double a = -Math.PI / 2 + i * 4 * Math.PI / 5;
					p[i * 2] = Math.cos(a);
					p[i * 2 + 1] = Math.sin(a);
				}
				for (int i = 0; i < 5; i++) {
					int j = (i + 1) % 5;
					s.add(new double[]{p[i * 2], p[i * 2 + 1], p[j * 2], p[j * 2 + 1]});
				}
			}
			case "Heart" -> {
				int n = 26;
				double[] p = new double[(n + 1) * 2];
				for (int i = 0; i <= n; i++) {
					double a = i / (double) n * Math.PI * 2;
					p[i * 2] = Math.pow(Math.sin(a), 3);
					p[i * 2 + 1] = -(13 * Math.cos(a) - 5 * Math.cos(2 * a) - 2 * Math.cos(3 * a) - Math.cos(4 * a)) / 16.0;
				}
				for (int i = 0; i < n; i++)
					s.add(new double[]{p[i * 2], p[i * 2 + 1], p[i * 2 + 2], p[i * 2 + 3]});
			}
			case "Smiley" -> {
				circle(s, 0, 0, 1.0, 24);
				d.add(new double[]{-0.36, -0.28, 0.12});
				d.add(new double[]{0.36, -0.28, 0.12});
				int n = 12;
				for (int i = 0; i < n; i++) {
					double a0 = Math.PI * 0.15 + i / (double) n * Math.PI * 0.7;
					double a1 = Math.PI * 0.15 + (i + 1) / (double) n * Math.PI * 0.7;
					s.add(new double[]{Math.cos(a0) * 0.62, Math.sin(a0) * 0.62, Math.cos(a1) * 0.62, Math.sin(a1) * 0.62});
				}
			}
			case "Cat" -> {
				circle(s, 0, 0.1, 0.85, 22);
				s.add(new double[]{-0.72, -0.5, -0.45, -1.0});
				s.add(new double[]{-0.45, -1.0, -0.18, -0.6});
				s.add(new double[]{0.72, -0.5, 0.45, -1.0});
				s.add(new double[]{0.45, -1.0, 0.18, -0.6});
				s.add(new double[]{-1.5, 0.25, -0.6, 0.25});
				s.add(new double[]{-1.5, 0.55, -0.6, 0.45});
				s.add(new double[]{1.5, 0.25, 0.6, 0.25});
				s.add(new double[]{1.5, 0.55, 0.6, 0.45});
				d.add(new double[]{-0.3, -0.05, 0.1});
				d.add(new double[]{0.3, -0.05, 0.1});
			}
			case "Skull" -> {
				circle(s, 0, -0.15, 0.85, 20);
				s.add(new double[]{-0.55, 0.6, 0.55, 0.6});
				s.add(new double[]{-0.4, 0.6, -0.4, 0.95});
				s.add(new double[]{0.0, 0.6, 0.0, 0.95});
				s.add(new double[]{0.4, 0.6, 0.4, 0.95});
				s.add(new double[]{-0.55, 0.95, 0.55, 0.95});
				d.add(new double[]{-0.32, -0.2, 0.18});
				d.add(new double[]{0.32, -0.2, 0.18});
			}
			case "Snowflake" -> {
				for (int k = 0; k < 6; k++) {
					double a = k * Math.PI / 3;
					double ex = Math.cos(a), ey = Math.sin(a);
					s.add(new double[]{0, 0, ex, ey});
					s.add(new double[]{ex * 0.62, ey * 0.62, ex * 0.62 + Math.cos(a + 0.9) * 0.3, ey * 0.62 + Math.sin(a + 0.9) * 0.3});
					s.add(new double[]{ex * 0.62, ey * 0.62, ex * 0.62 + Math.cos(a - 0.9) * 0.3, ey * 0.62 + Math.sin(a - 0.9) * 0.3});
				}
			}
			case "Yin Yang" -> {
				circle(s, 0, 0, 1.0, 26);
				int n = 16;
				for (int i = 0; i < n; i++) {
					double a0 = -Math.PI / 2 + i / (double) n * Math.PI;
					double a1 = -Math.PI / 2 + (i + 1) / (double) n * Math.PI;
					s.add(new double[]{Math.cos(a0) * 0.5, Math.sin(a0) * 0.5, Math.cos(a1) * 0.5, Math.sin(a1) * 0.5});
					s.add(new double[]{-Math.cos(a0) * 0.5, -Math.sin(a0) * 0.5, -Math.cos(a1) * 0.5, -Math.sin(a1) * 0.5});
				}
				d.add(new double[]{0, 0.5, 0.14});
				d.add(new double[]{0, -0.5, 0.14});
			}
			case "Anarchy" -> {
				circle(s, 0, 0, 1.0, 26);
				s.add(new double[]{-0.45, 0.5, 0.0, -0.6});
				s.add(new double[]{0.45, 0.5, 0.0, -0.6});
				s.add(new double[]{-0.25, 0.1, 0.25, 0.1});
			}
			case "Lightning" -> {
				s.add(new double[]{0.3, -1.0, -0.3, 0.05});
				s.add(new double[]{-0.3, 0.05, 0.15, 0.05});
				s.add(new double[]{0.15, 0.05, -0.2, 1.0});
			}
			case "Diamond" -> {
				s.add(new double[]{0, -1.0, 0.85, 0});
				s.add(new double[]{0.85, 0, 0, 1.0});
				s.add(new double[]{0, 1.0, -0.85, 0});
				s.add(new double[]{-0.85, 0, 0, -1.0});
				s.add(new double[]{-0.55, -0.5, 0.55, 0.5});
				s.add(new double[]{0.55, -0.5, -0.55, 0.5});
			}
			case "Flower" -> {
				for (int k = 0; k < 6; k++) {
					double a = k * Math.PI / 3;
					circle(s, Math.cos(a) * 0.6, Math.sin(a) * 0.6, 0.42, 12);
				}
				d.add(new double[]{0, 0, 0.22});
			}
			case "Paw" -> {
				circle(s, 0, 0.35, 0.55, 16);
				d.add(new double[]{-0.62, -0.35, 0.24});
				d.add(new double[]{-0.22, -0.6, 0.24});
				d.add(new double[]{0.22, -0.6, 0.24});
				d.add(new double[]{0.62, -0.35, 0.24});
			}
			case "Crown" -> {
				s.add(new double[]{-0.9, 0.55, -0.9, -0.5});
				s.add(new double[]{-0.9, -0.5, -0.45, 0.05});
				s.add(new double[]{-0.45, 0.05, 0.0, -0.65});
				s.add(new double[]{0.0, -0.65, 0.45, 0.05});
				s.add(new double[]{0.45, 0.05, 0.9, -0.5});
				s.add(new double[]{0.9, -0.5, 0.9, 0.55});
				s.add(new double[]{-0.9, 0.55, 0.9, 0.55});
			}
			case "Cross" -> {
				s.add(new double[]{0, -1.0, 0, 1.0});
				s.add(new double[]{-1.0, 0, 1.0, 0});
			}
			case "Chevron" -> {
				s.add(new double[]{-0.7, 0.6, 0.0, -0.6});
				s.add(new double[]{0.7, 0.6, 0.0, -0.6});
			}
			case "Arrow" -> {
				s.add(new double[]{0, 1.0, 0, -1.0});
				s.add(new double[]{0, -1.0, -0.5, -0.4});
				s.add(new double[]{0, -1.0, 0.5, -0.4});
			}
			case "Dot Ring" -> {
				circle(s, 0, 0, 1.0, 20);
				d.add(new double[]{0, 0, 0.16});
			}
			default -> {
				s.add(new double[]{0, -1.0, 0, 1.0});
				s.add(new double[]{-1.0, 0, 1.0, 0});
			}
		}

		List<double[]> out = new ArrayList<>();
		for (double[] sg : s) {
			double len = Math.hypot(sg[2] - sg[0], sg[3] - sg[1]);
			if (len < 1e-6) continue;
			if (g0 > 0.001 && Math.hypot(sg[0], sg[1]) < 1e-6) {
				double ux = (sg[2] - sg[0]) / len, uy = (sg[3] - sg[1]) / len;
				out.add(new double[]{ux * g0, uy * g0, sg[2], sg[3]});
			} else if (g0 > 0.001 && Math.hypot(sg[2], sg[3]) < 1e-6) {
				double ux = (sg[0] - sg[2]) / len, uy = (sg[1] - sg[3]) / len;
				out.add(new double[]{sg[0], sg[1], ux * g0, uy * g0});
			} else out.add(sg);
		}

		segs = out.toArray(new double[0][]);
		dots = d.toArray(new double[0][]);
	}

	private static void circle(List<double[]> s, double cx, double cy, double r, int n) {
		for (int i = 0; i < n; i++) {
			double a0 = i / (double) n * Math.PI * 2;
			double a1 = (i + 1) / (double) n * Math.PI * 2;
			s.add(new double[]{
					cx + Math.cos(a0) * r, cy + Math.sin(a0) * r,
					cx + Math.cos(a1) * r, cy + Math.sin(a1) * r});
		}
	}
}
EOF

echo "== writing visual pack =="

# ------------------------------------------------------------------ Vfx
cat > src/main/java/com/lovenote/util/Vfx.java <<'EOF'
package com.lovenote.util;

import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;

/** Shared math + vertex emitters for the visual pack. Absolute world space. */
public final class Vfx {
	private Vfx() {}

	public static float hash(int n) {
		int h = n * 0x9E3779B9;
		h ^= h >>> 16;
		h *= 0x85EBCA6B;
		h ^= h >>> 13;
		h *= 0xC2B2AE35;
		h ^= h >>> 16;
		return (h & 0xFFFFFF) / (float) 0xFFFFFF;
	}

	public static float hash(int a, int b) { return hash((a * 73856093) ^ (b * 19349663)); }

	public static float fade(float d, float range) {
		float t = 1f - d / Math.max(0.0001f, range);
		if (t <= 0f) return 0f;
		return t * t;
	}

	public static final class P {
		public final double x, y, z;
		public P(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
	}

	public static P rotY(P p, double a) {
		double c = Math.cos(a), s = Math.sin(a);
		return new P(p.x * c - p.z * s, p.y, p.x * s + p.z * c);
	}

	public static P rotX(P p, double a) {
		double c = Math.cos(a), s = Math.sin(a);
		return new P(p.x, p.y * c - p.z * s, p.y * s + p.z * c);
	}

	public static P rotZ(P p, double a) {
		double c = Math.cos(a), s = Math.sin(a);
		return new P(p.x * c - p.y * s, p.x * s + p.y * c, p.z);
	}

	public static P rot(P p, double yaw, double pitch, double roll) {
		return rotZ(rotX(rotY(p, yaw), pitch), roll);
	}

	public static void tube(MeshBuilder b, Matrix4f m,
							double ax, double ay, double az,
							double bx, double by, double bz,
							float th, int ca, int cb,
							double camX, double camY, double camZ) {
		double dx = bx - ax, dy = by - ay, dz = bz - az;
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1e-6) return;
		dx /= len; dy /= len; dz /= len;

		double mx = (ax + bx) * 0.5 - camX;
		double my = (ay + by) * 0.5 - camY;
		double mz = (az + bz) * 0.5 - camZ;

		double sx = dy * mz - dz * my;
		double sy = dz * mx - dx * mz;
		double sz = dx * my - dy * mx;
		double sl = Math.sqrt(sx * sx + sy * sy + sz * sz);
		if (sl < 1e-6) { sx = 1; sy = 0; sz = 0; sl = 1; }
		sx = sx / sl * th; sy = sy / sl * th; sz = sz / sl * th;

		Fx.vc(b, m, (float) (ax - sx), (float) (ay - sy), (float) (az - sz), ca);
		Fx.vc(b, m, (float) (ax + sx), (float) (ay + sy), (float) (az + sz), ca);
		Fx.vc(b, m, (float) (bx + sx), (float) (by + sy), (float) (bz + sz), cb);
		Fx.vc(b, m, (float) (bx - sx), (float) (by - sy), (float) (bz - sz), cb);
	}

	public static void ribbon(MeshBuilder b, Matrix4f m,
							  double ax, double ay, double az,
							  double bx, double by, double bz,
							  float th, int ca, int cb,
							  double camX, double camY, double camZ) {
		double dx = bx - ax, dy = by - ay, dz = bz - az;
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1e-6) return;
		dx /= len; dy /= len; dz /= len;

		double mx = (ax + bx) * 0.5 - camX;
		double my = (ay + by) * 0.5 - camY;
		double mz = (az + bz) * 0.5 - camZ;

		double sx = dy * mz - dz * my;
		double sy = dz * mx - dx * mz;
		double sz = dx * my - dy * mx;
		double sl = Math.sqrt(sx * sx + sy * sy + sz * sz);
		if (sl < 1e-6) { sx = 1; sy = 0; sz = 0; sl = 1; }
		sx = sx / sl * th; sy = sy / sl * th; sz = sz / sl * th;

		Fx.vt(b, m, (float) (ax - sx), (float) (ay - sy), (float) (az - sz), 0f, 1f, ca);
		Fx.vt(b, m, (float) (ax + sx), (float) (ay + sy), (float) (az + sz), 1f, 1f, ca);
		Fx.vt(b, m, (float) (bx + sx), (float) (by + sy), (float) (bz + sz), 1f, 0f, cb);
		Fx.vt(b, m, (float) (bx - sx), (float) (by - sy), (float) (bz - sz), 0f, 0f, cb);
	}

	public static void floorQuad(MeshBuilder b, Matrix4f m,
								 double x0, double z0, double x1, double z1, double y,
								 int c0, int c1, int c2, int c3) {
		Fx.vc(b, m, (float) x0, (float) y, (float) z0, c0);
		Fx.vc(b, m, (float) x1, (float) y, (float) z0, c1);
		Fx.vc(b, m, (float) x1, (float) y, (float) z1, c2);
		Fx.vc(b, m, (float) x0, (float) y, (float) z1, c3);
	}

	public static void ringFlat(MeshBuilder b, Matrix4f m,
								double cx, double cy, double cz,
								double r0, double r1, int seg, int cin, int cout) {
		for (int s = 0; s < seg; s++) {
			double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
			double c0 = Math.cos(a0), s0 = Math.sin(a0);
			double c1 = Math.cos(a1), s1 = Math.sin(a1);
			Fx.vc(b, m, (float) (cx + c0 * r0), (float) cy, (float) (cz + s0 * r0), cin);
			Fx.vc(b, m, (float) (cx + c1 * r0), (float) cy, (float) (cz + s1 * r0), cin);
			Fx.vc(b, m, (float) (cx + c1 * r1), (float) cy, (float) (cz + s1 * r1), cout);
			Fx.vc(b, m, (float) (cx + c0 * r1), (float) cy, (float) (cz + s0 * r1), cout);
		}
	}

	public static void cube(MeshBuilder b, Matrix4f m, P c, double half,
							double yaw, double pitch, double roll, float th, int col,
							double camX, double camY, double camZ) {
		P[] v = new P[8];
		int i = 0;
		for (int xi = -1; xi <= 1; xi += 2)
			for (int yi = -1; yi <= 1; yi += 2)
				for (int zi = -1; zi <= 1; zi += 2) {
					P p = rot(new P(xi * half, yi * half, zi * half), yaw, pitch, roll);
					v[i++] = new P(c.x + p.x, c.y + p.y, c.z + p.z);
				}
		int[][] e = {
				{0, 1}, {2, 3}, {4, 5}, {6, 7},
				{0, 2}, {1, 3}, {4, 6}, {5, 7},
				{0, 4}, {1, 5}, {2, 6}, {3, 7}
		};
		for (int[] ed : e) {
			P a = v[ed[0]], d = v[ed[1]];
			tube(b, m, a.x, a.y, a.z, d.x, d.y, d.z, th, col, col, camX, camY, camZ);
		}
	}

	public static void crystal(MeshBuilder b, Matrix4f m, P c, double r, double h,
							   double yaw, double pitch, double roll, float th,
							   int top, int bottom,
							   double camX, double camY, double camZ) {
		P t = rot(new P(0, h, 0), yaw, pitch, roll);
		P bt = rot(new P(0, -h * 0.75, 0), yaw, pitch, roll);
		double tx = c.x + t.x, ty = c.y + t.y, tz = c.z + t.z;
		double bx = c.x + bt.x, by = c.y + bt.y, bz = c.z + bt.z;

		P[] ring = new P[4];
		for (int s = 0; s < 4; s++) {
			double a = s * Math.PI * 0.5 + Math.PI * 0.25;
			P p = rot(new P(Math.cos(a) * r, 0, Math.sin(a) * r), yaw, pitch, roll);
			ring[s] = new P(c.x + p.x, c.y + p.y, c.z + p.z);
		}
		for (int s = 0; s < 4; s++) {
			P p = ring[s], q = ring[(s + 1) % 4];
			tube(b, m, tx, ty, tz, p.x, p.y, p.z, th, top, top, camX, camY, camZ);
			tube(b, m, p.x, p.y, p.z, q.x, q.y, q.z, th, top, bottom, camX, camY, camZ);
			tube(b, m, bx, by, bz, p.x, p.y, p.z, th, bottom, bottom, camX, camY, camZ);
		}
	}
}
EOF

# ------------------------------------------------------------------ CubeField
cat > src/main/java/com/lovenote/module/impl/CubeField.java <<'EOF'
package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Tex;
import com.lovenote.util.Vfx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;

/** Floating additive wireframe cubes tumbling around you. */
public class CubeField extends Module {
	private final ModeSetting mode = add(new ModeSetting("Mode", "Orbit", "Orbit", "Rise", "Helix", "Halo", "Swarm"));
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "White"));
	private final NumberSetting count = add(new NumberSetting("Count", 26, 1, 120, 1));
	private final NumberSetting radius = add(new NumberSetting("Radius", 6.5, 1.0, 24.0, 0.5));
	private final NumberSetting height = add(new NumberSetting("Height", 5.0, 1.0, 20.0, 0.5));
	private final NumberSetting size = add(new NumberSetting("Size", 0.42, 0.06, 1.6, 0.02));
	private final NumberSetting spin = add(new NumberSetting("Spin", 1.0, 0.0, 4.0, 0.05));
	private final NumberSetting speed = add(new NumberSetting("Speed", 0.35, 0.0, 2.0, 0.05));
	private final NumberSetting width = add(new NumberSetting("Line width", 0.035, 0.008, 0.15, 0.004));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 190, 20, 255, 5));
	private final BoolSetting cores = add(new BoolSetting("Glow cores", true));
	private final NumberSetting coreSize = add(new NumberSetting("Core size", 0.35, 0.05, 2.0, 0.05));

	private double[] px = new double[0], py = new double[0], pz = new double[0];

	public CubeField() {
		super("CubeField", Category.VISUALS);
		describe("Tumbling additive wireframe cubes drifting around you.");
	}

	private int tint(int i, float t) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(i * 0.03f + t * 0.03f);
			case "Ice" -> Colors.lerp(0xFF9BE7FF, 0xFF7C6CFF, Vfx.hash(i * 5));
			case "White" -> 0xFFFFFFFF;
			default -> Colors.wave(i * 0.07f + t * 0.04f);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null) return;
		Matrix4f mat = stack.last().pose();
		var cam = ctx.camera().position();
		double cx = mc.player.getX(), cy = mc.player.getY(), cz = mc.player.getZ();

		int n = count.getI();
		if (px.length != n) { px = new double[n]; py = new double[n]; pz = new double[n]; }

		float t = System.currentTimeMillis() / 1000f;
		float rad = radius.getF();
		float hh = height.getF();
		float half = size.getF();
		float th = width.getF();
		int op = opacity.getI();
		float sp = speed.getF();
		float spn = spin.getF();
		String md = mode.get();

		MeshBuilder b = Fx.colorQuads();
		for (int i = 0; i < n; i++) {
			float h1 = Vfx.hash(i * 3 + 1), h2 = Vfx.hash(i * 3 + 2), h3 = Vfx.hash(i * 3 + 3);
			float h4 = Vfx.hash(i * 7), h5 = Vfx.hash(i * 11);
			double ang = h4 * 6.2832 + t * sp * (0.35 + h5 * 0.9);
			double x = cx, y = cy, z = cz;

			switch (md) {
				case "Rise" -> {
					x = cx + (h1 * 2 - 1) * rad;
					z = cz + (h3 * 2 - 1) * rad;
					y = cy + ((h2 + t * sp * 0.3) % 1.0) * hh * 2 - hh * 0.5;
				}
				case "Helix" -> {
					double r = rad * (0.3 + 0.7 * ((i % 9) / 9.0));
					x = cx + Math.cos(ang * 1.7) * r;
					z = cz + Math.sin(ang * 1.7) * r;
					y = cy + ((i / (double) Math.max(1, n) + t * sp * 0.4) % 1.0) * hh * 1.6;
				}
				case "Halo" -> {
					double r = rad * 0.65;
					x = cx + Math.cos(ang) * r;
					z = cz + Math.sin(ang) * r;
					y = cy + 2.4 + Math.sin(ang * 2 + t) * 0.4;
				}
				case "Swarm" -> {
					double r = rad * (0.15 + 0.85 * h2);
					x = cx + Math.cos(ang) * r + Math.sin(t * 1.3 + h4 * 6) * 0.8;
					z = cz + Math.sin(ang) * r + Math.cos(t * 1.1 + h4 * 6) * 0.8;
					y = cy + 0.4 + h2 * hh * 0.8 + Math.sin(t * 0.9 + h4 * 6) * 0.7;
				}
				default -> {
					double r = rad * (0.35 + 0.65 * h2);
					x = cx + Math.cos(ang) * r;
					z = cz + Math.sin(ang) * r;
					y = cy + 0.6 + h2 * hh + Math.sin(t * 0.8 + h4 * 6) * 0.6;
				}
			}

			px[i] = x; py[i] = y; pz[i] = z;
			Vfx.cube(b, mat, new Vfx.P(x, y, z), half * (0.7 + 0.6 * h5),
					ang + t * spn, t * spn * 0.7 + h4 * 3, t * spn * 0.45,
					th, Colors.alpha(tint(i, t), op), cam.x, cam.y, cam.z);
		}
		Fx.draw();

		if (!cores.get()) return;
		Fx.cameraBasis(ctx.camera());
		Tex.ensure();
		Fx.texture(Tex.GLOW);
		MeshBuilder g = Fx.texQuads();
		float cs = coreSize.getF();
		for (int i = 0; i < n; i++) {
			int a = (int) (op * 0.85f * (0.55f + 0.45f * Vfx.hash(i * 13)));
			if (a < 4) continue;
			Fx.billboard(g, mat, (float) px[i], (float) py[i], (float) pz[i], cs,
					Colors.alpha(tint(i, t), a));
		}
		Fx.draw();
	}
}
EOF

# ------------------------------------------------------------------ CrystalHalo
cat > src/main/java/com/lovenote/module/impl/CrystalHalo.java <<'EOF'
package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Tex;
import com.lovenote.util.Vfx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

/** A spinning halo of crystal shards around you, with a shatter burst on hit. */
public class CrystalHalo extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Blood", "White"));
	private final NumberSetting shards = add(new NumberSetting("Shards", 14, 3, 48, 1));
	private final NumberSetting radius = add(new NumberSetting("Radius", 1.9, 0.6, 6.0, 0.1));
	private final NumberSetting size = add(new NumberSetting("Size", 0.2, 0.04, 0.8, 0.01));
	private final NumberSetting len = add(new NumberSetting("Shard length", 0.75, 0.2, 2.5, 0.05));
	private final NumberSetting spin = add(new NumberSetting("Spin", 1.4, 0.0, 5.0, 0.05));
	private final NumberSetting tilt = add(new NumberSetting("Tilt", 0.35, -1.5, 1.5, 0.05));
	private final NumberSetting width = add(new NumberSetting("Line width", 0.03, 0.008, 0.12, 0.004));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 210, 20, 255, 5));
	private final BoolSetting burst = add(new BoolSetting("Burst on hit", true));
	private final NumberSetting burstTime = add(new NumberSetting("Burst ms", 900, 200, 3000, 50));

	private long burstAt;
	private double bx, by, bz;

	public CrystalHalo() {
		super("CrystalHalo", Category.VISUALS);
		describe("Spinning crystal shards orbiting you, shattering outward on hit.");
	}

	@Override
	public void onAttack(Entity target) {
		if (!burst.get() || target == null) return;
		burstAt = System.currentTimeMillis();
		bx = target.getX();
		by = target.getY() + target.getBbHeight() * 0.5;
		bz = target.getZ();
	}

	private int tint(float o) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(o);
			case "Ice" -> Colors.lerp(0xFFB7F0FF, 0xFF6E7BFF, o);
			case "Blood" -> Colors.lerp(0xFFFF3B3B, 0xFF7A0B0B, o);
			case "White" -> 0xFFFFFFFF;
			default -> Colors.skeet(o);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null) return;
		Matrix4f mat = stack.last().pose();
		var cam = ctx.camera().position();
		double cx = mc.player.getX(), cy = mc.player.getY() + 1.1, cz = mc.player.getZ();

		float t = System.currentTimeMillis() / 1000f;
		float rad = radius.getF();
		float r = size.getF();
		float h = len.getF();
		float th = width.getF();
		float tl = tilt.getF();
		int op = opacity.getI();
		int n = shards.getI();
		double rot = t * spin.getF();

		MeshBuilder b = Fx.colorQuads();

		for (int i = 0; i < n; i++) {
			double a = i / (double) n * Math.PI * 2 + rot;
			double rr = rad * (0.92 + 0.08 * Math.sin(t * 2 + i));
			double x = cx + Math.cos(a) * rr;
			double z = cz + Math.sin(a) * rr;
			double y = cy + Math.sin(a * 2 + t) * 0.25 + tl;
			int c = Colors.alpha(tint(i / (float) n), op);
			Vfx.crystal(b, mat, new Vfx.P(x, y, z), r, h * (0.85 + 0.3 * Math.sin(t * 3 + i)),
					a, tl * 0.6, a * 0.5, th, c,
					Colors.alpha(tint(i / (float) n), op / 2), cam.x, cam.y, cam.z);
		}

		int inner = Math.max(3, n / 2);
		for (int i = 0; i < inner; i++) {
			double a = -i / (double) inner * Math.PI * 2 - rot * 1.6;
			double rr = rad * 0.45;
			double x = cx + Math.cos(a) * rr;
			double z = cz + Math.sin(a) * rr;
			double y = cy - 0.5 + Math.sin(a + t) * 0.2;
			int c = Colors.alpha(tint(0.6f), (int) (op * 0.7f));
			Vfx.crystal(b, mat, new Vfx.P(x, y, z), r * 0.7, h * 0.55, a, -tl, -a, th * 0.8,
					c, c, cam.x, cam.y, cam.z);
		}

		long now = System.currentTimeMillis();
		float age = (now - burstAt) / (float) burstTime.getI();
		if (burstAt > 0 && age < 1f) {
			int bn = 18;
			float e = (float) Math.pow(age, 0.6);
			for (int i = 0; i < bn; i++) {
				double a = i / (double) bn * Math.PI * 2 + i * 0.3;
				double rr = 0.3 + e * 2.6;
				double x = bx + Math.cos(a) * rr;
				double z = bz + Math.sin(a) * rr;
				double y = by + Math.sin(i * 1.7) * e * 1.4;
				int c = Colors.alpha(tint(i / (float) bn), (int) (255 * (1f - age)));
				Vfx.crystal(b, mat, new Vfx.P(x, y, z), r * 0.6, h * 0.5, a + e * 4, a, a * 2, th,
						c, c, cam.x, cam.y, cam.z);
			}
		}
		Fx.draw();

		Fx.cameraBasis(ctx.camera());
		Tex.ensure();
		Fx.texture(Tex.GLOW);
		MeshBuilder g = Fx.texQuads();
		Fx.billboard(g, mat, (float) cx, (float) cy, (float) cz, 1.1f,
				Colors.alpha(tint(0.2f), (int) (op * 0.9f)));
		if (burstAt > 0 && age < 1f) {
			Fx.billboard(g, mat, (float) bx, (float) by, (float) bz, 1.0f + age * 4f,
					Colors.alpha(tint(0.5f), (int) (200 * (1f - age))));
		}
		Fx.draw();
	}
}
EOF

# ------------------------------------------------------------------ AuroraVeil
cat > src/main/java/com/lovenote/module/impl/AuroraVeil.java <<'EOF'
package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Tex;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;

/** Wavy aurora curtains wrapping around you at flying height. */
public class AuroraVeil extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Toxic", "Solar"));
	private final NumberSetting bands = add(new NumberSetting("Bands", 3, 1, 8, 1));
	private final NumberSetting dist = add(new NumberSetting("Distance", 24, 6, 70, 1));
	private final NumberSetting low = add(new NumberSetting("Bottom", 6, -8, 40, 1));
	private final NumberSetting high = add(new NumberSetting("Height", 22, 4, 90, 1));
	private final NumberSetting amp = add(new NumberSetting("Wave", 2.4, 0.0, 12.0, 0.2));
	private final NumberSetting speed = add(new NumberSetting("Speed", 0.5, 0.0, 3.0, 0.05));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 130, 10, 255, 5));
	private final NumberSetting segments = add(new NumberSetting("Segments", 64, 12, 128, 4));

	public AuroraVeil() {
		super("AuroraVeil", Category.VISUALS);
		describe("Undulating aurora curtains wrapping around you.");
	}

	private int tint(float o) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(o);
			case "Ice" -> Colors.lerp(0xFF8AF5D0, 0xFF5FA8FF, o);
			case "Toxic" -> Colors.lerp(0xFF7CFC00, 0xFF00FF9C, o);
			case "Solar" -> Colors.lerp(0xFFFF8C00, 0xFFFF2E88, o);
			default -> Colors.skeet(o);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null) return;
		Matrix4f mat = stack.last().pose();
		double cx = mc.player.getX(), cz = mc.player.getZ();
		double base = mc.player.getY();

		float t = System.currentTimeMillis() / 1000f * speed.getF();
		float R = dist.getF();
		int seg = segments.getI();
		int nb = bands.getI();
		float y0 = (float) base + low.getF();
		float y1 = y0 + high.getF() * 0.5f;
		float y2 = y0 + high.getF();
		float wa = amp.getF();
		int op = opacity.getI();

		Fx.texture(Tex.SOFT);
		MeshBuilder b = Fx.texQuads();

		for (int band = 0; band < nb; band++) {
			float ph = band * 2.1f;
			int col = tint(band / (float) Math.max(1, nb));
			int peak = (int) (op * (1f - band / (float) (nb + 1)));
			if (peak < 4) continue;

			for (int s = 0; s < seg; s++) {
				double a0 = s / (double) seg * Math.PI * 2;
				double a1 = (s + 1) / (double) seg * Math.PI * 2;
				double rr = R + Math.sin(a0 * 3 + t + ph) * wa;

				float x0 = (float) (cx + Math.cos(a0) * rr);
				float z0 = (float) (cz + Math.sin(a0) * rr);
				float x1 = (float) (cx + Math.cos(a1) * rr);
				float z1 = (float) (cz + Math.sin(a1) * rr);

				float u0 = s / (float) seg;
				float u1 = (s + 1) / (float) seg;
				float yo = (float) (Math.sin(a0 * 4 + t * 1.3 + ph) * wa * 1.6);

				float fy0 = y0 + yo, fy1 = y1 + yo * 0.6f, fy2 = y2 + yo * 0.2f;

				Fx.vt(b, mat, x0, fy0, z0, u0, 1f, Colors.alpha(col, 0));
				Fx.vt(b, mat, x1, fy0, z1, u1, 1f, Colors.alpha(col, 0));
				Fx.vt(b, mat, x1, fy1, z1, u1, 0.5f, Colors.alpha(col, peak));
				Fx.vt(b, mat, x0, fy1, z0, u0, 0.5f, Colors.alpha(col, peak));

				Fx.vt(b, mat, x0, fy1, z0, u0, 0.5f, Colors.alpha(col, peak));
				Fx.vt(b, mat, x1, fy1, z1, u1, 0.5f, Colors.alpha(col, peak));
				Fx.vt(b, mat, x1, fy2, z1, u1, 0f, Colors.alpha(col, 0));
				Fx.vt(b, mat, x0, fy2, z0, u0, 0f, Colors.alpha(col, 0));
			}
		}
		Fx.draw();
	}
}
EOF

# ------------------------------------------------------------------ NeonGrid
cat > src/main/java/com/lovenote/module/impl/NeonGrid.java <<'EOF'
package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Vfx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;

/** A neon floor grid locked to the world, sweeping outward with a scan pulse. */
public class NeonGrid extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Cyber", "Matrix", "White"));
	private final NumberSetting cell = add(new NumberSetting("Cell", 2.0, 0.5, 8.0, 0.5));
	private final NumberSetting extent = add(new NumberSetting("Extent", 14, 3, 40, 1));
	private final NumberSetting lineWidth = add(new NumberSetting("Line width", 0.045, 0.01, 0.2, 0.005));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 120, 10, 255, 5));
	private final NumberSetting scanSpeed = add(new NumberSetting("Scan speed", 7.0, 0.0, 30.0, 0.5));
	private final NumberSetting scanBoost = add(new NumberSetting("Scan boost", 2.4, 1.0, 6.0, 0.1));
	private final BoolSetting scan = add(new BoolSetting("Scan pulse", true));
	private final BoolSetting pulseRing = add(new BoolSetting("Pulse ring", true));
	private final NumberSetting yOffset = add(new NumberSetting("Y offset", 0.05, 0.0, 0.5, 0.01));

	public NeonGrid() {
		super("NeonGrid", Category.VISUALS);
		describe("Neon floor grid with a radar scan sweeping outward from you.");
	}

	private int tint(float o) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(o);
			case "Cyber" -> Colors.lerp(0xFFFF2E88, 0xFF00E5FF, o);
			case "Matrix" -> Colors.lerp(0xFF00FF41, 0xFF004F11, o);
			case "White" -> 0xFFFFFFFF;
			default -> Colors.skeet(o);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null) return;
		Matrix4f mat = stack.last().pose();
		double cx = mc.player.getX(), cy = mc.player.getY(), cz = mc.player.getZ();

		float t = System.currentTimeMillis() / 1000f;
		float ext = extent.getF();
		float cs = cell.getF();
		float lw = lineWidth.getF() * 0.5f;
		int op = opacity.getI();
		float boost = scanBoost.getF();
		float scanR = scan.get() && scanSpeed.getF() > 0.01f
				? (t * scanSpeed.getF()) % Math.max(1f, ext) : -1f;

		double y = Math.floor(cy) + yOffset.getF();
		double gx = Math.floor(cx / cs) * cs;
		double gz = Math.floor(cz / cs) * cs;
		int steps = (int) Math.ceil(ext / cs);

		MeshBuilder b = Fx.colorQuads();

		for (int i = -steps; i <= steps; i++) {
			double lx = gx + i * cs;
			for (int j = -steps; j < steps; j++) {
				double z0 = gz + j * cs, z1 = z0 + cs;
				double d = Math.hypot(lx - cx, (z0 + z1) * 0.5 - cz);
				if (d > ext) continue;
				int a = (int) (op * Vfx.fade((float) d, ext));
				if (scanR > 0f && Math.abs(d - scanR) < 1.0) a = (int) Math.min(255, a * boost);
				if (a < 4) continue;
				int c = Colors.alpha(tint(d / ext), a);
				Vfx.floorQuad(b, mat, lx - lw, z0, lx + lw, z1, y, c, c, c, c);
			}
		}
		for (int i = -steps; i <= steps; i++) {
			double lz = gz + i * cs;
			for (int j = -steps; j < steps; j++) {
				double x0 = gx + j * cs, x1 = x0 + cs;
				double d = Math.hypot((x0 + x1) * 0.5 - cx, lz - cz);
				if (d > ext) continue;
				int a = (int) (op * Vfx.fade((float) d, ext));
				if (scanR > 0f && Math.abs(d - scanR) < 1.0) a = (int) Math.min(255, a * boost);
				if (a < 4) continue;
				int c = Colors.alpha(tint(d / ext), a);
				Vfx.floorQuad(b, mat, x0, lz - lw, x1, lz + lw, y, c, c, c, c);
			}
		}

		if (pulseRing.get() && scanR > 0f) {
			float f = 1f - scanR / Math.max(1f, ext);
			Vfx.ringFlat(b, mat, cx, y, cz, scanR - 0.35, scanR + 0.35, 96,
					Colors.alpha(tint(1f - f), (int) (200 * f)), Colors.alpha(tint(1f - f), 0));
			Vfx.ringFlat(b, mat, cx, y, cz, 0.0, 0.5, 48,
					Colors.alpha(tint(0f), (int) (op * 0.7f)), Colors.alpha(tint(0f), 0));
		}
		Fx.draw();
	}
}
EOF

# ------------------------------------------------------------------ RibbonTrail
cat > src/main/java/com/lovenote/module/impl/RibbonTrail.java <<'EOF'
package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Tex;
import com.lovenote.util.Vfx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;

import java.util.ArrayDeque;

/** A wide glowing ribbon that trails behind you as you move. */
public class RibbonTrail extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Fire", "White"));
	private final NumberSetting length = add(new NumberSetting("Length", 44, 6, 140, 1));
	private final NumberSetting width = add(new NumberSetting("Width", 0.26, 0.04, 1.4, 0.02));
	private final NumberSetting taper = add(new NumberSetting("Taper", 0.75, 0.0, 1.0, 0.05));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 190, 20, 255, 5));
	private final NumberSetting twist = add(new NumberSetting("Twist", 0.0, -3.0, 3.0, 0.05));
	private final NumberSetting minMove = add(new NumberSetting("Min move", 0.03, 0.0, 0.5, 0.01));
	private final BoolSetting second = add(new BoolSetting("Double ribbon", true));
	private final NumberSetting offset = add(new NumberSetting("Offset", 0.5, 0.05, 2.0, 0.05));

	private final ArrayDeque<double[]> pts = new ArrayDeque<>();
	private double lx, ly, lz;

	public RibbonTrail() {
		super("RibbonTrail", Category.VISUALS);
		describe("Wide glowing ribbon trailing behind you.");
	}

	@Override
	public void onDisable() { pts.clear(); }

	@Override
	public void onTick() {
		if (mc.player == null) return;
		double x = mc.player.getX(), y = mc.player.getY() + 1.0, z = mc.player.getZ();
		if (Math.abs(x - lx) + Math.abs(y - ly) + Math.abs(z - lz) < minMove.get()) return;
		lx = x; ly = y; lz = z;
		pts.addLast(new double[]{x, y, z});
		while (pts.size() > length.getI()) pts.removeFirst();
	}

	private int tint(float o) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(o);
			case "Ice" -> Colors.lerp(0xFFB7F0FF, 0xFF7C6CFF, o);
			case "Fire" -> Colors.lerp(0xFFFFD166, 0xFFFF3B3B, o);
			case "White" -> 0xFFFFFFFF;
			default -> Colors.skeet(o);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (pts.size() < 3) return;
		Matrix4f mat = stack.last().pose();
		var cam = ctx.camera().position();

		int n = pts.size();
		float w = width.getF();
		float tp = taper.getF();
		int op = opacity.getI();
		float tw = twist.getF();
		float off = offset.getF();
		float t = System.currentTimeMillis() / 1000f;

		Fx.texture(Tex.SOFT);
		MeshBuilder b = Fx.texQuads();

		double[] prev = null;
		int i = 0;
		for (double[] p : pts) {
			if (prev != null) {
				float f0 = 1f - (i - 1) / (float) n;
				float f1 = 1f - i / (float) n;
				float th0 = w * (1f - tp + tp * f0);
				float th1 = w * (1f - tp + tp * f1);
				int a0 = (int) (op * f0 * f0);
				int a1 = (int) (op * f1 * f1);

				double o0 = Math.sin(t * 3 + i * 0.5) * tw * 0.3;
				double o1 = Math.sin(t * 3 + (i + 1) * 0.5) * tw * 0.3;

				Vfx.ribbon(b, mat, prev[0], prev[1] + o0, prev[2], p[0], p[1] + o1, p[2],
						th0, Colors.alpha(tint(f0), a0), Colors.alpha(tint(f1), a1),
						cam.x, cam.y, cam.z);

				if (second.get()) {
					Vfx.ribbon(b, mat, prev[0], prev[1] - off + o0, prev[2],
							p[0], p[1] - off + o1, p[2],
							th0 * 0.6f, Colors.alpha(tint(f0 + 0.3f), a0 / 2),
							Colors.alpha(tint(f1 + 0.3f), a1 / 2), cam.x, cam.y, cam.z);
				}
			}
			prev = p;
			i++;
		}
		Fx.draw();
	}
}
EOF

# ------------------------------------------------------------------ StarSphere
cat > src/main/java/com/lovenote/module/impl/StarSphere.java <<'EOF'
package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Tex;
import com.lovenote.util.Vfx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;

/** A slowly rotating sphere of stars linked into constellations around you. */
public class StarSphere extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Gold", "White"));
	private final NumberSetting stars = add(new NumberSetting("Stars", 110, 20, 300, 5));
	private final NumberSetting radius = add(new NumberSetting("Radius", 3.4, 1.0, 12.0, 0.1));
	private final NumberSetting spin = add(new NumberSetting("Spin", 0.35, 0.0, 3.0, 0.05));
	private final NumberSetting starSize = add(new NumberSetting("Star size", 0.12, 0.02, 0.6, 0.01));
	private final NumberSetting linkWidth = add(new NumberSetting("Link width", 0.018, 0.004, 0.08, 0.002));
	private final NumberSetting linkDist = add(new NumberSetting("Link dist", 0.36, 0.15, 0.8, 0.01));
	private final NumberSetting twinkle = add(new NumberSetting("Twinkle", 0.8, 0.0, 3.0, 0.05));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 200, 20, 255, 5));
	private final BoolSetting links = add(new BoolSetting("Constellations", true));
	private final BoolSetting wobble = add(new BoolSetting("Wobble", true));

	private float[] sp = new float[0];
	private int[] pairs = new int[0];
	private int pairCount;
	private int built = -1;
	private float builtThr = -1f;

	public StarSphere() {
		super("StarSphere", Category.VISUALS);
		describe("Rotating constellation sphere of stars around you.");
	}

	private void build(int n, float thr) {
		if (built == n && Math.abs(builtThr - thr) < 0.001f) return;
		built = n;
		builtThr = thr;
		sp = new float[n * 3];
		float ga = (float) (Math.PI * (3 - Math.sqrt(5)));
		for (int i = 0; i < n; i++) {
			float y = 1f - (i / (float) Math.max(1, n - 1)) * 2f;
			float r = (float) Math.sqrt(Math.max(0f, 1f - y * y));
			float a = ga * i;
			sp[i * 3] = (float) Math.cos(a) * r;
			sp[i * 3 + 1] = y;
			sp[i * 3 + 2] = (float) Math.sin(a) * r;
		}
		int[] tmp = new int[Math.max(2, n * n * 2)];
		int k = 0;
		float t2 = thr * thr;
		for (int a = 0; a < n; a++) {
			for (int b = a + 1; b < n; b++) {
				float dx = sp[a * 3] - sp[b * 3];
				float dy = sp[a * 3 + 1] - sp[b * 3 + 1];
				float dz = sp[a * 3 + 2] - sp[b * 3 + 2];
				if (dx * dx + dy * dy + dz * dz <= t2) {
					tmp[k++] = a;
					tmp[k++] = b;
				}
			}
		}
		pairs = new int[Math.max(2, k)];
		if (k > 0) System.arraycopy(tmp, 0, pairs, 0, k);
		pairCount = k / 2;
	}

	private int tint(float o) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(o);
			case "Ice" -> Colors.lerp(0xFFE0F7FF, 0xFF8AB4FF, o);
			case "Gold" -> Colors.lerp(0xFFFFE9A8, 0xFFFFB020, o);
			case "White" -> 0xFFFFFFFF;
			default -> Colors.skeet(o);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null) return;
		Matrix4f mat = stack.last().pose();
		var cam = ctx.camera().position();
		double cx = mc.player.getX(), cy = mc.player.getY() + 1.0, cz = mc.player.getZ();

		int n = stars.getI();
		float rad = radius.getF();
		build(n, linkDist.getF());

		float t = System.currentTimeMillis() / 1000f;
		double ang = t * spin.getF();
		double cs = Math.cos(ang), sn = Math.sin(ang);
		float wob = wobble.get() ? (float) Math.sin(t * 0.5) * 0.35f : 0f;
		float twk = twinkle.getF();
		int op = opacity.getI();

		float[] wx = new float[n];
		float[] wy = new float[n];
		float[] wz = new float[n];
		for (int i = 0; i < n; i++) {
			float x = sp[i * 3], y = sp[i * 3 + 1], z = sp[i * 3 + 2];
			float rx = (float) (x * cs - z * sn);
			float rz = (float) (x * sn + z * cs);
			float ry = y + wob * (float) Math.sin(t + i);
			wx[i] = (float) (cx + rx * rad);
			wy[i] = (float) (cy + ry * rad);
			wz[i] = (float) (cz + rz * rad);
		}

		if (links.get() && pairCount > 0) {
			MeshBuilder b = Fx.colorQuads();
			float lw = linkWidth.getF();
			for (int p = 0; p < pairCount; p++) {
				int a = pairs[p * 2], q = pairs[p * 2 + 1];
				int c = Colors.alpha(tint(a / (float) Math.max(1, n)), (int) (op * 0.55f));
				Vfx.tube(b, mat, wx[a], wy[a], wz[a], wx[q], wy[q], wz[q], lw, c, c,
						cam.x, cam.y, cam.z);
			}
			Fx.draw();
		}

		Fx.cameraBasis(ctx.camera());
		Tex.ensure();
		Fx.texture(Tex.STAR);
		MeshBuilder g = Fx.texQuads();
		for (int i = 0; i < n; i++) {
			float f = twk > 0f ? 0.55f + 0.45f * (float) Math.sin(t * twk * 2f + i) : 1f;
			int a = (int) (op * f);
			if (a < 4) continue;
			Fx.billboard(g, mat, wx[i], wy[i], wz[i], starSize.getF() * (0.7f + 0.6f * f),
					Colors.alpha(tint(i / (float) Math.max(1, n)), a));
		}
		Fx.draw();
	}
}
EOF

# ------------------------------------------------------------------ SparkBurst
cat > src/main/java/com/lovenote/module/impl/SparkBurst.java <<'EOF'
package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Tex;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Random;

/** Physics sparkles bursting on hit, on jump and on landing. */
public class SparkBurst extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Fire", "White"));
	private final NumberSetting amount = add(new NumberSetting("Amount", 26, 3, 90, 1));
	private final NumberSetting speed = add(new NumberSetting("Speed", 3.2, 0.2, 12.0, 0.1));
	private final NumberSetting gravity = add(new NumberSetting("Gravity", 4.5, -4.0, 14.0, 0.1));
	private final NumberSetting drag = add(new NumberSetting("Drag", 1.4, 0.0, 6.0, 0.1));
	private final NumberSetting life = add(new NumberSetting("Life (ms)", 900, 150, 3000, 50));
	private final NumberSetting size = add(new NumberSetting("Size", 0.16, 0.03, 0.8, 0.01));
	private final BoolSetting onHit = add(new BoolSetting("On hit", true));
	private final BoolSetting onJump = add(new BoolSetting("On jump", true));
	private final BoolSetting onLand = add(new BoolSetting("On land", true));

	private static final class Spark {
		double x, y, z, vx, vy, vz;
		long t0;
		float sz;
		int col;
	}

	private final ArrayList<Spark> sparks = new ArrayList<>();
	private final Random rnd = new Random();
	private boolean wasGround = true;

	public SparkBurst() {
		super("SparkBurst", Category.VISUALS);
		describe("Physics sparkles bursting on hit, jump and landing.");
	}

	private void burst(double x, double y, double z, int n) {
		for (int i = 0; i < n; i++) {
			Spark s = new Spark();
			s.x = x; s.y = y; s.z = z;
			double a = rnd.nextDouble() * Math.PI * 2;
			double e = rnd.nextDouble() * 0.9;
			double sp = speed.get() * (0.4 + rnd.nextDouble() * 0.9);
			s.vx = Math.cos(a) * Math.cos(e) * sp;
			s.vz = Math.sin(a) * Math.cos(e) * sp;
			s.vy = Math.sin(e) * sp + 0.6;
			s.t0 = System.currentTimeMillis();
			s.sz = size.getF() * (0.6f + rnd.nextFloat() * 0.9f);
			s.col = tint(rnd.nextFloat());
			sparks.add(s);
		}
		if (sparks.size() > 900) sparks.subList(0, sparks.size() - 900).clear();
	}

	private int tint(float o) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(o);
			case "Ice" -> Colors.lerp(0xFFE0F7FF, 0xFF8AB4FF, o);
			case "Fire" -> Colors.lerp(0xFFFFF3B0, 0xFFFF3B3B, o);
			case "White" -> 0xFFFFFFFF;
			default -> Colors.skeet(o);
		};
	}

	@Override
	public void onAttack(Entity target) {
		if (!onHit.get() || target == null) return;
		burst(target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), amount.getI());
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		boolean g = mc.player.onGround();
		if (!wasGround && g && onLand.get()) {
			burst(mc.player.getX(), mc.player.getY() + 0.1, mc.player.getZ(), Math.max(1, amount.getI() / 2));
		} else if (wasGround && !g && onJump.get()) {
			burst(mc.player.getX(), mc.player.getY() + 0.15, mc.player.getZ(), Math.max(1, amount.getI() / 2));
		}
		wasGround = g;
	}

	@Override
	public void onDisable() { sparks.clear(); }

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (sparks.isEmpty()) return;
		Matrix4f mat = stack.last().pose();

		long now = System.currentTimeMillis();
		int dur = life.getI();
		sparks.removeIf(s -> now - s.t0 > dur);
		if (sparks.isEmpty()) return;

		Fx.cameraBasis(ctx.camera());
		Tex.ensure();
		Fx.texture(Tex.STAR);
		MeshBuilder b = Fx.texQuads();

		double g = gravity.get();
		double dr = drag.get();

		for (Spark s : sparks) {
			float t = (now - s.t0) / 1000f;
			float f = 1f - t / (dur / 1000f);
			if (f <= 0f) continue;
			double d = Math.max(0.15, 1.0 - dr * t * 0.35);
			double x = s.x + s.vx * t * d;
			double y = s.y + s.vy * t - 0.5 * g * t * t;
			double z = s.z + s.vz * t * d;
			int a = (int) (255 * f * f);
			if (a < 5) continue;
			Fx.billboard(b, mat, (float) x, (float) y, (float) z, s.sz * (0.5f + f),
					Colors.alpha(s.col, a));
		}
		Fx.draw();
	}
}
EOF

# ------------------------------------------------------------------ PrismOrb
cat > src/main/java/com/lovenote/module/impl/PrismOrb.java <<'EOF'
package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Tex;
import com.lovenote.util.Vfx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;

/** A spinning glass prism orb with a counter-rotating core and orbiting sparks. */
public class PrismOrb extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Void", "White"));
	private final NumberSetting radius = add(new NumberSetting("Radius", 1.35, 0.4, 5.0, 0.05));
	private final NumberSetting spin = add(new NumberSetting("Spin", 1.1, 0.0, 4.0, 0.05));
	private final NumberSetting width = add(new NumberSetting("Line width", 0.028, 0.006, 0.12, 0.004));
	private final NumberSetting faceAlpha = add(new NumberSetting("Face glow", 46, 0, 140, 2));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 220, 20, 255, 5));
	private final NumberSetting sparks = add(new NumberSetting("Sparks", 7, 0, 24, 1));
	private final NumberSetting sparkSize = add(new NumberSetting("Spark size", 0.16, 0.03, 0.7, 0.01));
	private final BoolSetting core = add(new BoolSetting("Pulsing core", true));
	private final BoolSetting faces = add(new BoolSetting("Glass faces", true));

	public PrismOrb() {
		super("PrismOrb", Category.VISUALS);
		describe("Spinning glass prism orb with a counter-rotating core.");
	}

	private int tint(float o) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(o);
			case "Ice" -> Colors.lerp(0xFFD6F3FF, 0xFF7C6CFF, o);
			case "Void" -> Colors.lerp(0xFFB28DFF, 0xFF1B0B3B, o);
			case "White" -> 0xFFFFFFFF;
			default -> Colors.skeet(o);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null) return;
		Matrix4f mat = stack.last().pose();
		var cam = ctx.camera().position();
		double cx = mc.player.getX(), cy = mc.player.getY() + 1.15, cz = mc.player.getZ();

		float t = System.currentTimeMillis() / 1000f;
		float r = radius.getF();
		float th = width.getF();
		int op = opacity.getI();
		int fa = faceAlpha.getI();
		double yaw = t * spin.getF();
		double pitch = t * spin.getF() * 0.6;

		Vfx.P[] v = new Vfx.P[6];
		v[0] = new Vfx.P(r, 0, 0);
		v[1] = new Vfx.P(-r, 0, 0);
		v[2] = new Vfx.P(0, r, 0);
		v[3] = new Vfx.P(0, -r, 0);
		v[4] = new Vfx.P(0, 0, r);
		v[5] = new Vfx.P(0, 0, -r);
		for (int i = 0; i < 6; i++) {
			Vfx.P p = Vfx.rot(v[i], yaw, pitch, 0);
			v[i] = new Vfx.P(cx + p.x, cy + p.y, cz + p.z);
		}

		int[][] edges = {
				{2, 0}, {2, 1}, {2, 4}, {2, 5},
				{3, 0}, {3, 1}, {3, 4}, {3, 5},
				{0, 4}, {4, 1}, {1, 5}, {5, 0}
		};

		MeshBuilder b = Fx.colorQuads();

		if (faces.get() && fa > 0) {
			int[][] tri = {
					{2, 0, 4}, {2, 4, 1}, {2, 1, 5}, {2, 5, 0},
					{3, 4, 0}, {3, 1, 4}, {3, 5, 1}, {3, 0, 5}
			};
			for (int[] f : tri) {
				int c1 = Colors.alpha(tint(f[0] / 6f), fa);
				int c2 = Colors.alpha(tint(f[1] / 6f), (int) (fa * 0.5f));
				Vfx.P p0 = v[f[0]], p1 = v[f[1]], p2 = v[f[2]];
				Fx.vc(b, mat, (float) p0.x, (float) p0.y, (float) p0.z, c1);
				Fx.vc(b, mat, (float) p1.x, (float) p1.y, (float) p1.z, c2);
				Fx.vc(b, mat, (float) p2.x, (float) p2.y, (float) p2.z, c2);
				Fx.vc(b, mat, (float) p2.x, (float) p2.y, (float) p2.z, c2);
			}
		}

		for (int i = 0; i < edges.length; i++) {
			Vfx.P a = v[edges[i][0]], d = v[edges[i][1]];
			int c = Colors.alpha(tint(i / (float) edges.length), op);
			Vfx.tube(b, mat, a.x, a.y, a.z, d.x, d.y, d.z, th, c, c, cam.x, cam.y, cam.z);
		}

		double iy = -t * spin.getF() * 1.7;
		double ip = t * spin.getF() * 1.1;
		double ir = r * 0.45;
		Vfx.P[] tv = new Vfx.P[4];
		for (int i = 0; i < 4; i++) {
			double a = i * Math.PI * 0.5;
			Vfx.P p = new Vfx.P(Math.cos(a) * ir, (i % 2 == 0 ? 1 : -1) * ir * 0.8, Math.sin(a) * ir);
			p = Vfx.rot(p, iy, ip, 0);
			tv[i] = new Vfx.P(cx + p.x, cy + p.y, cz + p.z);
		}
		for (int i = 0; i < 4; i++) {
			for (int j = i + 1; j < 4; j++) {
				int c = Colors.alpha(tint(0.5f), (int) (op * 0.7f));
				Vfx.tube(b, mat, tv[i].x, tv[i].y, tv[i].z, tv[j].x, tv[j].y, tv[j].z,
						th * 0.7f, c, c, cam.x, cam.y, cam.z);
			}
		}
		Fx.draw();

		Fx.cameraBasis(ctx.camera());
		Tex.ensure();
		Fx.texture(Tex.GLOW);
		MeshBuilder g = Fx.texQuads();

		if (core.get()) {
			float pulse = 0.7f + 0.3f * (float) Math.sin(t * 2.4);
			Fx.billboard(g, mat, (float) cx, (float) cy, (float) cz, r * 1.5f * pulse,
					Colors.alpha(tint(0.1f), (int) (op * 0.45f)));
			Fx.billboard(g, mat, (float) cx, (float) cy, (float) cz, r * 0.5f * pulse,
					Colors.alpha(0xFFFFFFFF, (int) (op * 0.8f)));
		}

		int n = sparks.getI();
		for (int i = 0; i < n; i++) {
			double a = i / (double) Math.max(1, n) * Math.PI * 2 + t * spin.getF() * 2.2;
			double rr = r * 1.7;
			double y = cy + Math.sin(a * 2 + t * 2) * r * 0.7;
			Fx.billboard(g, mat, (float) (cx + Math.cos(a) * rr), (float) y,
					(float) (cz + Math.sin(a) * rr), sparkSize.getF(),
					Colors.alpha(tint(i / (float) Math.max(1, n)), op));
		}
		Fx.draw();
	}
}
EOF

# ------------------------------------------------------------------ SkyExtras
cat > src/main/java/com/lovenote/module/impl/SkyExtras.java <<'EOF'
package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Gl;
import com.lovenote.util.Tex;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.Random;

/** Extra celestial layers that sit on top of whatever SkyShader is doing. */
public class SkyExtras extends Module {
	private final BoolSetting planet = add(new BoolSetting("Ringed planet", true));
	private final NumberSetting planetSize = add(new NumberSetting("Planet size", 30, 6, 120, 1));
	private final BoolSetting twinSuns = add(new BoolSetting("Twin suns", false));
	private final BoolSetting rainbow = add(new BoolSetting("Rainbow arc", true));
	private final NumberSetting rainbowAlpha = add(new NumberSetting("Rainbow opacity", 70, 0, 255, 5));
	private final BoolSetting bolts = add(new BoolSetting("Sky lightning", true));
	private final NumberSetting boltRate = add(new NumberSetting("Lightning rate", 0.4, 0.0, 3.0, 0.05));
	private final BoolSetting bands = add(new BoolSetting("Cloud bands", true));
	private final NumberSetting bandAlpha = add(new NumberSetting("Band opacity", 40, 0, 200, 5));
	private final BoolSetting overworldOnly = add(new BoolSetting("Overworld only", true));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 210, 20, 255, 5));

	private static final float R = 300f;
	private final Random rnd = new Random();
	private long lastBolt;
	private float boltX, boltY, boltZ, boltLife;
	private float[] bandSeed;

	public SkyExtras() {
		super("SkyExtras", Category.VISUALS);
		describe("Ringed planet, twin suns, rainbow arc and sky lightning.");
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null || mc.level == null) return;
		if (overworldOnly.get() && !mc.level.dimension().location().getPath().equals("overworld")) return;

		Matrix4f mat = stack.last().pose();
		var cam = ctx.camera().position();
		double cx = cam.x, cy = cam.y, cz = cam.z;
		float t = System.currentTimeMillis() / 1000f;
		float td = ctx.tickDelta();
		float skyAng = Gl.skyAngle(mc.level, td) * 6.28318f;
		int op = opacity.getI();

		Fx.cameraBasis(ctx.camera());
		Tex.ensure();

		if (planet.get()) {
			float pAng = t * 0.03f;
			float px = (float) (cx + Mth.cos(pAng) * R * 0.8f);
			float pz = (float) (cz + Mth.sin(pAng) * R * 0.8f);
			float py = (float) cy + 150f;
			int col = Colors.alpha(Colors.lerp(0xFF8AB4FF, 0xFFC4B5FD, 0.5f), op);
			Fx.texture(Tex.GLOW);
			MeshBuilder b = Fx.texQuads();
			Fx.billboard(b, mat, px, py, pz, planetSize.getF() * 1.5f, Colors.alpha(col, op / 3));
			Fx.billboard(b, mat, px, py, pz, planetSize.getF(), col);
			float rw = planetSize.getF() * 1.9f;
			for (int i = 0; i < 3; i++) {
				float rr = rw * (0.8f + i * 0.14f);
				int a = (int) (op * 0.5f * (1f - i * 0.25f));
				Fx.billboard(b, mat, px, py + i * 0.6f, pz, rr, Colors.alpha(col, a));
			}
			Fx.draw();
		}

		if (twinSuns.get()) {
			Fx.texture(Tex.GLOW);
			MeshBuilder b = Fx.texQuads();
			for (int k = 0; k < 2; k++) {
				float a2 = skyAng + k * 0.35f;
				float sx = (float) (cx + Mth.cos(a2) * R);
				float sy = (float) (cy + Mth.sin(a2) * R);
				int col = Colors.alpha(k == 0 ? 0xFFFFE9A8 : 0xFFFFC48A, op);
				Fx.billboard(b, mat, sx, sy, (float) cz, 60f, Colors.alpha(col, op / 2));
				Fx.billboard(b, mat, sx, sy, (float) cz, 30f, col);
			}
			Fx.draw();
		}

		if (rainbow.get()) {
			int a = rainbowAlpha.getI();
			Fx.texture(Tex.SOFT);
			MeshBuilder b = Fx.texQuads();
			int bandsN = 7;
			for (int i = 0; i < bandsN; i++) {
				float rr = R * 0.55f + i * 4f;
				int col = Colors.hsv(i / (float) bandsN, 0.75f, 1f);
				for (int s = 0; s < 36; s++) {
					double a0 = Math.PI + s / 36.0 * Math.PI;
					double a1 = Math.PI + (s + 1) / 36.0 * Math.PI;
					float x0 = (float) (cx + Math.cos(a0) * rr);
					float y0 = (float) (cy + Math.sin(a0) * rr * 0.5f);
					float x1 = (float) (cx + Math.cos(a1) * rr);
					float y1 = (float) (cy + Math.sin(a1) * rr * 0.5f);
					float w = 3.2f;
					Fx.vt(b, mat, x0, y0, (float) cz - R * 0.2f, 0f, 0f, Colors.alpha(col, a / 2));
					Fx.vt(b, mat, x1, y1, (float) cz - R * 0.2f, 1f, 0f, Colors.alpha(col, a / 2));
					Fx.vt(b, mat, x1, y1 + w, (float) cz - R * 0.2f, 1f, 1f, Colors.alpha(col, a));
					Fx.vt(b, mat, x0, y0 + w, (float) cz - R * 0.2f, 0f, 1f, Colors.alpha(col, a));
				}
			}
			Fx.draw();
		}

		if (bands.get()) {
			if (bandSeed == null) {
				bandSeed = new float[14 * 3];
				for (int i = 0; i < 14; i++) {
					bandSeed[i * 3] = rnd.nextFloat();
					bandSeed[i * 3 + 1] = rnd.nextFloat();
					bandSeed[i * 3 + 2] = 0.5f + rnd.nextFloat();
				}
			}
			int a = bandAlpha.getI();
			Fx.texture(Tex.SOFT);
			MeshBuilder b = Fx.texQuads();
			for (int i = 0; i < 14; i++) {
				float ang = bandSeed[i * 3] * 6.2832f + t * 0.02f * bandSeed[i * 3 + 2];
				float rad = R * (0.5f + bandSeed[i * 3 + 1] * 0.5f);
				float y = (float) cy + 90f + bandSeed[i * 3 + 1] * 90f;
				float x = (float) (cx + Math.cos(ang) * rad);
				float z = (float) (cz + Math.sin(ang) * rad);
				int col = Colors.alpha(Colors.lerp(Colors.C1, 0xFFFFFFFF, 0.4f), a);
				Fx.billboard(b, mat, x, y, z, 140f + bandSeed[i * 3 + 2] * 120f, col);
			}
			Fx.draw();
		}

		if (bolts.get()) {
			long now = System.currentTimeMillis();
			if (boltLife <= 0f && rnd.nextFloat() < boltRate.get() * 0.04f) {
				float a2 = rnd.nextFloat() * 6.2832f;
				boltX = (float) (cx + Math.cos(a2) * R * 0.7f);
				boltZ = (float) (cz + Math.sin(a2) * R * 0.7f);
				boltY = (float) cy + 120f + rnd.nextFloat() * 60f;
				boltLife = 0.55f;
				lastBolt = now;
			}
			if (boltLife > 0f) {
				boltLife -= (now - lastBolt) / 1000f;
				lastBolt = now;
				float f = Mth.clamp(boltLife / 0.55f, 0f, 1f);
				int a = (int) (255 * f);
				if (a > 4) {
					Fx.texture(Tex.SOFT);
					MeshBuilder b = Fx.texQuads();
					Fx.billboard(b, mat, boltX, boltY, boltZ, 220f, Colors.alpha(0xFFCFE6FF, a / 4));
					Fx.billboard(b, mat, boltX, boltY, boltZ, 90f, Colors.alpha(0xFFFFFFFF, a));
					float y = boltY;
					float x = boltX, z = boltZ;
					for (int i = 0; i < 12; i++) {
						x += (rnd.nextFloat() - 0.5f) * 26f;
						z += (rnd.nextFloat() - 0.5f) * 26f;
						y -= 22f;
						Fx.billboard(b, mat, x, y, z, 26f, Colors.alpha(0xFFFFFFFF, a / 2));
					}
					Fx.draw();
				}
			}
		}
	}
}
EOF

# ------------------------------------------------------------------ Vortex
cat > src/main/java/com/lovenote/module/impl/Vortex.java <<'EOF'
package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Tex;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;

/** A spiralling particle vortex around you. */
public class Vortex extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Fire", "White"));
	private final NumberSetting arms = add(new NumberSetting("Arms", 4, 1, 12, 1));
	private final NumberSetting perArm = add(new NumberSetting("Per arm", 40, 5, 120, 5));
	private final NumberSetting radius = add(new NumberSetting("Radius", 2.2, 0.4, 10.0, 0.1));
	private final NumberSetting rise = add(new NumberSetting("Rise", 3.0, 0.5, 20.0, 0.5));
	private final NumberSetting speed = add(new NumberSetting("Speed", 1.2, 0.0, 5.0, 0.05));
	private final NumberSetting size = add(new NumberSetting("Size", 0.2, 0.03, 0.9, 0.01));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 190, 20, 255, 5));
	private final BoolSetting reverse = add(new BoolSetting("Reverse", false));

	public Vortex() {
		super("Vortex", Category.VISUALS);
		describe("Spiralling particle vortex around you.");
	}

	private int tint(float o) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(o);
			case "Ice" -> Colors.lerp(0xFFE0F7FF, 0xFF7C6CFF, o);
			case "Fire" -> Colors.lerp(0xFFFFF3B0, 0xFFFF3B3B, o);
			case "White" -> 0xFFFFFFFF;
			default -> Colors.skeet(o);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null) return;
		Matrix4f mat = stack.last().pose();
		double cx = mc.player.getX(), cy = mc.player.getY(), cz = mc.player.getZ();

		float t = System.currentTimeMillis() / 1000f;
		float dir = reverse.get() ? -1f : 1f;
		float sp = speed.getF() * dir;
		int armsN = arms.getI();
		int per = perArm.getI();
		float rad = radius.getF();
		float rs = rise.getF();
		float sz = size.getF();
		int op = opacity.getI();

		Fx.cameraBasis(ctx.camera());
		Tex.ensure();
		Fx.texture(Tex.STAR);
		MeshBuilder b = Fx.texQuads();

		for (int a = 0; a < armsN; a++) {
			double base = a / (double) armsN * Math.PI * 2;
			for (int i = 0; i < per; i++) {
				float f = i / (float) per;
				double ang = base + f * 6.2832 * 1.6 + t * sp;
				double rr = rad * (0.25 + f * 0.95);
				double x = cx + Math.cos(ang) * rr;
				double z = cz + Math.sin(ang) * rr;
				double y = cy + ((f * rs + t * 0.4 * dir) % rs) - 0.5;
				int al = (int) (op * (1f - f * 0.75f));
				if (al < 4) continue;
				Fx.billboard(b, mat, (float) x, (float) y, (float) z,
						sz * (1.2f - f * 0.7f), Colors.alpha(tint(f), al));
			}
		}
		Fx.draw();
	}
}
EOF

# ------------------------------------------------------------------ PlasmaShell
cat > src/main/java/com/lovenote/module/impl/PlasmaShell.java <<'EOF'
package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Tex;
import com.lovenote.util.Vfx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;

/** A wireframe plasma shell around you. */
public class PlasmaShell extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Toxic", "White"));
	private final NumberSetting radius = add(new NumberSetting("Radius", 1.6, 0.4, 6.0, 0.05));
	private final NumberSetting lat = add(new NumberSetting("Rings", 7, 2, 20, 1));
	private final NumberSetting lon = add(new NumberSetting("Segments", 14, 4, 40, 1));
	private final NumberSetting width = add(new NumberSetting("Line width", 0.022, 0.005, 0.1, 0.002));
	private final NumberSetting spin = add(new NumberSetting("Spin", 0.7, 0.0, 3.0, 0.05));
	private final NumberSetting pulse = add(new NumberSetting("Pulse", 0.15, 0.0, 0.8, 0.01));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 170, 20, 255, 5));
	private final BoolSetting core = add(new BoolSetting("Core", true));

	public PlasmaShell() {
		super("PlasmaShell", Category.VISUALS);
		describe("Wireframe plasma shell humming around you.");
	}

	private int tint(float o) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(o);
			case "Ice" -> Colors.lerp(0xFFD6F3FF, 0xFF6E7BFF, o);
			case "Toxic" -> Colors.lerp(0xFF7CFC00, 0xFF00FF9C, o);
			case "White" -> 0xFFFFFFFF;
			default -> Colors.skeet(o);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null) return;
		Matrix4f mat = stack.last().pose();
		var cam = ctx.camera().position();
		double cx = mc.player.getX(), cy = mc.player.getY() + 1.0, cz = mc.player.getZ();

		float t = System.currentTimeMillis() / 1000f;
		float r = radius.getF() * (1f + pulse.getF() * (float) Math.sin(t * 2.2));
		int rings = lat.getI();
		int segs = lon.getI();
		float th = width.getF();
		int op = opacity.getI();

		MeshBuilder b = Fx.colorQuads();
		for (int i = 1; i < rings; i++) {
			double phi = i / (double) rings * Math.PI;
			double ry = Math.cos(phi) * r;
			double rr = Math.sin(phi) * r;
			int c = Colors.alpha(tint(i / (float) rings), op);
			for (int s = 0; s < segs; s++) {
				double a0 = s / (double) segs * Math.PI * 2 + t * spin.getF();
				double a1 = (s + 1) / (double) segs * Math.PI * 2 + t * spin.getF();
				double x0 = cx + Math.cos(a0) * rr, z0 = cz + Math.sin(a0) * rr;
				double x1 = cx + Math.cos(a1) * rr, z1 = cz + Math.sin(a1) * rr;
				Vfx.tube(b, mat, x0, cy + ry, z0, x1, cy + ry, z1, th, c, c, cam.x, cam.y, cam.z);
			}
		}
		int lonN = Math.max(3, segs / 3);
		for (int s = 0; s < lonN; s++) {
			double a = s / (double) lonN * Math.PI * 2 + t * spin.getF();
			int c = Colors.alpha(tint(s / (float) lonN), (int) (op * 0.8f));
			for (int i = 0; i < rings; i++) {
				double p0 = i / (double) rings * Math.PI, p1 = (i + 1) / (double) rings * Math.PI;
				Vfx.tube(b, mat,
						cx + Math.cos(a) * Math.sin(p0) * r, cy + Math.cos(p0) * r, cz + Math.sin(a) * Math.sin(p0) * r,
						cx + Math.cos(a) * Math.sin(p1) * r, cy + Math.cos(p1) * r, cz + Math.sin(a) * Math.sin(p1) * r,
						th, c, c, cam.x, cam.y, cam.z);
			}
		}
		Fx.draw();

		if (!core.get()) return;
		Fx.cameraBasis(ctx.camera());
		Tex.ensure();
		Fx.texture(Tex.GLOW);
		MeshBuilder g = Fx.texQuads();
		Fx.billboard(g, mat, (float) cx, (float) cy, (float) cz, r * 0.6f,
				Colors.alpha(tint(0.3f), (int) (op * 0.5f)));
		Fx.draw();
	}
}
EOF

# ------------------------------------------------------------------ ArcBeams
cat > src/main/java/com/lovenote/module/impl/ArcBeams.java <<'EOF'
package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Vfx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

import java.util.Random;

/** Crackling electric arcs between you and nearby players. */
public class ArcBeams extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Volt", "White"));
	private final NumberSetting range = add(new NumberSetting("Range", 24, 4, 64, 1));
	private final NumberSetting steps = add(new NumberSetting("Jitter", 14, 3, 40, 1));
	private final NumberSetting jitter = add(new NumberSetting("Jitter size", 0.5, 0.05, 2.5, 0.05));
	private final NumberSetting width = add(new NumberSetting("Line width", 0.035, 0.008, 0.14, 0.004));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 200, 20, 255, 5));
	private final NumberSetting refresh = add(new NumberSetting("Refresh (ms)", 90, 20, 600, 10));
	private final BoolSetting heads = add(new BoolSetting("End sparks", true));

	private final Random rnd = new Random();
	private long lastRefresh;
	private double[][] offsets;

	public ArcBeams() {
		super("ArcBeams", Category.VISUALS);
		describe("Electric arcs arcing between you and nearby players.");
	}

	private int tint(float o) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(o);
			case "Ice" -> Colors.lerp(0xFFE0F7FF, 0xFF7C6CFF, o);
			case "Volt" -> Colors.lerp(0xFFFFFFA8, 0xFF00E5FF, o);
			case "White" -> 0xFFFFFFFF;
			default -> Colors.skeet(o);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null || mc.level == null) return;
		long now = System.currentTimeMillis();
		if (offsets == null || now - lastRefresh > refresh.getI()) {
			lastRefresh = now;
			offsets = new double[64][3];
			for (double[] o : offsets) {
				o[0] = (rnd.nextDouble() - 0.5);
				o[1] = (rnd.nextDouble() - 0.5);
				o[2] = (rnd.nextDouble() - 0.5);
			}
		}

		Matrix4f mat = stack.last().pose();
		var cam = ctx.camera().position();
		Player self = mc.player;
		double sx = self.getX(), sy = self.getY() + 1.0, sz = self.getZ();
		double rng = range.getF();
		int n = steps.getI();
		float jit = jitter.getF();
		float th = width.getF();
		int op = opacity.getI();
		float t = now / 1000f;

		MeshBuilder b = Fx.colorQuads();
		int used = 0;
		for (Player p : mc.level.players()) {
			if (p == self) continue;
			double dx = p.getX() - sx, dy = p.getY() + 1.0 - sy, dz = p.getZ() - sz;
			if (dx * dx + dy * dy + dz * dz > rng * rng) continue;
			double ex = p.getX(), ey = p.getY() + 1.0, ez = p.getZ();
			int col = Colors.alpha(tint((used % 8) / 8f + t * 0.05f), op);

			double px = sx, py = sy, pz = sz;
			for (int i = 1; i <= n; i++) {
				double f = i / (double) n;
				double taper = Math.sin(f * Math.PI) * jit;
				double ox = (offsets[(used * 7 + i) % offsets.length][0]) * taper;
				double oy = (offsets[(used * 7 + i) % offsets.length][1]) * taper;
				double oz = (offsets[(used * 7 + i) % offsets.length][2]) * taper;
				double nx = sx + dx * f + ox;
				double ny = sy + dy * f + oy;
				double nz = sz + dz * f + oz;
				Vfx.tube(b, mat, px, py, pz, nx, ny, nz, th, col, col, cam.x, cam.y, cam.z);
				px = nx; py = ny; pz = nz;
			}
			if (heads.get()) {
				Vfx.tube(b, mat, px, py, pz, ex, ey, ez, th * 1.4f, col, col, cam.x, cam.y, cam.z);
			}
			used++;
		}
		if (used > 0) Fx.draw();
	}
}
EOF

# ------------------------------------------------------------------ PrismRain
cat > src/main/java/com/lovenote/module/impl/PrismRain.java <<'EOF'
package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Vfx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;

/** Prism shards drifting down around you. */
public class PrismRain extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Prism", "White"));
	private final NumberSetting count = add(new NumberSetting("Count", 30, 2, 120, 1));
	private final NumberSetting spread = add(new NumberSetting("Spread", 10, 2, 30, 0.5));
	private final NumberSetting height = add(new NumberSetting("Height", 12, 3, 40, 1));
	private final NumberSetting speed = add(new NumberSetting("Fall speed", 1.6, 0.1, 8.0, 0.1));
	private final NumberSetting size = add(new NumberSetting("Size", 0.14, 0.03, 0.6, 0.01));
	private final NumberSetting len = add(new NumberSetting("Length", 0.5, 0.1, 2.0, 0.05));
	private final NumberSetting spin = add(new NumberSetting("Spin", 1.2, 0.0, 5.0, 0.05));
	private final NumberSetting width = add(new NumberSetting("Line width", 0.02, 0.004, 0.09, 0.002));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 170, 20, 255, 5));

	public PrismRain() {
		super("PrismRain", Category.VISUALS);
		describe("Prism shards drifting down around you.");
	}

	private int tint(float o) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(o);
			case "Ice" -> Colors.lerp(0xFFE0F7FF, 0xFF7C6CFF, o);
			case "Prism" -> Colors.hsv(o * 3f, 0.55f, 1f);
			case "White" -> 0xFFFFFFFF;
			default -> Colors.skeet(o);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null) return;
		Matrix4f mat = stack.last().pose();
		var cam = ctx.camera().position();
		double cx = mc.player.getX(), cy = mc.player.getY(), cz = mc.player.getZ();

		float t = System.currentTimeMillis() / 1000f;
		int n = count.getI();
		float sp = spread.getF();
		float hh = height.getF();
		float spd = speed.getF();
		float r = size.getF();
		float h = len.getF();
		float th = width.getF();
		int op = opacity.getI();

		MeshBuilder b = Fx.colorQuads();
		for (int i = 0; i < n; i++) {
			float h1 = Vfx.hash(i * 5 + 1), h2 = Vfx.hash(i * 5 + 2), h3 = Vfx.hash(i * 5 + 3);
			double x = cx + (h1 * 2 - 1) * sp;
			double z = cz + (h3 * 2 - 1) * sp;
			double y = cy + hh - ((h2 * hh + t * spd) % hh);
			int a = (int) (op * (0.45f + 0.55f * h3));
			if (a < 4) continue;
			double ang = t * spin.getF() * (0.5 + h2) + h1 * 6.28;
			int c = Colors.alpha(tint(h1), a);
			Vfx.crystal(b, mat, new Vfx.P(x, y, z), r, h, ang, ang * 0.6, ang * 0.4, th,
					c, Colors.alpha(tint(h1), a / 2), cam.x, cam.y, cam.z);
		}
		Fx.draw();
	}
}
EOF

# =====================================================================
#  mixins
# =====================================================================
echo "== mixins =="
python3 - <<'PY'
import json, pathlib
cands = sorted(pathlib.Path("src/main/resources").glob("*.mixins.json"))
if not cands:
    print("!! no *.mixins.json found - mixins skipped"); raise SystemExit
cfg = cands[0]
try:
    data = json.loads(cfg.read_text())
except Exception as e:
    print("!! mixin config is not plain JSON:", e); raise SystemExit
pkg = data.get("package")
key = "client" if "client" in data else ("mixins" if "mixins" in data else None)
if not pkg or key is None:
    print("!! mixin config missing 'package' or client/mixins array"); raise SystemExit
for n in ["LovenoteScreenFxMixin", "LovenoteCrosshairMixin"]:
    if n not in data[key]: data[key].append(n)
cfg.write_text(json.dumps(data, indent=2) + "\n")
print("mixins config ok:", cfg.name, "package", pkg)

d = pathlib.Path("src/main/java") / pkg.replace(".", "/")
d.mkdir(parents=True, exist_ok=True)

(d / "LovenoteScreenFxMixin.java").write_text('''package {PKG};

import com.lovenote.util.MenuFx;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class LovenoteScreenFxMixin {
\t@Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
\tprivate void lovenote$screenFx(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
\t\tMenuFx.onScreen(g, mouseX, mouseY, delta);
\t}
}
'''.replace("{PKG}", pkg))

(d / "LovenoteCrosshairMixin.java").write_text('''package {PKG};

import com.lovenote.module.impl.FunnyCrosshair;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class LovenoteCrosshairMixin {
\t@Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
\tprivate void lovenote$crosshairExtract(GuiGraphicsExtractor g, CallbackInfo ci) {
\t\tif (FunnyCrosshair.hideVanilla()) ci.cancel();
\t}

\t@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
\tprivate void lovenote$crosshairRender(GuiGraphicsExtractor g, CallbackInfo ci) {
\t\tif (FunnyCrosshair.hideVanilla()) ci.cancel();
\t}
}
'''.replace("{PKG}", pkg))
print("mixin sources written:", d)
PY

# =====================================================================
#  registrations (self-pruning)
# =====================================================================
echo "== registrations =="
python3 - <<'PY'
import pathlib, re
mp = pathlib.Path("src/main/java/com/lovenote/module/ModuleManager.java")
impl = pathlib.Path("src/main/java/com/lovenote/module/impl")
s = mp.read_text()

wanted = ["Interface", "LogoWidget", "FunnyCrosshair", "SkyExtras", "CubeField", "CrystalHalo",
          "AuroraVeil", "NeonGrid", "RibbonTrail", "StarSphere", "SparkBurst", "PrismOrb",
          "Vortex", "PlasmaShell", "ArcBeams", "PrismRain"]
alwaysOn = {"Interface", "LogoWidget", "FunnyCrosshair"}

lines, dropped = [], []
for line in s.splitlines(True):
    m = re.search(r"register\(new (\w+)\(\)\)", line)
    if m and not (impl / (m.group(1) + ".java")).exists():
        dropped.append(m.group(1)); continue
    lines.append(line)
s = "".join(lines)

missing = [n for n in wanted
           if (impl / (n + ".java")).exists()
           and not re.search(r"register\(new %s\(\)\)" % n, s)]

if missing:
    block = "\n\t\t// ---- lovenote pack\n"
    for n in missing:
        block += "\t\tregister(new %s())%s;\n" % (n, ".enabled = true" if n in alwaysOn else "")
    anchor = "\t\tregister(new SkyShader());"
    if anchor in s:
        s = s.replace(anchor, anchor + block, 1)
    else:
        found = re.findall(r"^.*register\(new \w+\(\)\).*$", s, re.M)
        if found:
            s = s.replace(found[-1], found[-1] + block, 1)
        else:
            i = s.find("public ModuleManager()")
            b = s.find("{", i) if i >= 0 else -1
            if b < 0:
                print("!! could not find an insertion point"); raise SystemExit
            s = s[:b + 1] + block + s[b + 1:]
    print("registered:", ", ".join(missing))
else:
    print("nothing new to register")

if dropped:
    print("pruned (no source file):", ", ".join(dropped))
mp.write_text(s)
PY

# =====================================================================
#  chrome / ice-blue accent preset
# =====================================================================
python3 - <<'PY'
import pathlib
p = pathlib.Path("src/main/java/com/lovenote/util/Colors.java")
s = p.read_text()
if "Drain" in s:
    print("Colors: Drain preset already present")
else:
    a1 = '"Midnight", "Blood", "Frost", "Haze", "Sakura", "Toxic"'
    a2 = "{0xB6FF00, 0x00FF9C}"
    if a1 in s and a2 in s:
        s = s.replace(a1, a1 + ', "Drain"', 1)
        s = s.replace(a2, a2 + ", {0xE6F0FF, 0x59C2FF}", 1)
        p.write_text(s)
        print('Colors: "Drain" preset added (chrome -> ice blue)')
    else:
        print("!! Colors anchors changed - preset not added")
PY

# =====================================================================
#  verify + build
# =====================================================================
echo
echo "===================== VERIFY ====================="
for f in util/Gfx.java util/Ui.java util/Logos.java util/MenuFx.java util/Vfx.java \
         module/impl/Interface.java module/impl/LogoWidget.java module/impl/FunnyCrosshair.java; do
  [ -f "src/main/java/com/lovenote/$f" ] && echo "  ok   $f" || echo "  FAIL $f"
done
echo "-- modules present:"
ls -1 src/main/java/com/lovenote/module/impl/ | sed 's/^/     /'
echo "-- registrations:"
grep -c "register(new" src/main/java/com/lovenote/module/ModuleManager.java | sed 's/^/     /'
echo "-- mixins:"
for n in LovenoteScreenFxMixin LovenoteCrosshairMixin; do
  grep -q "$n" src/main/resources/*.mixins.json && echo "  ok   $n" || echo "  FAIL $n"
done
[ -f run/lovenote-logo.png ] && echo "  ok   run/lovenote-logo.png" \
  || echo "  NOTE save your image as run/lovenote-logo.png (fallback mark until then)"
echo "=================================================="

echo "== build =="
./gradlew build --console=plain > /tmp/lovenote-build.log 2>&1
tail -4 /tmp/lovenote-build.log
echo
echo "--- compile errors (paste these if any) ---"
grep -E ": error:" /tmp/lovenote-build.log | head -40 || true
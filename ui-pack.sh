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
	
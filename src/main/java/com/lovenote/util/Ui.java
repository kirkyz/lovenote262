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
		MenuFx.logoStyle = i.logoStyle.index();
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

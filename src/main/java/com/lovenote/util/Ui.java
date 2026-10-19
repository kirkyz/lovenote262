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
        if (idx != lastAccent) { lastAccent = idx; Logos.invalidate(); }
        // Menu Appearance and native widgets use the SAME editable accent colours.
        int selected = meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig.accentIndex;
        int[] preset = meteordevelopment.meteorclient.gui.themes.lovenote.LovenotePalette.PRESETS[
            Math.max(0, Math.min(selected < 0 ? idx : selected,
                meteordevelopment.meteorclient.gui.themes.lovenote.LovenotePalette.PRESETS.length - 1))];
        int p = meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig.accentIndex < 0
            ? meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig.parse(
                meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig.customPrimary, preset[0]) : preset[0];
        int q = meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig.accentIndex < 0
            ? meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig.parse(
                meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig.customSecondary, preset[1]) : preset[1];
        Colors.setAccent(p,q);
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
		// The visible Appearance toggle gates this older hidden module setting.
		MenuFx.logoInMenu = i.logoInMenu.get() && meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig.decorativeLogo;
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

	    /** Shared premium HUD chrome, intentionally 3 rounded fills plus 1 lightweight sheen. */
    private static void neonChrome(GuiGraphicsExtractor g,int x,int y,int w,int h,boolean isPill){
        if(w<=0||h<=0)return;
        int r=isPill?Math.max(5,h/2):Math.max(7,Math.min(14,radius));
        int glow=Colors.alpha(accent2,Math.max(10,Math.min(80,glowAlpha)));
        if(glowOn)rrect(g,x-2,y-2,w+4,h+4,r+2,Colors.alpha(accent2,16));
        rrect(g,x-1,y-1,w+2,h+2,r+1,glow);
        rrect(g,x,y,w,h,r,Colors.alpha(0x0D0B19,Math.max(155,(bg>>>24)&255)));
        if(glass && h>13)rrect(g,x+3,y+2,w-6,Math.max(2,h/5),Math.max(2,r-2),0x1BFFFFFF);
        if(accentBorder && w>15)rect(g,x+Math.max(6,r/2),y+h-2,Math.max(2,w-Math.max(6,r/2)*2),1,Colors.alpha(accent1,94));
    }

    /** Compact glass HUD chrome with shared editable colours and low-cost shine. */
    public static void widgetCard(GuiGraphicsExtractor g, int x, int y, int w, int h, int opacity) {
        if (w <= 3 || h <= 3) return;
        int r = Math.min(3, Math.max(1,h/14));
        int a = Math.max(0, Math.min(255,opacity));
        a = Math.round(a*meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig.widgetOpacity/100f);
        int borderA = meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig.widgetBorder;
        int glowA = meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig.widgetGlow;
        if (glowOn && glowA>0) rrect(g,x-1,y-1,w+2,h+2,r+1,Colors.alpha(accent2,Math.min(30,glowA)));
        rrect(g,x,y,w,h,r,Colors.alpha(accent2,borderA));
        rrectGrad(g,x+1,y+1,w-2,h-2,Math.max(1,r-1),
            Colors.alpha(0x12101B,a),Colors.alpha(0x08080F,a));
        if (glass && meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig.widgetGlass && h>14) {
            int sheen=meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig.widgetSheen;
            if(sheen>0)rect(g,x+6,y+2,Math.max(1,w/4),1,Colors.alpha(0xFFFFFF,Math.min(24,sheen)));
        }
    }

    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        neonChrome(g,x,y,w,h,false);
    }
    public static void pill(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        // Legacy text widgets now share the premium rectangular chrome.
        widgetCard(g, x, y, w, h, 224);
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
		return ModernText.enabled() ? ModernText.width(s) : m.font.width(s);
	}
}

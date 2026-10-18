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
	public static int logoStyle = 2;
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
		// Keep the spinning mark fully within the visible GUI at any UI scale.
		int pad = 12;
		int s = Math.max(12, Math.min(logoMenuSize, Math.min(w, h) / 8));
		int x = logoMenuCorner.contains("Right") ? w - s - pad : pad;
		int y = logoMenuCorner.contains("Bottom") ? h - s - pad : pad;
		x = Math.max(0, Math.min(x, w - s));
		y = Math.max(0, Math.min(y, h - s));
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

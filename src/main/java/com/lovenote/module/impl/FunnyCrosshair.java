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
		// Default activation is handled by ModuleManager after construction.
	}

	public static boolean hideVanilla() {
		return INSTANCE != null && INSTANCE.enabled && INSTANCE.hideVanilla.get();
	}

	@Override
	public void onHud(GuiGraphicsExtractor g, float td) {
		if (mc.player == null) return;
		if (mc.options != null && mc.gameRenderer.gameRenderState().guiRenderState.isHudHidden) return;

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

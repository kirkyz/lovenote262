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

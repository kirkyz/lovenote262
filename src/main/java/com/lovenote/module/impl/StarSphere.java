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

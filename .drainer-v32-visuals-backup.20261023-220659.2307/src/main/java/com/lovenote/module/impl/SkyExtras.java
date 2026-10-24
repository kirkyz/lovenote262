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
		if (overworldOnly.get() && !mc.level.dimension().identifier().getPath().equals("overworld")) return;

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

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
			Vfx.crystal(b, mat, new Vfx.P((float) x, (float) y, (float) z), r, h * (0.85 + 0.3 * Math.sin(t * 3 + i)),
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
			Vfx.crystal(b, mat, new Vfx.P((float) x, (float) y, (float) z), (float) (r * 0.7), (float) (h * 0.55), a, -tl, -a, (float) (th * 0.8),
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
				Vfx.crystal(b, mat, new Vfx.P((float) x, (float) y, (float) z), r * 0.6, h * 0.5, a + e * 4, a, a * 2, th,
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

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

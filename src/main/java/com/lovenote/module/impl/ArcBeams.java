package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Vfx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

import java.util.Random;

/** Crackling electric arcs between you and nearby players. */
public class ArcBeams extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Volt", "White"));
	private final NumberSetting range = add(new NumberSetting("Range", 24, 4, 64, 1));
	private final NumberSetting steps = add(new NumberSetting("Jitter", 14, 3, 40, 1));
	private final NumberSetting jitter = add(new NumberSetting("Jitter size", 0.5, 0.05, 2.5, 0.05));
	private final NumberSetting width = add(new NumberSetting("Line width", 0.035, 0.008, 0.14, 0.004));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 200, 20, 255, 5));
	private final NumberSetting refresh = add(new NumberSetting("Refresh (ms)", 90, 20, 600, 10));
	private final BoolSetting heads = add(new BoolSetting("End sparks", true));

	private final Random rnd = new Random();
	private long lastRefresh;
	private double[][] offsets;

	public ArcBeams() {
		super("ArcBeams", Category.VISUALS);
		describe("Electric arcs arcing between you and nearby players.");
	}

	private int tint(float o) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(o);
			case "Ice" -> Colors.lerp(0xFFE0F7FF, 0xFF7C6CFF, o);
			case "Volt" -> Colors.lerp(0xFFFFFFA8, 0xFF00E5FF, o);
			case "White" -> 0xFFFFFFFF;
			default -> Colors.skeet(o);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null || mc.level == null) return;
		long now = System.currentTimeMillis();
		if (offsets == null || now - lastRefresh > refresh.getI()) {
			lastRefresh = now;
			offsets = new double[64][3];
			for (double[] o : offsets) {
				o[0] = (rnd.nextDouble() - 0.5);
				o[1] = (rnd.nextDouble() - 0.5);
				o[2] = (rnd.nextDouble() - 0.5);
			}
		}

		Matrix4f mat = stack.last().pose();
		var cam = ctx.camera().position();
		Player self = mc.player;
		double sx = self.getX(), sy = self.getY() + 1.0, sz = self.getZ();
		double rng = range.getF();
		int n = steps.getI();
		float jit = jitter.getF();
		float th = width.getF();
		int op = opacity.getI();
		float t = now / 1000f;

		MeshBuilder b = Fx.colorQuads();
		int used = 0;
		for (Player p : mc.level.players()) {
			if (p == self) continue;
			double dx = p.getX() - sx, dy = p.getY() + 1.0 - sy, dz = p.getZ() - sz;
			if (dx * dx + dy * dy + dz * dz > rng * rng) continue;
			double ex = p.getX(), ey = p.getY() + 1.0, ez = p.getZ();
			int col = Colors.alpha(tint((used % 8) / 8f + t * 0.05f), op);

			double px = sx, py = sy, pz = sz;
			for (int i = 1; i <= n; i++) {
				double f = i / (double) n;
				double taper = Math.sin(f * Math.PI) * jit;
				double ox = (offsets[(used * 7 + i) % offsets.length][0]) * taper;
				double oy = (offsets[(used * 7 + i) % offsets.length][1]) * taper;
				double oz = (offsets[(used * 7 + i) % offsets.length][2]) * taper;
				double nx = sx + dx * f + ox;
				double ny = sy + dy * f + oy;
				double nz = sz + dz * f + oz;
				Vfx.tube(b, mat, px, py, pz, nx, ny, nz, th, col, col, cam.x, cam.y, cam.z);
				px = nx; py = ny; pz = nz;
			}
			if (heads.get()) {
				Vfx.tube(b, mat, px, py, pz, ex, ey, ez, th * 1.4f, col, col, cam.x, cam.y, cam.z);
			}
			used++;
		}
		if (used > 0) Fx.draw();
	}
}

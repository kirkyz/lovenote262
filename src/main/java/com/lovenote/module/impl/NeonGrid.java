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
import org.joml.Matrix4f;

/** A neon floor grid locked to the world, sweeping outward with a scan pulse. */
public class NeonGrid extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Cyber", "Matrix", "White"));
	private final NumberSetting cell = add(new NumberSetting("Cell", 2.0, 0.5, 8.0, 0.5));
	private final NumberSetting extent = add(new NumberSetting("Extent", 14, 3, 40, 1));
	private final NumberSetting lineWidth = add(new NumberSetting("Line width", 0.045, 0.01, 0.2, 0.005));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 120, 10, 255, 5));
	private final NumberSetting scanSpeed = add(new NumberSetting("Scan speed", 7.0, 0.0, 30.0, 0.5));
	private final NumberSetting scanBoost = add(new NumberSetting("Scan boost", 2.4, 1.0, 6.0, 0.1));
	private final BoolSetting scan = add(new BoolSetting("Scan pulse", true));
	private final BoolSetting pulseRing = add(new BoolSetting("Pulse ring", true));
	private final NumberSetting yOffset = add(new NumberSetting("Y offset", 0.05, 0.0, 0.5, 0.01));

	public NeonGrid() {
		super("NeonGrid", Category.VISUALS);
		describe("Neon floor grid with a radar scan sweeping outward from you.");
	}

	private int tint(float o) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(o);
			case "Cyber" -> Colors.lerp(0xFFFF2E88, 0xFF00E5FF, o);
			case "Matrix" -> Colors.lerp(0xFF00FF41, 0xFF004F11, o);
			case "White" -> 0xFFFFFFFF;
			default -> Colors.skeet(o);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null) return;
		Matrix4f mat = stack.last().pose();
		double cx = mc.player.getX(), cy = mc.player.getY(), cz = mc.player.getZ();

		float t = System.currentTimeMillis() / 1000f;
		float ext = extent.getF();
		float cs = cell.getF();
		float lw = lineWidth.getF() * 0.5f;
		int op = opacity.getI();
		float boost = scanBoost.getF();
		float scanR = scan.get() && scanSpeed.getF() > 0.01f
				? (t * scanSpeed.getF()) % Math.max(1f, ext) : -1f;

		double y = Math.floor(cy) + yOffset.getF();
		double gx = Math.floor(cx / cs) * cs;
		double gz = Math.floor(cz / cs) * cs;
		int steps = (int) Math.ceil(ext / cs);

		MeshBuilder b = Fx.colorQuads();

		for (int i = -steps; i <= steps; i++) {
			double lx = gx + i * cs;
			for (int j = -steps; j < steps; j++) {
				double z0 = gz + j * cs, z1 = z0 + cs;
				double d = Math.hypot(lx - cx, (z0 + z1) * 0.5 - cz);
				if (d > ext) continue;
				int a = (int) (op * Vfx.fade((float) d, ext));
				if (scanR > 0f && Math.abs(d - scanR) < 1.0) a = (int) Math.min(255, a * boost);
				if (a < 4) continue;
				int c = Colors.alpha(tint((float) (d / ext)), a);
				Vfx.floorQuad(b, mat, (float) (lx - lw), (float) z0, (float) (lx + lw), (float) z1, (float) y, c, c, c, c);
			}
		}
		for (int i = -steps; i <= steps; i++) {
			double lz = gz + i * cs;
			for (int j = -steps; j < steps; j++) {
				double x0 = gx + j * cs, x1 = x0 + cs;
				double d = Math.hypot((x0 + x1) * 0.5 - cx, lz - cz);
				if (d > ext) continue;
				int a = (int) (op * Vfx.fade((float) d, ext));
				if (scanR > 0f && Math.abs(d - scanR) < 1.0) a = (int) Math.min(255, a * boost);
				if (a < 4) continue;
				int c = Colors.alpha(tint((float) (d / ext)), a);
				Vfx.floorQuad(b, mat, (float) x0, (float) (lz - lw), (float) x1, (float) (lz + lw), (float) y, c, c, c, c);
			}
		}

		if (pulseRing.get() && scanR > 0f) {
			float f = 1f - scanR / Math.max(1f, ext);
			Vfx.ringFlat(b, mat, cx, y, cz, scanR - 0.35, scanR + 0.35, 96,
					Colors.alpha(tint(1f - f), (int) (200 * f)), Colors.alpha(tint(1f - f), 0));
			Vfx.ringFlat(b, mat, cx, y, cz, 0.0, 0.5, 48,
					Colors.alpha(tint(0f), (int) (op * 0.7f)), Colors.alpha(tint(0f), 0));
		}
		Fx.draw();
	}
}

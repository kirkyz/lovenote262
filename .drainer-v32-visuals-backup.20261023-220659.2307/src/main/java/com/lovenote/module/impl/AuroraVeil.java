package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Tex;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;

/** Wavy aurora curtains wrapping around you at flying height. */
public class AuroraVeil extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Toxic", "Solar"));
	private final NumberSetting bands = add(new NumberSetting("Bands", 3, 1, 8, 1));
	private final NumberSetting dist = add(new NumberSetting("Distance", 24, 6, 70, 1));
	private final NumberSetting low = add(new NumberSetting("Bottom", 6, -8, 40, 1));
	private final NumberSetting high = add(new NumberSetting("Height", 22, 4, 90, 1));
	private final NumberSetting amp = add(new NumberSetting("Wave", 2.4, 0.0, 12.0, 0.2));
	private final NumberSetting speed = add(new NumberSetting("Speed", 0.5, 0.0, 3.0, 0.05));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 130, 10, 255, 5));
	private final NumberSetting segments = add(new NumberSetting("Segments", 64, 12, 128, 4));

	public AuroraVeil() {
		super("AuroraVeil", Category.VISUALS);
		describe("Undulating aurora curtains wrapping around you.");
	}

	private int tint(float o) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(o);
			case "Ice" -> Colors.lerp(0xFF8AF5D0, 0xFF5FA8FF, o);
			case "Toxic" -> Colors.lerp(0xFF7CFC00, 0xFF00FF9C, o);
			case "Solar" -> Colors.lerp(0xFFFF8C00, 0xFFFF2E88, o);
			default -> Colors.skeet(o);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null) return;
		Matrix4f mat = stack.last().pose();
		double cx = mc.player.getX(), cz = mc.player.getZ();
		double base = mc.player.getY();

		float t = System.currentTimeMillis() / 1000f * speed.getF();
		float R = dist.getF();
		int seg = segments.getI();
		int nb = bands.getI();
		float y0 = (float) base + low.getF();
		float y1 = y0 + high.getF() * 0.5f;
		float y2 = y0 + high.getF();
		float wa = amp.getF();
		int op = opacity.getI();

		Fx.texture(Tex.SOFT);
		MeshBuilder b = Fx.texQuads();

		for (int band = 0; band < nb; band++) {
			float ph = band * 2.1f;
			int col = tint(band / (float) Math.max(1, nb));
			int peak = (int) (op * (1f - band / (float) (nb + 1)));
			if (peak < 4) continue;

			for (int s = 0; s < seg; s++) {
				double a0 = s / (double) seg * Math.PI * 2;
				double a1 = (s + 1) / (double) seg * Math.PI * 2;
				double rr = R + Math.sin(a0 * 3 + t + ph) * wa;

				float x0 = (float) (cx + Math.cos(a0) * rr);
				float z0 = (float) (cz + Math.sin(a0) * rr);
				float x1 = (float) (cx + Math.cos(a1) * rr);
				float z1 = (float) (cz + Math.sin(a1) * rr);

				float u0 = s / (float) seg;
				float u1 = (s + 1) / (float) seg;
				float yo = (float) (Math.sin(a0 * 4 + t * 1.3 + ph) * wa * 1.6);

				float fy0 = y0 + yo, fy1 = y1 + yo * 0.6f, fy2 = y2 + yo * 0.2f;

				Fx.vt(b, mat, x0, fy0, z0, u0, 1f, Colors.alpha(col, 0));
				Fx.vt(b, mat, x1, fy0, z1, u1, 1f, Colors.alpha(col, 0));
				Fx.vt(b, mat, x1, fy1, z1, u1, 0.5f, Colors.alpha(col, peak));
				Fx.vt(b, mat, x0, fy1, z0, u0, 0.5f, Colors.alpha(col, peak));

				Fx.vt(b, mat, x0, fy1, z0, u0, 0.5f, Colors.alpha(col, peak));
				Fx.vt(b, mat, x1, fy1, z1, u1, 0.5f, Colors.alpha(col, peak));
				Fx.vt(b, mat, x1, fy2, z1, u1, 0f, Colors.alpha(col, 0));
				Fx.vt(b, mat, x0, fy2, z0, u0, 0f, Colors.alpha(col, 0));
			}
		}
		Fx.draw();
	}
}

package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.SkyColorMath;
import com.lovenote.util.Fx;
import com.lovenote.util.Tex;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;

/** Wavy aurora curtains wrapping around you at flying height. */
public class AuroraVeil extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Toxic", "Solar", "Custom"));
	private final NumberSetting bands = add(new NumberSetting("Bands", 3, 1, 8, 1));
	private final NumberSetting dist = add(new NumberSetting("Distance", 24, 6, 70, 1));
	private final NumberSetting low = add(new NumberSetting("Bottom", 6, -8, 40, 1));
	private final NumberSetting high = add(new NumberSetting("Height", 22, 4, 90, 1));
	private final NumberSetting amp = add(new NumberSetting("Wave", 2.4, 0.0, 12.0, 0.2));
	private final NumberSetting speed = add(new NumberSetting("Speed", 0.5, 0.0, 3.0, 0.05));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 130, 10, 255, 5));
	private final NumberSetting segments = add(new NumberSetting("Segments", 64, 12, 128, 4));
    private final NumberSetting primaryHue = add(new NumberSetting("Primary hue", 275, 0, 360, 1));
    private final NumberSetting secondaryHue = add(new NumberSetting("Secondary hue", 175, 0, 360, 1));
    private final NumberSetting saturation = add(new NumberSetting("Saturation", 80, 0, 100, 1));
    private final NumberSetting brightness = add(new NumberSetting("Brightness", 94, 0, 100, 1));

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
            case "Custom" -> SkyColorMath.mix(
                SkyColorMath.hsv(primaryHue.getF(),saturation.getF()/100f,brightness.getF()/100f),
                SkyColorMath.hsv(secondaryHue.getF(),saturation.getF()/100f,brightness.getF()/100f),o);
			default -> Colors.skeet(o);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null) return;
		Matrix4f mat = stack.last().pose();
		double cx = mc.player.getX(), cz = mc.player.getZ();
		double base = mc.player.getY();

		float t = (System.nanoTime() % 3_600_000_000_000L) * 1e-9f * speed.getF();
		float R = dist.getF();
		int seg = VisualOptimizer.budget(segments.getI(), 12);
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
				double rr0 = R + Math.sin(a0 * 3 + t + ph) * wa;
                double rr1 = R + Math.sin(a1 * 3 + t + ph) * wa;

                float x0 = (float) (cx + Math.cos(a0) * rr0);
                float z0 = (float) (cz + Math.sin(a0) * rr0);
                float x1 = (float) (cx + Math.cos(a1) * rr1);
                float z1 = (float) (cz + Math.sin(a1) * rr1);

				float u0 = s / (float) seg;
				float u1 = (s + 1) / (float) seg;
				float yo0 = (float) (Math.sin(a0 * 4 + t * 1.3 + ph) * wa * 1.6);
                float yo1 = (float) (Math.sin(a1 * 4 + t * 1.3 + ph) * wa * 1.6);
                float low0 = y0 + yo0, mid0 = y1 + yo0 * .6f, top0 = y2 + yo0 * .2f;
                float low1 = y0 + yo1, mid1 = y1 + yo1 * .6f, top1 = y2 + yo1 * .2f;

                Fx.vt(b, mat, x0, low0, z0, u0, 1f, Colors.alpha(col, 0));
                Fx.vt(b, mat, x1, low1, z1, u1, 1f, Colors.alpha(col, 0));
                Fx.vt(b, mat, x1, mid1, z1, u1, .5f, Colors.alpha(col, peak));
                Fx.vt(b, mat, x0, mid0, z0, u0, .5f, Colors.alpha(col, peak));

                Fx.vt(b, mat, x0, mid0, z0, u0, .5f, Colors.alpha(col, peak));
                Fx.vt(b, mat, x1, mid1, z1, u1, .5f, Colors.alpha(col, peak));
                Fx.vt(b, mat, x1, top1, z1, u1, 0f, Colors.alpha(col, 0));
                Fx.vt(b, mat, x0, top0, z0, u0, 0f, Colors.alpha(col, 0));
			}
		}
		Fx.draw();
	}
}

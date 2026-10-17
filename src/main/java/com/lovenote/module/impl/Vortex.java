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
import org.joml.Matrix4f;

/** A spiralling particle vortex around you. */
public class Vortex extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Fire", "White"));
	private final NumberSetting arms = add(new NumberSetting("Arms", 4, 1, 12, 1));
	private final NumberSetting perArm = add(new NumberSetting("Per arm", 40, 5, 120, 5));
	private final NumberSetting radius = add(new NumberSetting("Radius", 2.2, 0.4, 10.0, 0.1));
	private final NumberSetting rise = add(new NumberSetting("Rise", 3.0, 0.5, 20.0, 0.5));
	private final NumberSetting speed = add(new NumberSetting("Speed", 1.2, 0.0, 5.0, 0.05));
	private final NumberSetting size = add(new NumberSetting("Size", 0.2, 0.03, 0.9, 0.01));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 190, 20, 255, 5));
	private final BoolSetting reverse = add(new BoolSetting("Reverse", false));

	public Vortex() {
		super("Vortex", Category.VISUALS);
		describe("Spiralling particle vortex around you.");
	}

	private int tint(float o) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(o);
			case "Ice" -> Colors.lerp(0xFFE0F7FF, 0xFF7C6CFF, o);
			case "Fire" -> Colors.lerp(0xFFFFF3B0, 0xFFFF3B3B, o);
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
		float dir = reverse.get() ? -1f : 1f;
		float sp = speed.getF() * dir;
		int armsN = arms.getI();
		int per = perArm.getI();
		float rad = radius.getF();
		float rs = rise.getF();
		float sz = size.getF();
		int op = opacity.getI();

		Fx.cameraBasis(ctx.camera());
		Tex.ensure();
		Fx.texture(Tex.STAR);
		MeshBuilder b = Fx.texQuads();

		for (int a = 0; a < armsN; a++) {
			double base = a / (double) armsN * Math.PI * 2;
			for (int i = 0; i < per; i++) {
				float f = i / (float) per;
				double ang = base + f * 6.2832 * 1.6 + t * sp;
				double rr = rad * (0.25 + f * 0.95);
				double x = cx + Math.cos(ang) * rr;
				double z = cz + Math.sin(ang) * rr;
				double y = cy + ((f * rs + t * 0.4 * dir) % rs) - 0.5;
				int al = (int) (op * (1f - f * 0.75f));
				if (al < 4) continue;
				Fx.billboard(b, mat, (float) x, (float) y, (float) z,
						sz * (1.2f - f * 0.7f), Colors.alpha(tint(f), al));
			}
		}
		Fx.draw();
	}
}

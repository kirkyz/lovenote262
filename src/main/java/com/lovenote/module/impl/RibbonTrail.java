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

import java.util.ArrayDeque;

/** A wide glowing ribbon that trails behind you as you move. */
public class RibbonTrail extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Fire", "White"));
	private final NumberSetting length = add(new NumberSetting("Length", 44, 6, 140, 1));
	private final NumberSetting width = add(new NumberSetting("Width", 0.26, 0.04, 1.4, 0.02));
	private final NumberSetting taper = add(new NumberSetting("Taper", 0.75, 0.0, 1.0, 0.05));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 190, 20, 255, 5));
	private final NumberSetting twist = add(new NumberSetting("Twist", 0.0, -3.0, 3.0, 0.05));
	private final NumberSetting minMove = add(new NumberSetting("Min move", 0.03, 0.0, 0.5, 0.01));
	private final BoolSetting second = add(new BoolSetting("Double ribbon", true));
	private final NumberSetting offset = add(new NumberSetting("Offset", 0.5, 0.05, 2.0, 0.05));

	private final ArrayDeque<double[]> pts = new ArrayDeque<>();
	private double lx, ly, lz;

	public RibbonTrail() {
		super("RibbonTrail", Category.VISUALS);
		describe("Wide glowing ribbon trailing behind you.");
	}

	@Override
	public void onDisable() { pts.clear(); }

	@Override
	public void onTick() {
		if (mc.player == null) return;
		double x = mc.player.getX(), y = mc.player.getY() + 1.0, z = mc.player.getZ();
		if (Math.abs(x - lx) + Math.abs(y - ly) + Math.abs(z - lz) < minMove.get()) return;
		lx = x; ly = y; lz = z;
		pts.addLast(new double[]{x, y, z});
		while (pts.size() > length.getI()) pts.removeFirst();
	}

	private int tint(float o) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(o);
			case "Ice" -> Colors.lerp(0xFFB7F0FF, 0xFF7C6CFF, o);
			case "Fire" -> Colors.lerp(0xFFFFD166, 0xFFFF3B3B, o);
			case "White" -> 0xFFFFFFFF;
			default -> Colors.skeet(o);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (pts.size() < 3) return;
		Matrix4f mat = stack.last().pose();
		var cam = ctx.camera().position();

		int n = pts.size();
		float w = width.getF();
		float tp = taper.getF();
		int op = opacity.getI();
		float tw = twist.getF();
		float off = offset.getF();
		float t = System.currentTimeMillis() / 1000f;

		Fx.texture(Tex.SOFT);
		MeshBuilder b = Fx.texQuads();

		double[] prev = null;
		int i = 0;
		for (double[] p : pts) {
			if (prev != null) {
				float f0 = 1f - (i - 1) / (float) n;
				float f1 = 1f - i / (float) n;
				float th0 = w * (1f - tp + tp * f0);
				float th1 = w * (1f - tp + tp * f1);
				int a0 = (int) (op * f0 * f0);
				int a1 = (int) (op * f1 * f1);

				double o0 = Math.sin(t * 3 + i * 0.5) * tw * 0.3;
				double o1 = Math.sin(t * 3 + (i + 1) * 0.5) * tw * 0.3;

				Vfx.ribbon(b, mat, prev[0], prev[1] + o0, prev[2], p[0], p[1] + o1, p[2],
						th0, Colors.alpha(tint(f0), a0), Colors.alpha(tint(f1), a1),
						cam.x, cam.y, cam.z);

				if (second.get()) {
					Vfx.ribbon(b, mat, prev[0], prev[1] - off + o0, prev[2],
							p[0], p[1] - off + o1, p[2],
							th0 * 0.6f, Colors.alpha(tint(f0 + 0.3f), a0 / 2),
							Colors.alpha(tint(f1 + 0.3f), a1 / 2), cam.x, cam.y, cam.z);
				}
			}
			prev = p;
			i++;
		}
		Fx.draw();
	}
}

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

/** A wireframe plasma shell around you. */
public class PlasmaShell extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Toxic", "White"));
	private final NumberSetting radius = add(new NumberSetting("Radius", 1.6, 0.4, 6.0, 0.05));
	private final NumberSetting lat = add(new NumberSetting("Rings", 7, 2, 20, 1));
	private final NumberSetting lon = add(new NumberSetting("Segments", 14, 4, 40, 1));
	private final NumberSetting width = add(new NumberSetting("Line width", 0.022, 0.005, 0.1, 0.002));
	private final NumberSetting spin = add(new NumberSetting("Spin", 0.7, 0.0, 3.0, 0.05));
	private final NumberSetting pulse = add(new NumberSetting("Pulse", 0.15, 0.0, 0.8, 0.01));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 170, 20, 255, 5));
	private final BoolSetting core = add(new BoolSetting("Core", true));

	public PlasmaShell() {
		super("PlasmaShell", Category.VISUALS);
		describe("Wireframe plasma shell humming around you.");
	}

	private int tint(float o) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(o);
			case "Ice" -> Colors.lerp(0xFFD6F3FF, 0xFF6E7BFF, o);
			case "Toxic" -> Colors.lerp(0xFF7CFC00, 0xFF00FF9C, o);
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

		float t = System.currentTimeMillis() / 1000f;
		float r = radius.getF() * (1f + pulse.getF() * (float) Math.sin(t * 2.2));
		int rings = lat.getI();
		int segs = lon.getI();
		float th = width.getF();
		int op = opacity.getI();

		MeshBuilder b = Fx.colorQuads();
		for (int i = 1; i < rings; i++) {
			double phi = i / (double) rings * Math.PI;
			double ry = Math.cos(phi) * r;
			double rr = Math.sin(phi) * r;
			int c = Colors.alpha(tint(i / (float) rings), op);
			for (int s = 0; s < segs; s++) {
				double a0 = s / (double) segs * Math.PI * 2 + t * spin.getF();
				double a1 = (s + 1) / (double) segs * Math.PI * 2 + t * spin.getF();
				double x0 = cx + Math.cos(a0) * rr, z0 = cz + Math.sin(a0) * rr;
				double x1 = cx + Math.cos(a1) * rr, z1 = cz + Math.sin(a1) * rr;
				Vfx.tube(b, mat, x0, cy + ry, z0, x1, cy + ry, z1, th, c, c, cam.x, cam.y, cam.z);
			}
		}
		int lonN = Math.max(3, segs / 3);
		for (int s = 0; s < lonN; s++) {
			double a = s / (double) lonN * Math.PI * 2 + t * spin.getF();
			int c = Colors.alpha(tint(s / (float) lonN), (int) (op * 0.8f));
			for (int i = 0; i < rings; i++) {
				double p0 = i / (double) rings * Math.PI, p1 = (i + 1) / (double) rings * Math.PI;
				Vfx.tube(b, mat,
						cx + Math.cos(a) * Math.sin(p0) * r, cy + Math.cos(p0) * r, cz + Math.sin(a) * Math.sin(p0) * r,
						cx + Math.cos(a) * Math.sin(p1) * r, cy + Math.cos(p1) * r, cz + Math.sin(a) * Math.sin(p1) * r,
						th, c, c, cam.x, cam.y, cam.z);
			}
		}
		Fx.draw();

		if (!core.get()) return;
		Fx.cameraBasis(ctx.camera());
		Tex.ensure();
		Fx.texture(Tex.GLOW);
		MeshBuilder g = Fx.texQuads();
		Fx.billboard(g, mat, (float) cx, (float) cy, (float) cz, r * 0.6f,
				Colors.alpha(tint(0.3f), (int) (op * 0.5f)));
		Fx.draw();
	}
}

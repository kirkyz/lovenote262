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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Matrix4f;

public class GroundFog extends Module {

	private final NumberSetting radius = add(new NumberSetting("Radius", 26, 6, 60, 1));
	private final NumberSetting detail = add(new NumberSetting("Detail", 12, 3, 24, 1));
	private final NumberSetting height = add(new NumberSetting("Height", 0.55, 0.05, 3.0, 0.05));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 110, 10, 255, 5));
	private final NumberSetting speed = add(new NumberSetting("Speed", 0.35, 0.0, 2.0, 0.05));
	private final NumberSetting tile = add(new NumberSetting("Tile", 6.0, 1.0, 16.0, 0.5));
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "White", "Grey"));
	private final BoolSetting wave = add(new BoolSetting("Wave", true));
	private final BoolSetting follow = add(new BoolSetting("Follow terrain", true));

	public GroundFog() {
		super("GroundFog", Category.RENDER);
		describe("Low lying mist rolling across the ground.");
		enabled = true;
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack m) {
		Player p = mc.player;
		if (p == null || mc.level == null) return;
		Tex.ensure();

		Matrix4f mat = m.last().pose();
		float R = radius.getF();
		int N = detail.getI();
		float cell = (R * 2f) / N;
		float td = ctx.tickDelta();
		double px = Mth.lerp(td, p.xo, p.getX());
		double pz = Mth.lerp(td, p.zo, p.getZ());

		float ox = (float) (Math.floor(px / cell) * cell) - R;
		float oz = (float) (Math.floor(pz / cell) * cell) - R;

		float baseY = (float) p.getY() + 0.05f;
		float hOff = height.getF() * 0.25f;
		float time = (System.currentTimeMillis() % 100000L) / 1000f * speed.getF();
		float tl = tile.getF();
		int base = base();
		int op = opacity.getI();
		boolean wv = wave.get();
		boolean followT = follow.get();

		Fx.texture(Tex.SOFT);
		MeshBuilder b = Fx.texQuads();

		for (int gx = 0; gx < N; gx++) {
			for (int gz = 0; gz < N; gz++) {
				float x0 = ox + gx * cell;
				float z0 = oz + gz * cell;
				float x1 = x0 + cell, z1 = z0 + cell;

				float y;
				if (followT) {
					int gh = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING,
							Mth.floor(x0 + cell * 0.5f), Mth.floor(z0 + cell * 0.5f));
					y = Math.max(baseY, gh + 0.06f) + hOff;
				} else {
					y = baseY + hOff;
				}

				float puls = wv ? 0.7f + 0.3f * Mth.sin(time * 2f + (gx + gz) * 0.7f) : 1f;
				float u0 = (x0 / tl) + time * 0.1f, u1 = (x1 / tl) + time * 0.1f;
				float v0 = (z0 / tl) + time * 0.08f, v1 = (z1 / tl) + time * 0.08f;

				int a00 = alphaAt(x0, z0, px, pz, R, op, puls);
				int a10 = alphaAt(x1, z0, px, pz, R, op, puls);
				int a11 = alphaAt(x1, z1, px, pz, R, op, puls);
				int a01 = alphaAt(x0, z1, px, pz, R, op, puls);
				if (a00 + a10 + a11 + a01 < 6) continue;

				Fx.vt(b, mat, x0, y, z0, u0, v1, Colors.alpha(base, a00));
				Fx.vt(b, mat, x1, y, z0, u1, v1, Colors.alpha(base, a10));
				Fx.vt(b, mat, x1, y, z1, u1, v0, Colors.alpha(base, a11));
				Fx.vt(b, mat, x0, y, z1, u0, v0, Colors.alpha(base, a01));
			}
		}
		Fx.draw();
	}

	private static int alphaAt(float x, float z, double cx, double cz, float R, int op, float puls) {
		float dx = x - (float) cx, dz = z - (float) cz;
		float d = (float) Math.sqrt(dx * dx + dz * dz) / R;
		float fade = Mth.clamp(1f - d, 0f, 1f);
		return (int) (op * fade * fade * puls);
	}

	private int base() {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(0f);
			case "White" -> 0xFFFFFFFF;
			case "Grey" -> 0xFFB8B8C4;
			default -> Colors.wave(0f);
		};
	}
}

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

/** Floating additive wireframe cubes tumbling around you. */
public class CubeField extends Module {
	private final ModeSetting mode = add(new ModeSetting("Mode", "Orbit", "Orbit", "Rise", "Helix", "Halo", "Swarm"));
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "White"));
	private final NumberSetting count = add(new NumberSetting("Count", 26, 1, 120, 1));
	private final NumberSetting radius = add(new NumberSetting("Radius", 6.5, 1.0, 24.0, 0.5));
	private final NumberSetting height = add(new NumberSetting("Height", 5.0, 1.0, 20.0, 0.5));
	private final NumberSetting size = add(new NumberSetting("Size", 0.42, 0.06, 1.6, 0.02));
	private final NumberSetting spin = add(new NumberSetting("Spin", 1.0, 0.0, 4.0, 0.05));
	private final NumberSetting speed = add(new NumberSetting("Speed", 0.35, 0.0, 2.0, 0.05));
	private final NumberSetting width = add(new NumberSetting("Line width", 0.035, 0.008, 0.15, 0.004));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 190, 20, 255, 5));
	private final BoolSetting cores = add(new BoolSetting("Glow cores", true));
    private final BoolSetting worldAnchored = add(new BoolSetting("Anchor effect in world", true));
    private final NumberSetting travel = add(new NumberSetting("World drift", 1.2, 0, 5, 0.1));
	private final NumberSetting coreSize = add(new NumberSetting("Core size", 0.35, 0.05, 2.0, 0.05));

	private double[] px = new double[0], py = new double[0], pz = new double[0];
    private double anchorX, anchorY, anchorZ;
    private Object previousLevel;
    private boolean anchorReady;
    @Override public void onEnable() { anchorReady=false; previousLevel=null; }
    @Override public void onDisable() { anchorReady=false; previousLevel=null; }

	public CubeField() {
		super("CubeField", Category.VISUALS);
		describe("Tumbling additive wireframe cubes drifting around you.");
	}

	private int tint(int i, float t) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(i * 0.03f + t * 0.03f);
			case "Ice" -> Colors.lerp(0xFF9BE7FF, 0xFF7C6CFF, Vfx.hash(i * 5));
			case "White" -> 0xFFFFFFFF;
			default -> Colors.wave(i * 0.07f + t * 0.04f);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null) return;
		Matrix4f mat = stack.last().pose();
		var cam = ctx.camera().position();
        double cx = mc.player.getX(), cy = mc.player.getY(), cz = mc.player.getZ();
        if (!anchorReady || previousLevel!=mc.level || Math.hypot(cx-anchorX,cz-anchorZ)>radius.get()*1.2) {
            anchorX=cx;anchorY=cy;anchorZ=cz;previousLevel=mc.level;anchorReady=true;
        }
        if(worldAnchored.get()) {cx=anchorX;cy=anchorY;cz=anchorZ;}

		int n = count.getI();
		if (px.length != n) { px = new double[n]; py = new double[n]; pz = new double[n]; }

		float t = System.currentTimeMillis() / 1000f;
		float rad = radius.getF();
		float hh = height.getF();
		float half = size.getF();
		float th = width.getF();
		int op = opacity.getI();
		float sp = speed.getF();
		float spn = spin.getF();
		String md = mode.get();

        MeshBuilder b = Fx.colorQuadsDepth();
		for (int i = 0; i < n; i++) {
			float h1 = Vfx.hash(i * 3 + 1), h2 = Vfx.hash(i * 3 + 2), h3 = Vfx.hash(i * 3 + 3);
			float h4 = Vfx.hash(i * 7), h5 = Vfx.hash(i * 11);
			double ang = h4 * 6.2832 + t * sp * (0.35 + h5 * 0.9);
			double x = cx, y = cy, z = cz;

			switch (md) {
				case "Rise" -> {
					x = cx + (h1 * 2 - 1) * rad;
					z = cz + (h3 * 2 - 1) * rad;
					y = cy + ((h2 + t * sp * 0.3) % 1.0) * hh * 2 - hh * 0.5;
				}
				case "Helix" -> {
					double r = rad * (0.3 + 0.7 * ((i % 9) / 9.0));
					x = cx + Math.cos(ang * 1.7) * r;
					z = cz + Math.sin(ang * 1.7) * r;
					y = cy + ((i / (double) Math.max(1, n) + t * sp * 0.4) % 1.0) * hh * 1.6;
				}
				case "Halo" -> {
					double r = rad * 0.65;
					x = cx + Math.cos(ang) * r;
					z = cz + Math.sin(ang) * r;
					y = cy + 2.4 + Math.sin(ang * 2 + t) * 0.4;
				}
				case "Swarm" -> {
					double r = rad * (0.15 + 0.85 * h2);
					x = cx + Math.cos(ang) * r + Math.sin(t * 1.3 + h4 * 6) * 0.8;
					z = cz + Math.sin(ang) * r + Math.cos(t * 1.1 + h4 * 6) * 0.8;
					y = cy + 0.4 + h2 * hh * 0.8 + Math.sin(t * 0.9 + h4 * 6) * 0.7;
				}
				default -> {
					double r = rad * (0.35 + 0.65 * h2);
					x = cx + Math.cos(ang) * r;
					z = cz + Math.sin(ang) * r;
					y = cy + 0.6 + h2 * hh + Math.sin(t * 0.8 + h4 * 6) * 0.6;
				}
			}

            // A small independent world-space travel offset prevents a static shell.
            x += (h1*2-1)*travel.get()*Math.sin(t*sp*.55+h4*6);
            z += (h3*2-1)*travel.get()*Math.cos(t*sp*.5+h4*6);
            px[i] = x; py[i] = y; pz[i] = z;
			Vfx.cube(b, mat, new Vfx.P(x, y, z), half * (0.7 + 0.6 * h5),
					ang + t * spn, t * spn * 0.7 + h4 * 3, t * spn * 0.45,
					th, Colors.alpha(tint(i, t), op), cam.x, cam.y, cam.z);
		}
		Fx.draw();

		if (!cores.get()) return;
		Fx.cameraBasis(ctx.camera());
		Tex.ensure();
		Fx.texture(Tex.GLOW);
        MeshBuilder g = Fx.texQuadsDepth();
		float cs = coreSize.getF();
		for (int i = 0; i < n; i++) {
			int a = (int) (op * 0.85f * (0.55f + 0.45f * Vfx.hash(i * 13)));
			if (a < 4) continue;
			Fx.billboard(g, mat, (float) px[i], (float) py[i], (float) pz[i], cs,
					Colors.alpha(tint(i, t), a));
		}
		Fx.draw();
	}
}

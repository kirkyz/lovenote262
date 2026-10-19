package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Vfx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;

/** Prism shards drifting down around you. */
public class PrismRain extends Module {
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Prism", "White"));
	private final NumberSetting count = add(new NumberSetting("Count", 30, 2, 120, 1));
	private final NumberSetting spread = add(new NumberSetting("Spread", 10, 2, 30, 0.5));
	private final NumberSetting height = add(new NumberSetting("Height", 12, 3, 40, 1));
	private final NumberSetting speed = add(new NumberSetting("Fall speed", 1.6, 0.1, 8.0, 0.1));
	private final NumberSetting size = add(new NumberSetting("Size", 0.14, 0.03, 0.6, 0.01));
	private final NumberSetting len = add(new NumberSetting("Length", 0.5, 0.1, 2.0, 0.05));
	private final NumberSetting spin = add(new NumberSetting("Spin", 1.2, 0.0, 5.0, 0.05));
	private final NumberSetting width = add(new NumberSetting("Line width", 0.02, 0.004, 0.09, 0.002));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 170, 20, 255, 5));
    private double ax,ay,az;
    private Object previousLevel;
    private boolean anchored;
    @Override public void onEnable() {anchored=false;previousLevel=null;}
    @Override public void onDisable() {anchored=false;previousLevel=null;}

	public PrismRain() {
		super("PrismRain", Category.VISUALS);
		describe("Prism shards drifting down around you.");
	}

	private int tint(float o) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(o);
			case "Ice" -> Colors.lerp(0xFFE0F7FF, 0xFF7C6CFF, o);
			case "Prism" -> Colors.hsv(o * 3f, 0.55f, 1f);
			case "White" -> 0xFFFFFFFF;
			default -> Colors.skeet(o);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null) return;
		Matrix4f mat = stack.last().pose();
		var cam = ctx.camera().position();
        double cx=mc.player.getX(),cy=mc.player.getY(),cz=mc.player.getZ();
        if(!anchored || previousLevel!=mc.level || Math.hypot(cx-ax,cz-az)>spread.get()*.9){
            ax=cx;ay=cy;az=cz;previousLevel=mc.level;anchored=true;
        }
        cx=ax;cy=ay;cz=az;

		float t = System.currentTimeMillis() / 1000f;
		int n = count.getI();
		float sp = spread.getF();
		float hh = height.getF();
		float spd = speed.getF();
		float r = size.getF();
		float h = len.getF();
		float th = width.getF();
		int op = opacity.getI();

        MeshBuilder b = Fx.colorQuadsDepth();
		for (int i = 0; i < n; i++) {
			float h1 = Vfx.hash(i * 5 + 1), h2 = Vfx.hash(i * 5 + 2), h3 = Vfx.hash(i * 5 + 3);
			double x = cx + (h1 * 2 - 1) * sp;
			double z = cz + (h3 * 2 - 1) * sp;
			double y = cy + hh - ((h2 * hh + t * spd) % hh);
			int a = (int) (op * (0.45f + 0.55f * h3));
			if (a < 4) continue;
			double ang = t * spin.getF() * (0.5 + h2) + h1 * 6.28;
			int c = Colors.alpha(tint(h1), a);
			Vfx.crystal(b, mat, new Vfx.P(x, y, z), r, h, ang, ang * 0.6, ang * 0.4, th,
					c, Colors.alpha(tint(h1), a / 2), cam.x, cam.y, cam.z);
		}
		Fx.draw();
	}
}

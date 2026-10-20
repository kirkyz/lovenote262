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
	private final ModeSetting positionMode = add(new ModeSetting("Effect position", "World anchor", "World anchor", "Around player", "Camera follow"));
    private final NumberSetting forwardDistance = add(new NumberSetting("Forward distance", 7.5, 3, 22, .5));
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Toxic", "White"));
	private final NumberSetting radius = add(new NumberSetting("Radius", 1.6, 0.4, 6.0, 0.05));
	private final NumberSetting lat = add(new NumberSetting("Rings", 7, 2, 20, 1));
	private final NumberSetting lon = add(new NumberSetting("Segments", 14, 4, 40, 1));
	private final NumberSetting width = add(new NumberSetting("Line width", 0.022, 0.005, 0.1, 0.002));
	private final NumberSetting spin = add(new NumberSetting("Spin", 0.7, 0.0, 3.0, 0.05));
	private final NumberSetting wave = add(new NumberSetting("Energy motion", 0.09, 0, 0.40, 0.01));
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

    // World anchor is acquired once, not recomputed from the camera every frame.
    // Changing world/server or toggling the module resets the anchor.
    private boolean anchorReady;
    private double anchorX, anchorY, anchorZ;
    private net.minecraft.client.multiplayer.ClientLevel anchorLevel;
    @Override public void onEnable() { anchorReady=false; anchorLevel=null; }
    @Override public void onDisable() { anchorReady=false; anchorLevel=null; }
    private void positionAt(double[] position, WorldRenderContext ctx) {
        var cam=ctx.camera().position();
        if (positionMode.is("Around player")) {
            position[0]=mc.player.getX(); position[1]=mc.player.getY()+1.15; position[2]=mc.player.getZ();
            return;
        }
        if (!anchorReady || anchorLevel != mc.level || positionMode.is("Camera follow")) {
            double yaw=Math.toRadians(mc.player.getYRot());
            double pitch=Math.toRadians(mc.player.getXRot());
            double horizontal=Math.cos(pitch)*forwardDistance.get();
            double ax=cam.x-Math.sin(yaw)*horizontal;
            double ay=cam.y-Math.sin(pitch)*forwardDistance.get();
            double az=cam.z+Math.cos(yaw)*horizontal;
            if(positionMode.is("Camera follow")) {position[0]=ax;position[1]=ay;position[2]=az;return;}
            anchorX=ax;anchorY=ay;anchorZ=az;
            anchorLevel=mc.level; anchorReady=true;
        }
        position[0]=anchorX;position[1]=anchorY;position[2]=anchorZ;
    }

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null) return;
		Matrix4f mat = stack.last().pose();
		var cam = ctx.camera().position();
        double[] pos=new double[3];
        positionAt(pos,ctx);
        double cx=pos[0],cy=pos[1],cz=pos[2];

		float t = System.nanoTime() / 1_000_000_000f;
		float r = radius.getF() * (1f + pulse.getF() * (float) Math.sin(t * 2.2));
		int rings = VisualOptimizer.budget(lat.getI(), 2);
		int segs = VisualOptimizer.budget(lon.getI(), 4);
		float th = width.getF();
		int op = opacity.getI();

		MeshBuilder b = Fx.colorQuads();
		for (int i = 1; i < rings; i++) {
			double phi = i / (double) rings * Math.PI;
			double ry = Math.cos(phi) * r + Math.sin(t * 1.7 + phi * 5) * wave.getF();
			double rr = Math.sin(phi) * r * (1 + Math.sin(t * 1.9 + phi * 6) * wave.getF());
			int c = Colors.alpha(tint(i / (float) rings), (int)(op * (0.78f + 0.22f * Math.sin(t*2 + i*0.6))));
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

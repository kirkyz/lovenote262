package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.SkyColorMath;
import com.lovenote.util.Fx;
import com.lovenote.util.Gl;
import com.lovenote.util.Tex;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.Random;

/** Extra celestial layers that sit on top of whatever SkyShader is doing. */
public class SkyExtras extends Module {
	private final BoolSetting planet = add(new BoolSetting("Ringed planet", true));
	private final NumberSetting planetSize = add(new NumberSetting("Planet size", 30, 6, 120, 1));
    private final NumberSetting ringWidth = add(new NumberSetting("Planet ring width", 7, 1, 22, 1));
    private final NumberSetting ringTilt = add(new NumberSetting("Planet ring tilt", .34, .08, .85, .02));
	private final BoolSetting twinSuns = add(new BoolSetting("Twin suns", false));
	private final BoolSetting rainbow = add(new BoolSetting("Rainbow arc", true));
	private final NumberSetting rainbowAlpha = add(new NumberSetting("Rainbow opacity", 70, 0, 255, 5));
	private final BoolSetting bolts = add(new BoolSetting("Sky lightning", true));
	private final NumberSetting boltRate = add(new NumberSetting("Lightning rate", 0.4, 0.0, 3.0, 0.05));
	private final BoolSetting bands = add(new BoolSetting("Cloud bands", true));
	private final NumberSetting bandAlpha = add(new NumberSetting("Band opacity", 40, 0, 200, 5));
	private final BoolSetting overworldOnly = add(new BoolSetting("Overworld only", true));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 210, 20, 255, 5));
    private final ModeSetting palette = add(new ModeSetting("Sky extras colours", "Theme", "Theme", "Custom", "Rainbow"));
    private final NumberSetting hueA = add(new NumberSetting("Primary hue", 264, 0, 360, 1));
    private final NumberSetting hueB = add(new NumberSetting("Secondary hue", 322, 0, 360, 1));
    private final NumberSetting saturation = add(new NumberSetting("Saturation", 80, 0, 100, 1));
    private final NumberSetting brightness = add(new NumberSetting("Brightness", 95, 0, 100, 1));
    private final NumberSetting motion = add(new NumberSetting("Motion speed", 1, 0, 3, 0.05));

	private static final float R = 300f;
	private final Random rnd = new Random();
	private long lastBolt;
	private float boltX, boltY, boltZ, boltLife;
	private float[] bandSeed;
    private final float[] boltOffsets = new float[12 * 2];
    private long previousFrameNanos;

    private int tint(float phase) {
        if (palette.is("Rainbow")) return Colors.rainbow(phase);
        if (palette.is("Theme")) return Colors.skeet(phase);
        return SkyColorMath.mix(
           SkyColorMath.hsv(hueA.getF(),saturation.getF()/100f,brightness.getF()/100f),
           SkyColorMath.hsv(hueB.getF(),saturation.getF()/100f,brightness.getF()/100f),phase);
    }

	public SkyExtras() {
		super("SkyExtras", Category.VISUALS);
		describe("Ringed planet, twin suns, rainbow arc and sky lightning.");
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null || mc.level == null) return;
		if (overworldOnly.get() && !mc.level.dimension().identifier().getPath().equals("overworld")) return;

		Matrix4f mat = stack.last().pose();
		var cam = ctx.camera().position();
		double cx = cam.x, cy = cam.y, cz = cam.z;
		long nano = System.nanoTime();
        float dt = previousFrameNanos == 0 ? 0f : Math.min(0.1f, Math.max(0f,(nano - previousFrameNanos)*1e-9f));
        previousFrameNanos = nano;
        float t = ((nano % 3_600_000_000_000L)*1e-9f) * motion.getF();
		float td = ctx.tickDelta();
		float skyAng = Gl.skyAngle(mc.level, td) * 6.28318f;
		int op = opacity.getI();

		Fx.cameraBasis(ctx.camera());
		Tex.ensure();

		if (planet.get()) {
			float pAng = t * 0.03f;
			float px = (float) (cx + Mth.cos(pAng) * R * 0.8f);
			float pz = (float) (cz + Mth.sin(pAng) * R * 0.8f);
			float py = (float) cy + 150f;
			int col = Colors.alpha(tint(0.5f), op);
			Fx.texture(Tex.GLOW);
			MeshBuilder b = Fx.texQuads();
			Fx.billboard(b, mat, px, py, pz, planetSize.getF() * 1.5f, Colors.alpha(col, op / 3));
			Fx.billboard(b, mat, px, py, pz, planetSize.getF(), col);
			Fx.draw();
            // The old "ring" was three glowing squares. This is an actual angled annulus.
            MeshBuilder rb = Fx.colorQuads();
            float inner = planetSize.getF() * .73f;
            float outer = inner + ringWidth.getF();
            float tilt = ringTilt.getF();
            for (int i=0;i<48;i++) {
                float a0=(float)(i*2*Math.PI/48), a1=(float)((i+1)*2*Math.PI/48);
                float c0=Mth.cos(a0), c1=Mth.cos(a1), s0=Mth.sin(a0), s1=Mth.sin(a1);
                int tint0=Colors.alpha(tint(i/48f),Math.round(op*.7f));
                int tint1=Colors.alpha(tint(i/48f),Math.round(op*.4f));
                Fx.vc(rb,mat,px+Fx.RIGHT.x*c0*inner+Fx.UP.x*s0*inner*tilt,
                    py+Fx.RIGHT.y*c0*inner+Fx.UP.y*s0*inner*tilt,
                    pz+Fx.RIGHT.z*c0*inner+Fx.UP.z*s0*inner*tilt,tint0);
                Fx.vc(rb,mat,px+Fx.RIGHT.x*c1*inner+Fx.UP.x*s1*inner*tilt,
                    py+Fx.RIGHT.y*c1*inner+Fx.UP.y*s1*inner*tilt,
                    pz+Fx.RIGHT.z*c1*inner+Fx.UP.z*s1*inner*tilt,tint0);
                Fx.vc(rb,mat,px+Fx.RIGHT.x*c1*outer+Fx.UP.x*s1*outer*tilt,
                    py+Fx.RIGHT.y*c1*outer+Fx.UP.y*s1*outer*tilt,
                    pz+Fx.RIGHT.z*c1*outer+Fx.UP.z*s1*outer*tilt,tint1);
                Fx.vc(rb,mat,px+Fx.RIGHT.x*c0*outer+Fx.UP.x*s0*outer*tilt,
                    py+Fx.RIGHT.y*c0*outer+Fx.UP.y*s0*outer*tilt,
                    pz+Fx.RIGHT.z*c0*outer+Fx.UP.z*s0*outer*tilt,tint1);
            }
            Fx.draw();
		}

		if (twinSuns.get()) {
			Fx.texture(Tex.GLOW);
			MeshBuilder b = Fx.texQuads();
			for (int k = 0; k < 2; k++) {
				float a2 = skyAng + k * 0.35f;
				float sx = (float) (cx + Mth.cos(a2) * R);
				float sy = (float) (cy + Mth.sin(a2) * R);
				int col = Colors.alpha(tint(k * 0.7f), op);
				Fx.billboard(b, mat, sx, sy, (float) cz, 60f, Colors.alpha(col, op / 2));
				Fx.billboard(b, mat, sx, sy, (float) cz, 30f, col);
			}
			Fx.draw();
		}

		if (rainbow.get()) {
			int a = rainbowAlpha.getI();
			Fx.texture(Tex.SOFT);
			MeshBuilder b = Fx.texQuads();
			int bandsN = 7;
			for (int i = 0; i < bandsN; i++) {
				float rr = R * 0.55f + i * 4f;
				int col = Colors.hsv(i / (float) bandsN, 0.75f, 1f);
				for (int s = 0; s < 36; s++) {
					double a0 = Math.PI + s / 36.0 * Math.PI;
					double a1 = Math.PI + (s + 1) / 36.0 * Math.PI;
					float x0 = (float) (cx + Math.cos(a0) * rr);
					float y0 = (float) (cy + Math.sin(a0) * rr * 0.5f);
					float x1 = (float) (cx + Math.cos(a1) * rr);
					float y1 = (float) (cy + Math.sin(a1) * rr * 0.5f);
					float w = 3.2f;
					Fx.vt(b, mat, x0, y0, (float) cz - R * 0.2f, 0f, 0f, Colors.alpha(col, a / 2));
					Fx.vt(b, mat, x1, y1, (float) cz - R * 0.2f, 1f, 0f, Colors.alpha(col, a / 2));
					Fx.vt(b, mat, x1, y1 + w, (float) cz - R * 0.2f, 1f, 1f, Colors.alpha(col, a));
					Fx.vt(b, mat, x0, y0 + w, (float) cz - R * 0.2f, 0f, 1f, Colors.alpha(col, a));
				}
			}
			Fx.draw();
		}

		if (bands.get()) {
			if (bandSeed == null) {
				bandSeed = new float[14 * 3];
				for (int i = 0; i < 14; i++) {
					bandSeed[i * 3] = rnd.nextFloat();
					bandSeed[i * 3 + 1] = rnd.nextFloat();
					bandSeed[i * 3 + 2] = 0.5f + rnd.nextFloat();
				}
			}
			int a = bandAlpha.getI();
			Fx.texture(Tex.SOFT);
			MeshBuilder b = Fx.texQuads();
			for (int i = 0; i < 14; i++) {
				float ang = bandSeed[i * 3] * 6.2832f + t * 0.02f * bandSeed[i * 3 + 2];
				float rad = R * (0.5f + bandSeed[i * 3 + 1] * 0.5f);
				float y = (float) cy + 90f + bandSeed[i * 3 + 1] * 90f;
				float x = (float) (cx + Math.cos(ang) * rad);
				float z = (float) (cz + Math.sin(ang) * rad);
				int col = Colors.alpha(SkyColorMath.mix(tint(i / 14f), 0xFFFFFFFF, 0.25f), a);
				Fx.billboard(b, mat, x, y, z, 140f + bandSeed[i * 3 + 2] * 120f, col);
			}
			Fx.draw();
		}

		if (bolts.get()) {
			long now = System.currentTimeMillis();
			if (boltLife <= 0f && rnd.nextFloat() < Math.min(1f,boltRate.getF() * dt * 0.5f)) {
				float a2 = rnd.nextFloat() * 6.2832f;
				boltX = (float) (cx + Math.cos(a2) * R * 0.7f);
				boltZ = (float) (cz + Math.sin(a2) * R * 0.7f);
				boltY = (float) cy + 120f + rnd.nextFloat() * 60f;
				boltLife = 0.55f;
                lastBolt = now;
                for (int k=0;k<12;k++) { boltOffsets[k*2]=(rnd.nextFloat()-0.5f)*26f; boltOffsets[k*2+1]=(rnd.nextFloat()-0.5f)*26f; }
			}
			if (boltLife > 0f) {
				boltLife -= (now - lastBolt) / 1000f;
				lastBolt = now;
				float f = Mth.clamp(boltLife / 0.55f, 0f, 1f);
				int a = (int) (255 * f);
				if (a > 4) {
					Fx.texture(Tex.SOFT);
					MeshBuilder b = Fx.texQuads();
					Fx.billboard(b, mat, boltX, boltY, boltZ, 220f, Colors.alpha(tint(.2f), a / 4));
					Fx.billboard(b, mat, boltX, boltY, boltZ, 90f, Colors.alpha(tint(.8f), a));
					float y = boltY;
					float x = boltX, z = boltZ;
					for (int i = 0; i < 12; i++) {
						x += boltOffsets[i*2];
                        z += boltOffsets[i*2+1];
						y -= 22f;
						Fx.billboard(b, mat, x, y, z, 26f, Colors.alpha(tint(.4f), a / 2));
					}
					Fx.draw();
				}
			}
		}
	}
}

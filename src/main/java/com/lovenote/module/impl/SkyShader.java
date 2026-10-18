package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Gl;
import com.lovenote.util.Tex;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import net.minecraft.client.Camera;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SkyShader extends Module {

	private final ModeSetting preset = add(new ModeSetting("Preset", "Lovenote",
			"Off", "Lovenote", "Sunset", "Dawn", "Night", "Aurora", "Cyber", "Void",
			"Nebula", "Vaporwave", "Matrix", "BloodMoon", "Ice", "Candy", "Mono", "Custom",
			"Galaxy", "Aurora Borealis", "Meteor Shower", "Eclipse", "Twilight", "Toxic",
			"Rose Gold", "Deep Sea", "Ember", "Glacier", "Solar Storm", "Midnight Bloom",
			"Cotton Candy", "Obsidian", "Rainbow", "Comet", "Pastel Dream", "Storm",
			"Astral", "Golden Hour", "Lavender", "Cherry"));
	private final BoolSetting overworldOnly = add(new BoolSetting("Overworld only", true));
	private final NumberSetting starBrightness = add(new NumberSetting("Stars", 0.7, 0.0, 1.0, 0.05));
	private final NumberSetting twinkle = add(new NumberSetting("Twinkle", 0.4, 0.0, 1.0, 0.05));
	private final NumberSetting spin = add(new NumberSetting("Sky spin", 1.0, 0.0, 6.0, 0.1));
	private final NumberSetting auroraStrength = add(new NumberSetting("Aurora", 0.0, 0.0, 1.0, 0.05));
	private final NumberSetting auroraSpeed = add(new NumberSetting("Aurora speed", 0.5, 0.0, 3.0, 0.05));
	private final BoolSetting celestial = add(new BoolSetting("Sun & moon", true));
	private final NumberSetting sunSize = add(new NumberSetting("Sun size", 1.0, 0.3, 3.0, 0.1));
	private final BoolSetting sunFlare = add(new BoolSetting("Sun flare", true));
	private final NumberSetting milkyWay = add(new NumberSetting("Milky way", 0.0, 0.0, 1.0, 0.05));
	private final NumberSetting constellations = add(new NumberSetting("Constellations", 0.0, 0.0, 1.0, 0.05));
	private final NumberSetting planets = add(new NumberSetting("Planets", 0, 0, 4, 1));
	private final NumberSetting meteors = add(new NumberSetting("Shooting stars", 0.3, 0.0, 1.0, 0.05));
	private final NumberSetting haze = add(new NumberSetting("Horizon haze", 0.35, 0.0, 1.0, 0.05));
	private final NumberSetting posterize = add(new NumberSetting("Banding", 0, 0, 24, 1));
	private final BoolSetting cloudTintOn = add(new BoolSetting("Tint clouds", true));
	private final BoolSetting fogTintOn = add(new BoolSetting("Tint fog", true));
	private final NumberSetting domeDetail = add(new NumberSetting("Dome detail", 48, 16, 96, 8));

	private static final float R = 260f;
	private static final Identifier SUN = Identifier.withDefaultNamespace("textures/environment/sun.png");
	private static final Identifier MOON = Identifier.withDefaultNamespace("textures/environment/moon_phases.png");

	private float[] stars;
	private int starCount = -1;
	private long lastFrame = System.currentTimeMillis();

	private static final class Meteor { long t0; float ax, ay, az, dx, dy, dz; float len; }
	private final List<Meteor> meteorsList = new ArrayList<>();
	private final Random rng = new Random();

	public SkyShader() {
		super("SkyShader", Category.RENDER);
		describe("Custom sky, sun, stars, aurora and nebula.");
	        enabled = true;
}

	private int top() {
		return switch (preset.get()) {
			case "Lovenote" -> Colors.C3;
			case "Sunset" -> 0xFF2B1B4D;
			case "Dawn" -> 0xFF2A4A8A;
			case "Night" -> 0xFF05060F;
			case "Aurora" -> 0xFF071026;
			case "Aurora Borealis" -> 0xFF02121C;
			case "Cyber" -> 0xFF12002B;
			case "Void" -> 0xFF000000;
			case "Nebula" -> 0xFF1B0B3B;
			case "Vaporwave" -> 0xFF2A0A4A;
			case "Matrix" -> 0xFF001A0A;
			case "BloodMoon" -> 0xFF2A0505;
			case "Ice" -> 0xFF0B2A3A;
			case "Candy" -> 0xFF3A0B2E;
			case "Mono" -> 0xFF101014;
			case "Galaxy" -> 0xFF0B0420;
			case "Meteor Shower" -> 0xFF060612;
			case "Eclipse" -> 0xFF000000;
			case "Twilight" -> 0xFF2B1055;
			case "Toxic" -> 0xFF04200E;
			case "Rose Gold" -> 0xFF3B1F2B;
			case "Deep Sea" -> 0xFF001427;
			case "Ember" -> 0xFF1A0500;
			case "Glacier" -> 0xFF0A1A2F;
			case "Solar Storm" -> 0xFF1A0033;
			case "Midnight Bloom" -> 0xFF0A0118;
			case "Cotton Candy" -> 0xFF3D2C4E;
			case "Obsidian" -> 0xFF08080C;
			case "Rainbow" -> 0xFF14002B;
			case "Comet" -> 0xFF03030A;
			case "Pastel Dream" -> 0xFF2E2A4A;
			case "Storm" -> 0xFF10131C;
			case "Astral" -> 0xFF0D0526;
			case "Golden Hour" -> 0xFF2A1B0A;
			case "Lavender" -> 0xFF241B4A;
			case "Cherry" -> 0xFF2B0A14;
			case "Custom" -> 0xFF000000 | (Colors.C3 & 0xFFFFFF);
			default -> 0xFF4A90D9;
		};
	}

	private int horizon() {
		return switch (preset.get()) {
			case "Lovenote" -> Colors.C1;
			case "Sunset" -> 0xFFFF7A45;
			case "Dawn" -> 0xFFFFC48A;
			case "Night" -> 0xFF141A33;
			case "Aurora" -> 0xFF0E2A2A;
			case "Aurora Borealis" -> 0xFF3BF0A0;
			case "Cyber" -> 0xFFFF2E88;
			case "Void" -> 0xFF0A0A12;
			case "Nebula" -> 0xFF6D28D9;
			case "Vaporwave" -> 0xFFFF2E88;
			case "Matrix" -> 0xFF00FF41;
			case "BloodMoon" -> 0xFFDC143C;
			case "Ice" -> 0xFF67E8F9;
			case "Candy" -> 0xFFFF9ACB;
			case "Mono" -> 0xFFDDDDDD;
			case "Galaxy" -> 0xFF7C3AED;
			case "Meteor Shower" -> 0xFFFF6B35;
			case "Eclipse" -> 0xFFFFB347;
			case "Twilight" -> 0xFFFF9ACB;
			case "Toxic" -> 0xFF7CFC00;
			case "Rose Gold" -> 0xFFFFB6A8;
			case "Deep Sea" -> 0xFF1ABC9C;
			case "Ember" -> 0xFFFF4500;
			case "Glacier" -> 0xFFA5F3FC;
			case "Solar Storm" -> 0xFFFF8C00;
			case "Midnight Bloom" -> 0xFFB28DFF;
			case "Cotton Candy" -> 0xFFFFC0E0;
			case "Obsidian" -> 0xFF4B5563;
			case "Rainbow" -> 0xFFFF2E88;
			case "Comet" -> 0xFF67E8F9;
			case "Pastel Dream" -> 0xFFFFD1DC;
			case "Storm" -> 0xFF8899AA;
			case "Astral" -> 0xFF8B5CF6;
			case "Golden Hour" -> 0xFFFFD166;
			case "Lavender" -> 0xFFC4B5FD;
			case "Cherry" -> 0xFFFF9ACB;
			case "Custom" -> 0xFF000000 | (Colors.C1 & 0xFFFFFF);
			default -> 0xFFB7D9F2;
		};
	}

	private boolean starsOn() {
		return starBrightness.getF() > 0.01f && switch (preset.get()) {
			case "Lovenote", "Night", "Void", "Aurora", "Nebula", "Vaporwave", "Matrix", "BloodMoon", "Ice",
					"Candy", "Mono", "Custom", "Galaxy", "Meteor Shower", "Eclipse", "Twilight",
					"Deep Sea", "Solar Storm", "Midnight Bloom", "Obsidian", "Comet", "Astral",
					"Lavender", "Cherry", "Aurora Borealis" -> true;
			default -> false;
		};
	}

	private boolean auroraOn() {
		return auroraStrength.getF() > 0.01f && switch (preset.get()) {
			case "Aurora", "Cyber", "Nebula", "Vaporwave", "Matrix", "Custom",
					"Aurora Borealis", "Galaxy", "Astral", "Solar Storm", "Toxic" -> true;
			default -> false;
		};
	}

	        public boolean active() {
                return enabled && !preset.is("Off");
        }

@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack m) {
                if (preset.is("Off")) return;
                if (overworldOnly.get() && mc.level != null
                                && mc.level.dimensionType().skybox() != DimensionType.Skybox.OVERWORLD) return;

                long now = System.currentTimeMillis();
                float dt = Mth.clamp((now - lastFrame) / 1000f, 0f, 0.2f);
                lastFrame = now;

                Camera camera = ctx.camera();
                Matrix4f mat = m.last().pose();
                double camX = camera.position().x, camY = camera.position().y, camZ = camera.position().z;

                Fx.cameraBasis(camera);
                Tex.ensure();

                dome(mat, camX, camY, camZ);
                if (starsOn()) stars(mat, camX, camY, camZ, ctx.tickDelta());
                if (milkyWay.getF() > 0.01f) milkyWayBand(mat, camX, camY, camZ);
                if (constellations.getF() > 0.01f) constellationLines(mat, camX, camY, camZ);
                if (planets.getI() > 0) planetBodies(mat, camX, camY, camZ);
                if (auroraOn()) aurora(mat, camX, camY, camZ, ctx.tickDelta());
                if (celestial.get()) sunMoon(mat, camX, camY, camZ, ctx.tickDelta());
                if (meteors.getF() > 0.01f) shootingStars(mat, camX, camY, camZ, dt);
                if (haze.getF() > 0.01f) horizonHaze(mat, camX, camY, camZ);
        }

	private int shade(float y, int zenith, int horizon, int nadir) {
		float t = Mth.clamp(y / R, -1f, 1f);
		int c = t >= 0f ? Colors.lerp(horizon, zenith, t) : Colors.lerp(horizon, nadir, -t);
		int steps = posterize.getI();
		if (steps > 1) {
			int q = 255 / steps;
			int r = Math.min(255, ((c >> 16) & 255) / Math.max(1, q) * q);
			int g = Math.min(255, ((c >> 8) & 255) / Math.max(1, q) * q);
			int b = Math.min(255, (c & 255) / Math.max(1, q) * q);
			c = (c & 0xFF000000) | (r << 16) | (g << 8) | b;
		}
		return c;
	}

	private void dome(Matrix4f mat, double cx, double cy, double cz) {
		int zenith = top(), horizon = horizon();
		int nadir = Colors.lerp(horizon, 0xFF000000, 0.45f);

		int seg = domeDetail.getI();
		int rings = Math.max(6, seg / 3);

		MeshBuilder b = Fx.colorQuadsDepth();
		for (int r = 0; r < rings; r++) {
			float phi0 = (float) (r * Math.PI / rings);
			float phi1 = (float) ((r + 1) * Math.PI / rings);
			float y0 = (float) Math.cos(phi0) * R;
			float y1 = (float) Math.cos(phi1) * R;
			float rad0 = (float) Math.sin(phi0) * R;
			float rad1 = (float) Math.sin(phi1) * R;
			int c0 = shade(y0, zenith, horizon, nadir);
			int c1 = shade(y1, zenith, horizon, nadir);

			for (int s = 0; s < seg; s++) {
				float a0 = (float) (s * Math.PI * 2.0 / seg);
				float a1 = (float) ((s + 1) * Math.PI * 2.0 / seg);
				float x00 = Mth.cos(a0) * rad0, z00 = Mth.sin(a0) * rad0;
				float x01 = Mth.cos(a1) * rad0, z01 = Mth.sin(a1) * rad0;
				float x10 = Mth.cos(a0) * rad1, z10 = Mth.sin(a0) * rad1;
				float x11 = Mth.cos(a1) * rad1, z11 = Mth.sin(a1) * rad1;

				Fx.vc(b, mat, (float) cx + x00, (float) cy + y0, (float) cz + z00, c0);
				Fx.vc(b, mat, (float) cx + x01, (float) cy + y0, (float) cz + z01, c0);
				Fx.vc(b, mat, (float) cx + x11, (float) cy + y1, (float) cz + z11, c1);
				Fx.vc(b, mat, (float) cx + x10, (float) cy + y1, (float) cz + z10, c1);
			}
		}
		Fx.draw();
	}

	private float[] starField() {
		int n = 620;
		if (starCount != n) {
			starCount = n;
			stars = new float[n * 3];
			Random r = new Random(0x5EED);
			for (int i = 0; i < n; i++) {
				float y = r.nextFloat();
				float a = r.nextFloat() * 6.28318f;
				float rad = (float) Math.sqrt(Math.max(0f, 1f - y * y));
				stars[i * 3] = Mth.cos(a) * rad * R;
				stars[i * 3 + 1] = y * R;
				stars[i * 3 + 2] = Mth.sin(a) * rad * R;
			}
		}
		return stars;
	}

	private void stars(Matrix4f mat, double cx, double cy, double cz, float td) {
		float[] st = starField();
		float angle = (mc.level == null ? 0f : Gl.skyAngle(mc.level, td)) * 6.28318f * spin.getF();
		float cs = Mth.cos(angle), sn = Mth.sin(angle);
		float bright = starBrightness.getF();
		float tw = twinkle.getF();

		Fx.texture(Tex.STAR);
		MeshBuilder b = Fx.texQuadsDepth();

		long t = System.currentTimeMillis();
		for (int i = 0; i < starCount; i++) {
			float x = st[i * 3], y = st[i * 3 + 1], z = st[i * 3 + 2];
			float rx = x * cs - z * sn;
			float rz = x * sn + z * cs;
			float f = 1f;
			if (tw > 0f) f = 1f - tw * 0.5f * (0.5f + 0.5f * Mth.sin(t / 900f + i));
			int a = (int) (255 * bright * f);
			if (a < 4) continue;
			Fx.billboard(b, mat, (float) cx + rx, (float) cy + y, (float) cz + rz, 2.8f, Colors.alpha(0xFFFFFFFF, a));
		}
		Fx.draw();
	}

	private void milkyWayBand(Matrix4f mat, double cx, double cy, double cz) {
		float[] st = starField();
		int op = (int) (110 * milkyWay.getF());
		Fx.texture(Tex.SOFT);
		MeshBuilder b = Fx.texQuadsDepth();
		float tilt = 0.6f;
		for (int i = 0; i < 240; i++) {
			float x = st[i * 3], y = st[i * 3 + 1], z = st[i * 3 + 2];
			float rx = x * Mth.cos(tilt) - y * Mth.sin(tilt);
			float ry = x * Mth.sin(tilt) + y * Mth.cos(tilt);
			int a = (int) (op * (0.4f + 0.6f * Math.abs(Mth.sin(i * 0.7f))));
			Fx.billboard(b, mat, (float) cx + rx, (float) cy + ry, (float) cz + z, 26f, Colors.alpha(0xFFE8E0FF, a));
		}
		Fx.draw();
	}

	private void constellationLines(Matrix4f mat, double cx, double cy, double cz) {
		float[] st = starField();
		MeshBuilder b = Fx.colorQuadsDepth();
		int op = (int) (170 * constellations.getF());
		Random r = new Random(0xC0FFEE);
		for (int i = 0; i < 60; i++) {
			int a0 = r.nextInt(starCount);
			int a1 = r.nextInt(starCount);
			float x0 = st[a0 * 3], y0 = st[a0 * 3 + 1], z0 = st[a0 * 3 + 2];
			float x1 = st[a1 * 3], y1 = st[a1 * 3 + 1], z1 = st[a1 * 3 + 2];
			float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
			float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (len > 90f) continue;
			float th = 0.9f;
			int c = Colors.alpha(0xFFFFFFFF, op);
			Fx.vc(b, mat, (float) cx + x0 - th, (float) cy + y0, (float) cz + z0, c);
			Fx.vc(b, mat, (float) cx + x0 + th, (float) cy + y0, (float) cz + z0, c);
			Fx.vc(b, mat, (float) cx + x1 + th, (float) cy + y1, (float) cz + z1, c);
			Fx.vc(b, mat, (float) cx + x1 - th, (float) cy + y1, (float) cz + z1, c);
		}
		Fx.draw();
	}

	private void planetBodies(Matrix4f mat, double cx, double cy, double cz) {
		Fx.texture(Tex.GLOW);
		MeshBuilder b = Fx.texQuadsDepth();
		int n = planets.getI();
		for (int i = 0; i < n; i++) {
			float ang = (float) (i * 1.9);
			float px = Mth.cos(ang) * R * 0.72f;
			float pz = Mth.sin(ang) * R * 0.72f;
			float py = R * (0.18f + i * 0.12f);
			int c = Colors.alpha(Colors.lerp(horizon(), 0xFFFFFFFF, 0.4f), 210);
			Fx.billboard(b, mat, (float) cx + px, (float) cy + py, (float) cz + pz, 12f + i * 3f, c);
		}
		Fx.draw();
	}

	private void aurora(Matrix4f mat, double cx, double cy, double cz, float td) {
		float str = auroraStrength.getF();
		float time = (System.currentTimeMillis() % 100000L) / 1000f * auroraSpeed.getF();

		Fx.texture(Tex.SOFT);
		MeshBuilder b = Fx.texQuadsDepth();
		for (int band = 0; band < 3; band++) {
			float phase = time + band * 2.1f;
			int col = Colors.rainbow(band * 0.22f + time * 0.02f);
			int a = (int) (150 * str);
			if (a < 4) continue;

			float zOff = Mth.sin(phase) * R * 0.55f;
			float xOff = Mth.cos(phase * 0.7f) * R * 0.55f;
			float y0 = 40f + band * 18f, y1 = y0 + 120f;
			float w = R * 1.15f;

			Fx.vt(b, mat, (float) cx - w + xOff, (float) cy + y0, (float) cz + zOff, 0f, 1f, Colors.alpha(col, 0));
			Fx.vt(b, mat, (float) cx + w + xOff, (float) cy + y0, (float) cz + zOff, 1f, 1f, Colors.alpha(col, 0));
			Fx.vt(b, mat, (float) cx + w + xOff, (float) cy + y1, (float) cz + zOff, 1f, 0f, Colors.alpha(col, a));
			Fx.vt(b, mat, (float) cx - w + xOff, (float) cy + y1, (float) cz + zOff, 0f, 0f, Colors.alpha(col, a));
		}
		Fx.draw();
	}

	private void sunMoon(Matrix4f mat, double cx, double cy, double cz, float td) {
		float ang = (mc.level == null ? 0f : Gl.skyAngle(mc.level, td)) * 6.28318f;
		float sx = Mth.cos(ang) * R, sy = Mth.sin(ang) * R;
		boolean day = sy > -20f;
		float size = 34f * sunSize.getF();

		if (sunFlare.get() && day) {
			Fx.texture(Tex.GLOW);
			MeshBuilder g = Fx.texQuadsDepth();
			Fx.billboard(g, mat, (float) cx + sx, (float) cy + sy, (float) cz, size * 4.5f, Colors.alpha(horizon(), 90));
			Fx.draw();
		}

		Fx.texture(day ? SUN : MOON);
		MeshBuilder b = Fx.texQuadsDepth();
		int c = day ? 0xFFFFFFFF : 0xFFD8E4FF;
		Fx.billboard(b, mat, (float) cx + sx, (float) cy + sy, (float) cz, size, c);
		Fx.draw();
	}

	private void shootingStars(Matrix4f mat, double cx, double cy, double cz, float dt) {
		float intensity = meteors.getF();
		if (meteorsList.size() < (int) (intensity * 12)) {
			if (rng.nextFloat() < intensity * 0.35f) {
				Meteor mm = new Meteor();
				mm.t0 = System.currentTimeMillis();
				float a = rng.nextFloat() * 6.28318f;
				float e = 0.2f + rng.nextFloat() * 0.7f;
				mm.ax = Mth.cos(a) * R * 0.9f;
				mm.az = Mth.sin(a) * R * 0.9f;
				mm.ay = e * R * 0.9f;
				float dir = rng.nextFloat() * 6.28318f;
				mm.dx = Mth.cos(dir) * 0.5f;
				mm.dz = Mth.sin(dir) * 0.5f;
				mm.dy = -0.35f - rng.nextFloat() * 0.3f;
				mm.len = 60f + rng.nextFloat() * 90f;
				meteorsList.add(mm);
			}
		}

		long now = System.currentTimeMillis();
		Fx.texture(Tex.SOFT);
		MeshBuilder b = Fx.texQuadsDepth();
		for (int i = meteorsList.size() - 1; i >= 0; i--) {
			Meteor mm = meteorsList.get(i);
			float age = (now - mm.t0) / 1000f;
			if (age > 2.2f) { meteorsList.remove(i); continue; }
			float f = 1f - age / 2.2f;
			float px = mm.ax + mm.dx * mm.len * age;
			float py = mm.ay + mm.dy * mm.len * age;
			float pz = mm.az + mm.dz * mm.len * age;
			int a = (int) (230 * f * intensity);
			if (a < 6) continue;
			Fx.billboard(b, mat, (float) cx + px, (float) cy + py, (float) cz + pz, 5f, Colors.alpha(0xFFFFFFFF, a));
			Fx.billboard(b, mat, (float) cx + px - mm.dx * 14f, (float) cy + py - mm.dy * 14f,
					(float) cz + pz - mm.dz * 14f, 3f, Colors.alpha(0xFFFFFFFF, a / 2));
		}
		Fx.draw();
	}

	private void horizonHaze(Matrix4f mat, double cx, double cy, double cz) {
		int a = (int) (120 * haze.getF());
		if (a < 4) return;
		int col = horizon();
		Fx.texture(Tex.SOFT);
		MeshBuilder b = Fx.texQuadsDepth();
		float w = R * 1.6f;
		Fx.vt(b, mat, (float) cx - w, (float) cy - 6f, (float) cz - w, 0f, 0f, Colors.alpha(col, 0));
		Fx.vt(b, mat, (float) cx + w, (float) cy - 6f, (float) cz - w, 1f, 0f, Colors.alpha(col, 0));
		Fx.vt(b, mat, (float) cx + w, (float) cy + 70f, (float) cz + w, 1f, 1f, Colors.alpha(col, a));
		Fx.vt(b, mat, (float) cx - w, (float) cy + 70f, (float) cz + w, 0f, 1f, Colors.alpha(col, a));
		Fx.draw();
	}
}

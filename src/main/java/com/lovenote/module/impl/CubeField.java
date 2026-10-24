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
	private final ModeSetting geometry = add(new ModeSetting("Geometry", "Holographic", "Wireframe", "Holographic", "Crystal Glass", "Solid Neon"));
	private final NumberSetting faceOpacity = add(new NumberSetting("Face opacity", 45, 0, 190, 5));
	private final NumberSetting glowStrength = add(new NumberSetting("Glow strength", 0.8, 0.0, 2.0, 0.1));
	private final ModeSetting mode = add(new ModeSetting("Mode", "Wave", "Orbit", "Rise", "Helix", "Halo", "Swarm", "Tornado", "Tunnel", "Wave", "Fountain"));
	private final ModeSetting colour = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "White", "Amethyst", "Crimson", "Mint"));
	private final NumberSetting count = add(new NumberSetting("Count", 18, 1, 120, 1));
	private final NumberSetting radius = add(new NumberSetting("Radius", 6.5, 1.0, 24.0, 0.5));
	private final NumberSetting height = add(new NumberSetting("Height", 5.0, 1.0, 20.0, 0.5));
	private final NumberSetting size = add(new NumberSetting("Size", 0.29, 0.06, 1.6, 0.02));
	private final NumberSetting spin = add(new NumberSetting("Spin", 1.0, 0.0, 4.0, 0.05));
	private final NumberSetting speed = add(new NumberSetting("Speed", 0.65, 0.0, 2.0, 0.05));
	private final NumberSetting width = add(new NumberSetting("Line width", 0.035, 0.008, 0.15, 0.004));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", 190, 20, 255, 5));
	private final BoolSetting doubleFrames = add(new BoolSetting("Inner geometry", false));
	private final NumberSetting pulse = add(new NumberSetting("Size pulse", 0.24, 0.0, 0.8, 0.04));
	private final NumberSetting rotationVariety = add(new NumberSetting("Rotation variation", 0.6, 0.0, 2.0, 0.1));
	private final BoolSetting cores = add(new BoolSetting("Glow cores", false));
    private final BoolSetting worldAnchored = add(new BoolSetting("Anchor effect in world", true));
    private final NumberSetting travel = add(new NumberSetting("World drift", 0.5, 0, 5, 0.1));
	private final BoolSetting animate = add(new BoolSetting("Living animation", true));
    private final NumberSetting drawDistance = add(new NumberSetting("Draw distance", 28, 8, 96, 2));
    private final BoolSetting reduceWhenGui = add(new BoolSetting("Hide in menus", true));
    private final NumberSetting coreSize = add(new NumberSetting("Core size", 0.35, 0.05, 2.0, 0.05));

	private double[] px = new double[0], py = new double[0], pz = new double[0];
    private double anchorX, anchorY, anchorZ;
    private Object previousLevel;
    private boolean anchorReady;
    private long animationStartNanos;
    @Override public void onEnable() { anchorReady=false; previousLevel=null; animationStartNanos=System.nanoTime(); }
    @Override public void onDisable() { anchorReady=false; previousLevel=null; animationStartNanos=System.nanoTime(); }

	public CubeField() {
		super("CubeField", Category.VISUALS);
        animationStartNanos = System.nanoTime();
		describe("Animated 3D cubes with filled holographic faces, individually rotating frames and glow.");
	}

	private int tint(int i, float t) {
		return switch (colour.get()) {
			case "Rainbow" -> Colors.rainbow(i * 0.03f + t * 0.03f);
			case "Ice" -> Colors.lerp(0xFF9BE7FF, 0xFF7C6CFF, Vfx.hash(i * 5));
			case "White" -> 0xFFFFFFFF;
			case "Amethyst" -> Colors.lerp(0xFF9054F7, 0xFFE5A9FF, Vfx.hash(i * 5));
			case "Crimson" -> Colors.lerp(0xFFFF2555, 0xFFFFA04A, Vfx.hash(i * 5));
			case "Mint" -> Colors.lerp(0xFF20E8B0, 0xFF78E8FF, Vfx.hash(i * 5));
			default -> Colors.wave(i * 0.07f + t * 0.04f);
		};
	}

	@Override
	public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
		if (mc.player == null || mc.level == null) return;
        if (reduceWhenGui.get() && mc.gui.screen() != null) return;
		Matrix4f mat = stack.last().pose();
		var cam = ctx.camera().position();
        double cx = mc.player.getX(), cy = mc.player.getY(), cz = mc.player.getZ();
        if (!anchorReady || previousLevel!=mc.level) {
            anchorX=cx;anchorY=cy;anchorZ=cz;previousLevel=mc.level;anchorReady=true;
        }
        if(worldAnchored.get()) {cx=anchorX;cy=anchorY;cz=anchorZ;}

		int n = VisualOptimizer.budget(count.getI(), 4);
		if (px.length != n) { px = new double[n]; py = new double[n]; pz = new double[n]; }

		float t = (float)((System.nanoTime() - animationStartNanos) * 1.0e-9);
		float rad = radius.getF();
		float hh = height.getF();
		float half = size.getF();
		float th = width.getF();
		int op = opacity.getI();
		float sp = speed.getF();
		float spn = spin.getF();
		String md = mode.get();

        MeshBuilder b = Fx.colorQuadsDepth();
        float maxDistanceSq = drawDistance.getF() * drawDistance.getF();
        boolean live = animate.get();
        int visibleCount = 0;
		for (int i = 0; i < n; i++) {
			float h1 = Vfx.hash(i * 3 + 1), h2 = Vfx.hash(i * 3 + 2), h3 = Vfx.hash(i * 3 + 3);
			float h4 = Vfx.hash(i * 7), h5 = Vfx.hash(i * 11);
			double ang = h4 * 6.2832 + t * sp * (0.35 + h5 * 0.9);
			double x = cx, y = cy, z = cz;

			switch (md) {
				case "Tornado" -> {
                    double progress = (h2 + t * sp * 0.13) % 1.0;
                    double r = rad * (0.16 + progress * 0.86);
                    double orbit = ang * 3.0 + progress * 9.0;
                    x = cx + Math.cos(orbit) * r;
                    z = cz + Math.sin(orbit) * r;
                    y = cy + progress * hh * 1.6;
                }
                case "Tunnel" -> {
                    double depth = (h2 + t * sp * 0.16) % 1.0;
                    double orbit = h4 * 6.28318 + t * sp * 0.25;
                    x = cx + Math.cos(orbit) * rad * (0.2 + depth * 0.8);
                    z = cz + Math.sin(orbit) * rad * (0.2 + depth * 0.8);
                    y = cy + 1.4 + (depth - 0.5) * hh * 1.4;
                }
                case "Wave" -> {
                    x = cx + (h1 * 2 - 1) * rad;
                    z = cz + (h3 * 2 - 1) * rad;
                    y = cy + 1.0 + hh * 0.4 + Math.sin(t * sp * 2.0 + h1 * 9 + h3 * 7) * hh * 0.35;
                }
                case "Fountain" -> {
                    double age = (h2 + t * sp * 0.14) % 1.0;
                    double reach = Math.sin(age * Math.PI) * rad;
                    x = cx + Math.cos(ang) * reach;
                    z = cz + Math.sin(ang) * reach;
                    y = cy + age * hh * 1.5;
                }
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
            double dx = x - cam.x, dy = y - cam.y, dz = z - cam.z;
            if (dx*dx + dy*dy + dz*dz > maxDistanceSq) { px[i] = Double.NaN; continue; }
            // Different animation phase for each cube; fades out and in at staggered times.
            double life = (t * Math.max(0.05, sp) * 0.18 + h1) % 1.0;
            double fade = live ? Math.min(1.0, Math.min(life, 1.0-life)*6.0) : 1.0;
            double sway = live ? Math.sin(t*(0.5+h5)+h4*6.28318)*0.22 : 0;
            y += sway;
            px[i] = x; py[i] = y; pz[i] = z;
            visibleCount++;
			double dynamicHalf = half * (0.75 + 0.4 * h5) * (1.0 + (live ? pulse.getF() : 0) * Math.sin(t * (0.8 + h5 * 1.3) + i * 0.73)) * (0.8 + 0.2*fade);
            double yaw = ang + t * spn * (0.5 + h1 * rotationVariety.getF());
            double pitch = t * spn * (0.35 + h3 * rotationVariety.getF()) + h4 * 3;
            double roll = t * spn * (0.3 + h2 * rotationVariety.getF());
            int tint = Colors.alpha(tint(i, t), (int)(op * fade));
            if (!"Wireframe".equals(geometry.get()) && faceOpacity.getI() > 0) {
                int faceAlpha = switch (geometry.get()) {
                    case "Crystal Glass" -> Math.max(1, faceOpacity.getI() / 2);
                    case "Solid Neon" -> Math.min(230, faceOpacity.getI() * 2);
                    default -> faceOpacity.getI();
                };
                faceAlpha = (int)(faceAlpha * fade);
                Vfx.cubeFaces(b, mat, new Vfx.P(x, y, z), dynamicHalf,
                        yaw, pitch, roll, tint(i, t), faceAlpha);
            }
            Vfx.cube(b, mat, new Vfx.P(x, y, z), dynamicHalf,
                    yaw, pitch, roll, th, tint, cam.x, cam.y, cam.z);
            if (doubleFrames.get()) {
                Vfx.cube(b, mat, new Vfx.P(x, y, z), dynamicHalf * 0.52,
                        -yaw * 0.7, pitch * 1.2, -roll,
                        th * 0.56f, Colors.alpha(tint(i, t), (int)(op * fade * 0.5)), cam.x, cam.y, cam.z);
            }
		}
		Fx.draw();

		if (!cores.get() || glowStrength.getF() <= 0.01f || visibleCount == 0) return;
		Fx.cameraBasis(ctx.camera());
		Tex.ensure();
		Fx.texture(Tex.GLOW);
        MeshBuilder g = Fx.texQuadsDepth();
		float cs = coreSize.getF() * glowStrength.getF();
		for (int i = 0; i < n; i++) {
			if (Double.isNaN(px[i])) continue;
            int a = (int) (op * 0.65f * (0.55f + 0.45f * Vfx.hash(i * 13)));
			if (a < 4) continue;
			Fx.billboard(g, mat, (float) px[i], (float) py[i], (float) pz[i], cs,
					Colors.alpha(tint(i, t), a));
		}
		Fx.draw();
	}
}

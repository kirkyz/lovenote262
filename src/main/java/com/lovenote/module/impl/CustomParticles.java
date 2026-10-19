package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Tex;
import com.lovenote.util.Vfx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Matrix4f;

/** World-space animated particles. Every particle has its own origin, lifetime and velocity. */
public final class CustomParticles extends Module {
    private final ModeSetting palette = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Pink", "White"));
    private final ModeSetting shape = add(new ModeSetting("Shape", "Cube", "Soft", "Spark", "Cube", "Crystal"));
    private final ModeSetting movement = add(new ModeSetting("Movement", "Drift", "Drift", "Fall", "Rise", "Swirl", "Scatter"));
    private final NumberSetting count = add(new NumberSetting("Particle count", 32, 5, 125, 1));
    private final NumberSetting range = add(new NumberSetting("Range", 10, 2, 24, 1));
    private final NumberSetting size = add(new NumberSetting("Size", 0.16, 0.04, 1, 0.02));
    private final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0, 4, 0.1));
    private final NumberSetting airHeight = add(new NumberSetting("Height over terrain", 2.1, 0.5, 5.0, 0.1));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 120, 5, 255, 5));
    private final NumberSetting lifetime = add(new NumberSetting("Lifetime seconds", 8.5, 1.0, 30, 0.5));
    private final NumberSetting drift = add(new NumberSetting("Drift distance", 4.0, 0, 14, 0.2));
    private final NumberSetting rotation = add(new NumberSetting("Rotation speed", 0.6, 0, 5, 0.1));
    private final NumberSetting turbulence = add(new NumberSetting("Turbulence", 0.35, 0, 2, 0.05));
    private final NumberSetting fadeSeconds = add(new NumberSetting("Fade seconds", 1.5, 0.1, 6.0, 0.1));
    private final NumberSetting spread = add(new NumberSetting("Spawn spread", 100, 10, 100, 5));
    private final Particle[] particles = new Particle[125];
    private Object lastLevel;
    private double lastTime;
    private int serial;

    private static final class Particle {
        double x, y, z, vx, vy, vz, born, life, phase;
        int spawnGround, cellX=Integer.MIN_VALUE, cellZ=Integer.MIN_VALUE, cellGround;
        float seed, scale;
    }

    public CustomParticles() {
        super("CustomParticles", Category.VISUALS);
        describe("World-space animated cubes, crystals and sparks with independent motion and rotation.");
    }

    @Override public void onEnable() { reset(); }
    @Override public void onDisable() { reset(); }
    private void reset() {
        java.util.Arrays.fill(particles, null);
        lastLevel = null; lastTime = 0; serial = 0;
    }

    private int tint(float f) {
        return switch (palette.get()) {
            case "Rainbow" -> Colors.rainbow(f);
            case "Ice" -> 0xFF93E6FF;
            case "Pink" -> 0xFFFF7AD9;
            case "White" -> 0xFFFFFFFF;
            default -> Colors.wave(f);
        };
    }

    private static double fract(double x) { return x - Math.floor(x); }
    private static double random(int i, int salt) { return Vfx.hash(i * 31 + salt * 1301); }

    private void respawn(int slot, double now, double cx, double cz) {
        int id = ++serial;
        Particle a = new Particle();
        double angle = random(id, 9) * Math.PI * 2;
        double distance = Math.sqrt(random(id, 10)) * range.get() * (spread.get() / 100.0);
        a.x = cx + Math.cos(angle) * distance;
        a.z = cz + Math.sin(angle) * distance;
        int ground = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int)Math.floor(a.x), (int)Math.floor(a.z));
        a.spawnGround = ground;
        a.y = ground + .2 + random(id, 11) * airHeight.get();
        a.seed = (float)random(id, 5);
        a.scale = (float)(.62 + random(id, 6) * .78);
        double dir = random(id, 7) * 2 * Math.PI;
        double s = .15 + random(id, 8) * .85;
        a.vx = Math.cos(dir) * s;
        a.vz = Math.sin(dir) * s;
        a.vy = random(id, 12) * .3;
        a.born = now - random(id, 4) * .5;
        a.life = lifetime.get() * (.8 + random(id, 3) * .4);
        a.phase = random(id, 13) * Math.PI * 2;
        particles[slot] = a;
    }

    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if (mc.player == null || mc.level == null || speed.get() == 0) return;
        if (lastLevel != mc.level) { reset(); lastLevel = mc.level; }
        double now = System.nanoTime() / 1_000_000_000.0;
        if (lastTime == 0) lastTime = now;
        lastTime = now;
        double cx = mc.player.getX(), cz = mc.player.getZ();
        int n = VisualOptimizer.budget(count.getI(), 5);
        boolean solid = shape.is("Cube") || shape.is("Crystal");
        Matrix4f mat = stack.last().pose();
        Fx.cameraBasis(ctx.camera()); Tex.ensure();
        MeshBuilder b;
        if (solid) b = Fx.colorQuadsDepth();
        else b = Fx.texQuadsDepth(shape.is("Spark") ? Tex.STAR : Tex.SOFT);
        double velocity = speed.get();
        for (int i = 0; i < n; i++) {
            Particle a = particles[i];
            if (a == null || now - a.born >= a.life || Math.hypot(a.x-cx,a.z-cz) > range.get()*1.6) {
                respawn(i, now, cx, cz);
                a = particles[i];
            }
            double age = Math.max(0, now - a.born), progress = Math.min(1, age/a.life);
            double elapsed = age * velocity, distance = drift.get() * elapsed / Math.max(.2,a.life);
            double x = a.x + a.vx * distance, z = a.z + a.vz * distance;
            // Gentle long-period wind: no per-frame position teleportation.
            x += Math.sin(elapsed * .55 + a.phase) * turbulence.get() * .35;
            z += Math.cos(elapsed * .48 + a.phase) * turbulence.get() * .35;
            if (movement.is("Swirl")) {
                x += Math.cos(age*velocity*1.65 + a.phase) * .7;
                z += Math.sin(age*velocity*1.65 + a.phase) * .7;
            } else if (movement.is("Scatter")) {
                x += a.vx * distance * .85;
                z += a.vz * distance * .85;
            }
            // Re-sample terrain at the particle's current X/Z. No cliff-top levitation.
            int blockX=(int)Math.floor(x),blockZ=(int)Math.floor(z);
            if (blockX!=a.cellX || blockZ!=a.cellZ) {
                a.cellGround=mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING,blockX,blockZ);
                a.cellX=blockX;a.cellZ=blockZ;
            }
            int ground=a.cellGround;
            double height=Math.min(airHeight.get(),Math.max(.25,a.y-a.spawnGround));
            double y;
            switch (movement.get()) {
                case "Rise" -> y = ground + .16 + height * progress;
                case "Fall" -> y = ground + .16 + height * (1-progress);
                case "Swirl" -> y = ground + .25 + height * .5 + Math.sin(age*velocity*1.8+a.phase)*.3;
                default -> y = ground + .18 + height*.45 + Math.sin(age*velocity*.85+a.phase)*.2;
            }
            double separation = Math.hypot(x-cx,z-cz);
            if (separation > range.get()) continue;
            double in = Math.min(1, age / Math.max(.1,fadeSeconds.get()));
            double out = Math.min(1, Math.max(0,a.life-age) / Math.max(.1,fadeSeconds.get()));
            // Smoothstep alpha eliminates the sudden spawn/pop and fast disappearance.
            in = in*in*(3-2*in);
            out = out*out*(3-2*out);
            double envelope = Math.min(in,out);
            envelope *= Math.min(1,(range.get()-separation)/Math.max(.5,range.get()*.25));
            int alpha = (int)(opacity.getI()*Math.max(0,envelope));
            if (alpha < 4) continue;
            int colour = Colors.alpha(tint(a.seed),alpha);
            float sz = size.getF()*a.scale;
            if (shape.is("Cube")) {
                double rot = age*rotation.get()*1.7+a.phase;
                Vfx.cube(b,mat,new Vfx.P(x,y,z),sz,rot,rot*.57,rot*.26,sz*.07f,
                    colour,ctx.camera().position().x,ctx.camera().position().y,ctx.camera().position().z);
            } else if(shape.is("Crystal")) {
                double rot = age*rotation.get()*1.25+a.phase;
                Vfx.crystal(b,mat,new Vfx.P(x,y,z),sz*.7,sz,rot,rot*.4,rot*.27,sz*.07f,
                    colour,colour,ctx.camera().position().x,ctx.camera().position().y,ctx.camera().position().z);
            } else {
                Fx.billboard(b,mat,(float)x,(float)y,(float)z,sz,colour);
            }
        }
        Fx.draw();
    }
}

package com.lovenote.util;

import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;

/** Shared math + vertex emitters for the visual pack. Absolute world space. */
public final class Vfx {
	private Vfx() {}

	public static float hash(int n) {
		int h = n * 0x9E3779B9;
		h ^= h >>> 16;
		h *= 0x85EBCA6B;
		h ^= h >>> 13;
		h *= 0xC2B2AE35;
		h ^= h >>> 16;
		return (h & 0xFFFFFF) / (float) 0xFFFFFF;
	}

	public static float hash(int a, int b) { return hash((a * 73856093) ^ (b * 19349663)); }

	public static float fade(float d, float range) {
		float t = 1f - d / Math.max(0.0001f, range);
		if (t <= 0f) return 0f;
		return t * t;
	}

	public static final class P {
		public final double x, y, z;
		public P(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
	}

	public static P rotY(P p, double a) {
		double c = Math.cos(a), s = Math.sin(a);
		return new P(p.x * c - p.z * s, p.y, p.x * s + p.z * c);
	}

	public static P rotX(P p, double a) {
		double c = Math.cos(a), s = Math.sin(a);
		return new P(p.x, p.y * c - p.z * s, p.y * s + p.z * c);
	}

	public static P rotZ(P p, double a) {
		double c = Math.cos(a), s = Math.sin(a);
		return new P(p.x * c - p.y * s, p.x * s + p.y * c, p.z);
	}

	public static P rot(P p, double yaw, double pitch, double roll) {
		return rotZ(rotX(rotY(p, yaw), pitch), roll);
	}

	public static void tube(MeshBuilder b, Matrix4f m,
							double ax, double ay, double az,
							double bx, double by, double bz,
							float th, int ca, int cb,
							double camX, double camY, double camZ) {
		double dx = bx - ax, dy = by - ay, dz = bz - az;
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1e-6) return;
		dx /= len; dy /= len; dz /= len;

		double mx = (ax + bx) * 0.5 - camX;
		double my = (ay + by) * 0.5 - camY;
		double mz = (az + bz) * 0.5 - camZ;

		double sx = dy * mz - dz * my;
		double sy = dz * mx - dx * mz;
		double sz = dx * my - dy * mx;
		double sl = Math.sqrt(sx * sx + sy * sy + sz * sz);
		if (sl < 1e-6) { sx = 1; sy = 0; sz = 0; sl = 1; }
		sx = sx / sl * th; sy = sy / sl * th; sz = sz / sl * th;

		Fx.vc(b, m, (float) (ax - sx), (float) (ay - sy), (float) (az - sz), ca);
		Fx.vc(b, m, (float) (ax + sx), (float) (ay + sy), (float) (az + sz), ca);
		Fx.vc(b, m, (float) (bx + sx), (float) (by + sy), (float) (bz + sz), cb);
		Fx.vc(b, m, (float) (bx - sx), (float) (by - sy), (float) (bz - sz), cb);
	}

	public static void ribbon(MeshBuilder b, Matrix4f m,
							  double ax, double ay, double az,
							  double bx, double by, double bz,
							  float th, int ca, int cb,
							  double camX, double camY, double camZ) {
		double dx = bx - ax, dy = by - ay, dz = bz - az;
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1e-6) return;
		dx /= len; dy /= len; dz /= len;

		double mx = (ax + bx) * 0.5 - camX;
		double my = (ay + by) * 0.5 - camY;
		double mz = (az + bz) * 0.5 - camZ;

		double sx = dy * mz - dz * my;
		double sy = dz * mx - dx * mz;
		double sz = dx * my - dy * mx;
		double sl = Math.sqrt(sx * sx + sy * sy + sz * sz);
		if (sl < 1e-6) { sx = 1; sy = 0; sz = 0; sl = 1; }
		sx = sx / sl * th; sy = sy / sl * th; sz = sz / sl * th;

		Fx.vt(b, m, (float) (ax - sx), (float) (ay - sy), (float) (az - sz), 0f, 1f, ca);
		Fx.vt(b, m, (float) (ax + sx), (float) (ay + sy), (float) (az + sz), 1f, 1f, ca);
		Fx.vt(b, m, (float) (bx + sx), (float) (by + sy), (float) (bz + sz), 1f, 0f, cb);
		Fx.vt(b, m, (float) (bx - sx), (float) (by - sy), (float) (bz - sz), 0f, 0f, cb);
	}

	public static void floorQuad(MeshBuilder b, Matrix4f m,
								 double x0, double z0, double x1, double z1, double y,
								 int c0, int c1, int c2, int c3) {
		Fx.vc(b, m, (float) x0, (float) y, (float) z0, c0);
		Fx.vc(b, m, (float) x1, (float) y, (float) z0, c1);
		Fx.vc(b, m, (float) x1, (float) y, (float) z1, c2);
		Fx.vc(b, m, (float) x0, (float) y, (float) z1, c3);
	}

	public static void ringFlat(MeshBuilder b, Matrix4f m,
								double cx, double cy, double cz,
								double r0, double r1, int seg, int cin, int cout) {
		for (int s = 0; s < seg; s++) {
			double a0 = s * Math.PI * 2 / seg, a1 = (s + 1) * Math.PI * 2 / seg;
			double c0 = Math.cos(a0), s0 = Math.sin(a0);
			double c1 = Math.cos(a1), s1 = Math.sin(a1);
			Fx.vc(b, m, (float) (cx + c0 * r0), (float) cy, (float) (cz + s0 * r0), cin);
			Fx.vc(b, m, (float) (cx + c1 * r0), (float) cy, (float) (cz + s1 * r0), cin);
			Fx.vc(b, m, (float) (cx + c1 * r1), (float) cy, (float) (cz + s1 * r1), cout);
			Fx.vc(b, m, (float) (cx + c0 * r1), (float) cy, (float) (cz + s0 * r1), cout);
		}
	}

	public static void cube(MeshBuilder b, Matrix4f m, P c, double half,
							double yaw, double pitch, double roll, float th, int col,
							double camX, double camY, double camZ) {
		P[] v = new P[8];
		int i = 0;
		for (int xi = -1; xi <= 1; xi += 2)
			for (int yi = -1; yi <= 1; yi += 2)
				for (int zi = -1; zi <= 1; zi += 2) {
					P p = rot(new P(xi * half, yi * half, zi * half), yaw, pitch, roll);
					v[i++] = new P(c.x + p.x, c.y + p.y, c.z + p.z);
				}
		int[][] e = {
				{0, 1}, {2, 3}, {4, 5}, {6, 7},
				{0, 2}, {1, 3}, {4, 6}, {5, 7},
				{0, 4}, {1, 5}, {2, 6}, {3, 7}
		};
		for (int[] ed : e) {
			P a = v[ed[0]], d = v[ed[1]];
			tube(b, m, a.x, a.y, a.z, d.x, d.y, d.z, th, col, col, camX, camY, camZ);
		}
	}

    /** Filled rotating cube with subtle per-face lighting. Uses the existing 26.2 colour-quad pipeline. */
    public static void cubeFaces(MeshBuilder b, Matrix4f m, P c, double half,
                                 double yaw, double pitch, double roll, int color, int alpha) {
        P[] v = new P[8];
        int i = 0;
        for (int xi = -1; xi <= 1; xi += 2)
            for (int yi = -1; yi <= 1; yi += 2)
                for (int zi = -1; zi <= 1; zi += 2) {
                    P p = rot(new P(xi * half, yi * half, zi * half), yaw, pitch, roll);
                    v[i++] = new P(c.x + p.x, c.y + p.y, c.z + p.z);
                }
        int[][] faces = {{0,2,3,1},{4,5,7,6},{0,1,5,4},
                         {2,6,7,3},{0,4,6,2},{1,3,7,5}};
        float[] light = {0.62f,0.86f,0.76f,1.0f,0.68f,0.92f};
        for (int f = 0; f < faces.length; f++) {
            int r = Math.min(255, Math.round(((color >>> 16) & 255) * light[f]));
            int g = Math.min(255, Math.round(((color >>> 8) & 255) * light[f]));
            int blue = Math.min(255, Math.round((color & 255) * light[f]));
            int shaded = (Math.max(0, Math.min(255, alpha)) << 24) | (r << 16) | (g << 8) | blue;
            for (int vertex : faces[f]) {
                P p = v[vertex];
                Fx.vc(b, m, (float)p.x, (float)p.y, (float)p.z, shaded);
            }
        }
    }

	public static void crystal(MeshBuilder b, Matrix4f m, P c, double r, double h,
							   double yaw, double pitch, double roll, float th,
							   int top, int bottom,
							   double camX, double camY, double camZ) {
		P t = rot(new P(0, h, 0), yaw, pitch, roll);
		P bt = rot(new P(0, -h * 0.75, 0), yaw, pitch, roll);
		double tx = c.x + t.x, ty = c.y + t.y, tz = c.z + t.z;
		double bx = c.x + bt.x, by = c.y + bt.y, bz = c.z + bt.z;

		P[] ring = new P[4];
		for (int s = 0; s < 4; s++) {
			double a = s * Math.PI * 0.5 + Math.PI * 0.25;
			P p = rot(new P(Math.cos(a) * r, 0, Math.sin(a) * r), yaw, pitch, roll);
			ring[s] = new P(c.x + p.x, c.y + p.y, c.z + p.z);
		}
		for (int s = 0; s < 4; s++) {
			P p = ring[s], q = ring[(s + 1) % 4];
			tube(b, m, tx, ty, tz, p.x, p.y, p.z, th, top, top, camX, camY, camZ);
			tube(b, m, p.x, p.y, p.z, q.x, q.y, q.z, th, top, bottom, camX, camY, camZ);
			tube(b, m, bx, by, bz, p.x, p.y, p.z, th, bottom, bottom, camX, camY, camZ);
		}
	}
}

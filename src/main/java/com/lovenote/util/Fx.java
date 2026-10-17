package com.lovenote.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class Fx {
	private Fx() {}

	private static final Minecraft MC = Minecraft.getInstance();

	public static final Vector3f RIGHT = new Vector3f();
	public static final Vector3f UP = new Vector3f();

	private static final ThreadLocal<MeshBuilder> MESH = new ThreadLocal<>();
	private static final ThreadLocal<Identifier> TEXTURE = new ThreadLocal<>();
	private static final ThreadLocal<RenderPipeline> PIPE = new ThreadLocal<>();
	private static final Color COL = new Color();
	private static final int[] IDX = new int[4];
	private static boolean textured;

	private static PoseStack matrices;

	private static GpuSampler repeat() {
		return RenderSystem.getSamplerCache().getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.LINEAR, FilterMode.LINEAR, false);
	}
	private static int qi;

	public static void setMatrices(PoseStack stack) { matrices = stack; }

	public static void cameraBasis(Camera cam) {
		RIGHT.set(1, 0, 0);
		UP.set(0, 1, 0);
		cam.rotation().transform(RIGHT);
		cam.rotation().transform(UP);
	}

	public static void additive() { }
	public static void end() { }

	private static void rgb(int c) {
		COL.set((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24) & 255);
	}

	private static void push(int index) {
		IDX[qi++] = index;
		if (qi == 4) {
			MeshBuilder m = MESH.get();
			if (m != null) m.quad(IDX[0], IDX[1], IDX[2], IDX[3]);
			qi = 0;
		}
	}

	public static void vc(MeshBuilder b, Matrix4f mat, float x, float y, float z, int c) {
		if (qi == 0) b.ensureQuadCapacity();
		rgb(c);
		if (textured) push(b.vec3(x, y, z).vec2(0f, 0f).color(COL).next());
		else push(b.vec3(x, y, z).color(COL).next());
	}

	public static void vt(MeshBuilder b, Matrix4f mat, float x, float y, float z, float u, float v, int c) {
		if (qi == 0) b.ensureQuadCapacity();
		rgb(c);
		push(b.vec3(x, y, z).vec2(u, v).color(COL).next());
	}

	public static void billboard(MeshBuilder b, Matrix4f mat, float x, float y, float z, float size, int color) {
		float h = size * 0.5f;
		float rx = RIGHT.x * h, ry = RIGHT.y * h, rz = RIGHT.z * h;
		float ux = UP.x * h, uy = UP.y * h, uz = UP.z * h;
		vt(b, mat, x - rx - ux, y - ry - uy, z - rz - uz, 0f, 1f, color);
		vt(b, mat, x + rx - ux, y + ry - uy, z + rz - uz, 1f, 1f, color);
		vt(b, mat, x + rx + ux, y + ry + uy, z + rz + uz, 1f, 0f, color);
		vt(b, mat, x - rx + ux, y - ry + uy, z - rz + uz, 0f, 0f, color);
	}

	public static void texture(Identifier tex) { TEXTURE.set(tex); }

	public static MeshBuilder texQuads() {
		Identifier tex = TEXTURE.get();
		return begin(MeteorRenderPipelines.WORLD_TEXTURED, tex == null ? Tex.SOFT : tex);
	}

	public static MeshBuilder texQuads(Identifier tex) {
		return begin(MeteorRenderPipelines.WORLD_TEXTURED, tex);
	}

	public static MeshBuilder colorQuads() {
		return begin(MeteorRenderPipelines.WORLD_COLORED, null);
	}

	private static MeshBuilder begin(RenderPipeline pipe, Identifier tex) {
		MeshBuilder mesh = new MeshBuilder(pipe);
		mesh.begin();
		MESH.set(mesh);
		PIPE.set(pipe);
		qi = 0;
		textured = tex != null;
		if (tex != null) TEXTURE.set(tex);
		return mesh;
	}

	public static void draw() {
		MeshBuilder mesh = MESH.get();
		RenderPipeline pipe = PIPE.get();
		if (mesh == null || pipe == null) return;
		mesh.end();

		MeshRenderer r = MeshRenderer.begin()
				.attachments(MC.gameRenderer.mainRenderTarget())
				.pipeline(pipe)
				.mesh(mesh, matrices == null ? new PoseStack() : matrices);

		Identifier tex = TEXTURE.get();
		if (tex != null) {
			AbstractTexture t = MC.getTextureManager().getTexture(tex);
			if (t != null) r.sampler("u_Texture", t.getTextureView(), repeat());
		}
		r.end();

		MESH.remove();
		PIPE.remove();
		TEXTURE.remove();
	}
}

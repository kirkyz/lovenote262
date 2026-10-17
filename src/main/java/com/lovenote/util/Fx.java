package com.lovenote.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.utils.render.MeshBuilderVertexConsumerProvider;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class Fx {
	private Fx() {}

	public static final Vector3f RIGHT = new Vector3f();
	public static final Vector3f UP = new Vector3f();

	private static final ThreadLocal<RenderType> LAYER = new ThreadLocal<>();
	private static final ThreadLocal<Identifier> TEXTURE = new ThreadLocal<>();
	private static final ThreadLocal<MeshBuilder> MESH = new ThreadLocal<>();
	private static final PoseStack IDENTITY = new PoseStack();

	public static void cameraBasis(Camera cam) {
		RIGHT.set(1, 0, 0);
		UP.set(0, 1, 0);
		cam.rotation().transform(RIGHT);
		cam.rotation().transform(UP);
	}

	public static void additive() { }
	public static void end() { }

	public static void vc(VertexConsumer b, Matrix4f m, float x, float y, float z, int c) {
		b.addVertex(m, x, y, z)
				.setColor((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24) & 255);
	}

	public static void vt(VertexConsumer b, Matrix4f m, float x, float y, float z, float u, float v, int c) {
		b.addVertex(m, x, y, z)
				.setUv(u, v)
				.setColor((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24) & 255);
	}

	public static void billboard(VertexConsumer b, Matrix4f m, float x, float y, float z, float size, int color) {
		float h = size * 0.5f;
		float rx = RIGHT.x * h, ry = RIGHT.y * h, rz = RIGHT.z * h;
		float ux = UP.x * h, uy = UP.y * h, uz = UP.z * h;
		vt(b, m, x - rx - ux, y - ry - uy, z - rz - uz, 0f, 1f, color);
		vt(b, m, x + rx - ux, y + ry - uy, z + rz - uz, 1f, 1f, color);
		vt(b, m, x + rx + ux, y + ry + uy, z + rz + uz, 1f, 0f, color);
		vt(b, m, x - rx + ux, y - ry + uy, z - rz + uz, 0f, 0f, color);
	}

	public static void texture(Identifier tex) { TEXTURE.set(tex); }

	public static VertexConsumer texQuads() {
		Identifier tex = TEXTURE.get();
		return begin(RenderTypes.entityTranslucentEmissive(tex == null ? Tex.SOFT : tex));
	}

	public static VertexConsumer texQuads(Identifier texture) {
		return begin(RenderTypes.entityTranslucentEmissive(texture));
	}

	public static VertexConsumer colorQuads() {
		return begin(RenderTypes.debugQuads());
	}

	private static VertexConsumer begin(RenderType layer) {
		MeshBuilder mesh = new MeshBuilder(layer.pipeline());
		mesh.begin();
		MESH.set(mesh);
		LAYER.set(layer);
		return new MeshBuilderVertexConsumerProvider(mesh).getBuffer(layer);
	}

	public static void draw() {
		MeshBuilder mesh = MESH.get();
		RenderType layer = LAYER.get();
		if (mesh == null || layer == null) return;
		mesh.end();
		MeshRenderer.begin()
				.attachments(Minecraft.getInstance().gameRenderer.mainRenderTarget())
				.pipeline(layer.pipeline())
				.mesh(mesh, IDENTITY)
				.end();
		MESH.remove();
		LAYER.remove();
	}
}

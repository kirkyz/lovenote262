package com.lovenote.util;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorVertexFormats;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniformStorage;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * V48: One seamless view-direction mesh, with all atmospheric density evaluated
 * inside a finite-height slab at ABSOLUTE world positions along each view ray.
 * The geometry is just a draw surface, NOT the source of cloud coordinates.
 * Distant stars are a separate celestial pass in SkyShader.
 */
public final class NativeNebulaRenderer {
    private NativeNebulaRenderer() {}

    private static final int LONGITUDE = 40;
    private static final int LATITUDE = 20;
    private static GpuBuffer vertices;
    private static GpuBuffer indices;
    private static boolean pipelineReady;
    private static boolean pipelineFailed;

    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/drainer_atmospheric_nebula_v50"))
        .withVertexBinding(0, MeteorVertexFormats.POS3_TEXTURE_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(MeteorClient.identifier("shaders/native_nebula.vert"))
        .withFragmentShader(MeteorClient.identifier("shaders/native_nebula.frag"))
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("MeshData", UniformType.UNIFORM_BUFFER).build())
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("NebulaData", UniformType.UNIFORM_BUFFER).build())
        .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build();

    private static boolean ensurePipeline() {
        if (pipelineReady) return true;
        if (pipelineFailed) return false;
        try {
            var resources = Minecraft.getInstance().getResourceManager();
            RenderSystem.getDevice().precompilePipeline(PIPELINE, (id, ignored) -> {
                var resource = resources.getResource(id).orElseThrow();
                try (var stream = resource.open()) {
                    return IOUtils.toString(stream, StandardCharsets.UTF_8);
                } catch (IOException error) {
                    throw new IllegalStateException("Drainer V48 sky shader missing: " + id, error);
                }
            });
            pipelineReady = true;
            return true;
        } catch (RuntimeException error) {
            pipelineFailed = true;
            System.err.println("[Drainer V50] Atmospheric shader unavailable:");
            error.printStackTrace();
            return false;
        }
    }

    /** Static sky-facing unit geometry, uploaded just once, no tile edges. */
    private static void ensureMesh() {
        if (vertices != null && indices != null) return;
        MeshBuilder mesh = new MeshBuilder(PIPELINE);
        mesh.begin();
        var camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
        double ox = Utils.rendering3D ? camera.x : 0.0;
        double oz = Utils.rendering3D ? camera.z : 0.0;
        for (int row = 0; row < LATITUDE; row++) {
            float v0 = row / (float) LATITUDE;
            float v1 = (row + 1) / (float) LATITUDE;
            double a0 = Math.PI * v0, a1 = Math.PI * v1;
            double y0 = Math.cos(a0), y1 = Math.cos(a1);
            double r0 = Math.sin(a0), r1 = Math.sin(a1);
            for (int seg = 0; seg < LONGITUDE; seg++) {
                float u0 = seg / (float) LONGITUDE;
                float u1 = (seg + 1) / (float) LONGITUDE;
                double b0 = Math.PI * 2.0 * u0;
                double b1 = Math.PI * 2.0 * u1;
                mesh.ensureQuadCapacity();
                int a = mesh.vec3(ox + Math.cos(b0)*r0, y0, oz + Math.sin(b0)*r0)
                    .vec2(u0,v0).color(Color.WHITE).next();
                int b = mesh.vec3(ox + Math.cos(b1)*r0, y0, oz + Math.sin(b1)*r0)
                    .vec2(u1,v0).color(Color.WHITE).next();
                int c = mesh.vec3(ox + Math.cos(b1)*r1, y1, oz + Math.sin(b1)*r1)
                    .vec2(u1,v1).color(Color.WHITE).next();
                int d = mesh.vec3(ox + Math.cos(b0)*r1, y1, oz + Math.sin(b0)*r1)
                    .vec2(u0,v1).color(Color.WHITE).next();
                mesh.quad(a,b,c,d);
            }
        }
        mesh.end();
        GpuBuffer vbo = mesh.getVertexBuffer();
        try {
            GpuBuffer ibo = mesh.getIndexBuffer();
            vertices = vbo;
            indices = ibo;
        } catch (RuntimeException error) {
            vbo.close();
            throw error;
        }
    }

    public static void release() {
        if (vertices != null) { vertices.close(); vertices = null; }
        if (indices != null) { indices.close(); indices = null; }
        pipelineReady = false;
        pipelineFailed = false;
    }

    private static final int UBO_SIZE = new Std140SizeCalculator()
        .putVec4().putVec4().putVec4().putVec4().putVec4().get();
    private static final DynamicUniformStorage<NebulaData> STORAGE =
        new DynamicUniformStorage<>("Drainer V50 Atmospheric UBO", UBO_SIZE, 16);

    private record NebulaData(
        float camX, float camY, float camZ, float time,
        float altitude, float thickness, float opacity, float density,
        float r1, float g1, float b1, float flow,
        float r2, float g2, float b2, float brightness,
        float quality, float farDistance, float drift, float unused
    ) implements DynamicUniformStorage.DynamicUniform {
        @Override public void write(@NonNull ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec4(camX, camY, camZ, time)
                .putVec4(altitude, thickness, opacity, density)
                .putVec4(r1, g1, b1, flow)
                .putVec4(r2, g2, b2, brightness)
                .putVec4(quality, farDistance, drift, unused);
        }
    }

    public static void draw(Camera camera, PoseStack stack, float time, int quality,
                            float altitude, float thickness, float opacity,
                            float density, float flow, float brightness,
                            int primary, int secondary) {
        if (!ensurePipeline()) return;
        ensureMesh();
        var pos = camera.position();
        float distance = quality == 0 ? 800f : quality == 2 ? 1700f : 1250f;
        try {
            MeshRenderer.begin()
                .attachments(Minecraft.getInstance().gameRenderer.mainRenderTarget())
                .pipeline(PIPELINE)
                .mesh(vertices.slice(), indices.slice(), IndexType.INT)
                .transform(stack)
                .uniform("NebulaData", STORAGE.writeUniform(new NebulaData(
                    (float) pos.x, (float) pos.y, (float) pos.z, time,
                    altitude, Math.max(16f, thickness * 2f), opacity, density,
                    red(primary), green(primary), blue(primary), flow,
                    red(secondary), green(secondary), blue(secondary), brightness,
                    Math.max(0, Math.min(2, quality)), distance, 1f, 0f
                )))
                .end();
        } finally {
            STORAGE.endFrame();
        }
    }
    private static float red(int c) { return ((c >>> 16) & 255) / 255f; }
    private static float green(int c) { return ((c >>> 8) & 255) / 255f; }
    private static float blue(int c) { return (c & 255) / 255f; }
}

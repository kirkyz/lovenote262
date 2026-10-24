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
 * Experimental V47 native WORLD-ANCHORED diffuse nebula canopy.
 * Unlike CelestialGpuRenderer (an infinitely distant directional sky),
 * sheets are placed in stable world-space tiles, with actual parallax.
 * Geometry is built/uploaded once per quality tier and animated on the GPU.
 * This is not a physically accurate galaxy or a volumetric raymarcher.
 */
public final class NativeNebulaRenderer {
    private NativeNebulaRenderer() {}
    private static final float CELL = 160.0f;
    private static final int GRID = 3;
    private static final int MAX_LAYERS = 3;
    private static final GpuBuffer[] VERTICES = new GpuBuffer[3];
    private static final GpuBuffer[] INDICES = new GpuBuffer[3];
    private static boolean pipelineReady, pipelineFailed;

    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/drainer_native_nebula"))
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
        .withCull(false).build();

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
                    throw new IllegalStateException("Drainer world nebula shader failed to load " + id, error);
                }
            });
            pipelineReady = true;
            return true;
        } catch (RuntimeException error) {
            pipelineFailed = true;
            System.err.println("[Drainer V47] Native world nebula shader compile failed:");
            error.printStackTrace();
            return false;
        }
    }

    private static void ensureMesh(int q) {
        if (VERTICES[q] != null && INDICES[q] != null) return;
        int steps = q == 0 ? 8 : q == 2 ? 16 : 12;
        int layers = q + 1;
        MeshBuilder mesh = new MeshBuilder(PIPELINE);
        mesh.begin();
        var camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
        // MeshBuilder subtracts camera X/Z during 3D builds. Undo that in the
        // parameter mesh, because its positions encode tile/layer parameters.
        double ox = Utils.rendering3D ? camera.x : 0.0;
        double oz = Utils.rendering3D ? camera.z : 0.0;
        for (int tile = 0; tile < GRID * GRID; tile++) {
            for (int layer = 0; layer < layers; layer++) {
                float code = tile * MAX_LAYERS + layer;
                for (int i = 0; i < steps; i++) {
                    float u0 = i / (float) steps, u1 = (i + 1) / (float) steps;
                    for (int j = 0; j < steps; j++) {
                        float v0 = j / (float) steps, v1 = (j + 1) / (float) steps;
                        mesh.ensureQuadCapacity();
                        int a = mesh.vec3(ox + u0, v0, oz + code).vec2(u0,v0).color(Color.WHITE).next();
                        int b = mesh.vec3(ox + u1, v0, oz + code).vec2(u1,v0).color(Color.WHITE).next();
                        int c = mesh.vec3(ox + u1, v1, oz + code).vec2(u1,v1).color(Color.WHITE).next();
                        int d = mesh.vec3(ox + u0, v1, oz + code).vec2(u0,v1).color(Color.WHITE).next();
                        mesh.quad(a,b,c,d);
                    }
                }
            }
        }
        mesh.end();
        GpuBuffer vertices = mesh.getVertexBuffer();
        try {
            GpuBuffer indices = mesh.getIndexBuffer();
            VERTICES[q] = vertices;
            INDICES[q] = indices;
        } catch (RuntimeException error) {
            vertices.close();
            throw error;
        }
    }

    public static void release() {
        for (int i = 0; i < 3; i++) {
            if (VERTICES[i] != null) { VERTICES[i].close(); VERTICES[i] = null; }
            if (INDICES[i] != null) { INDICES[i].close(); INDICES[i] = null; }
        }
        pipelineReady = false;
        pipelineFailed = false;
    }

    private static final int UBO_SIZE = new Std140SizeCalculator()
        .putVec4().putVec4().putVec4().putVec4().putVec4().get();
    private static final DynamicUniformStorage<NebulaData> STORAGE =
        new DynamicUniformStorage<>("Drainer World Nebula UBO", UBO_SIZE, 16);

    private record NebulaData(
        float camX, float camY, float camZ, float time,
        float cellX, float cellZ, float altitude, float layerSpacing,
        float r1, float g1, float b1, float opacity,
        float r2, float g2, float b2, float flow,
        float brightness, float density, float cellSize, float quality
    ) implements DynamicUniformStorage.DynamicUniform {
        @Override public void write(@NonNull ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec4(camX, camY, camZ, time)
                .putVec4(cellX, cellZ, altitude, layerSpacing)
                .putVec4(r1, g1, b1, opacity)
                .putVec4(r2, g2, b2, flow)
                .putVec4(brightness, density, cellSize, quality);
        }
    }

    public static void draw(Camera camera, PoseStack stack, float time, int quality,
                            float altitude, float spacing, float opacity,
                            float density, float flow, float brightness,
                            int primary, int secondary) {
        if (!ensurePipeline()) return;
        int q = Math.max(0, Math.min(2, quality));
        ensureMesh(q);
        var pos = camera.position();
        float tileX = (float) Math.floor(pos.x / CELL);
        float tileZ = (float) Math.floor(pos.z / CELL);
        try {
            MeshRenderer.begin()
                .attachments(Minecraft.getInstance().gameRenderer.mainRenderTarget())
                .pipeline(PIPELINE)
                .mesh(VERTICES[q].slice(), INDICES[q].slice(), IndexType.INT)
                .transform(stack)
                .uniform("NebulaData", STORAGE.writeUniform(new NebulaData(
                    (float) pos.x, (float) pos.y, (float) pos.z, time,
                    tileX, tileZ, altitude, spacing,
                    red(primary), green(primary), blue(primary), opacity,
                    red(secondary), green(secondary), blue(secondary), flow,
                    brightness, density, CELL, q
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

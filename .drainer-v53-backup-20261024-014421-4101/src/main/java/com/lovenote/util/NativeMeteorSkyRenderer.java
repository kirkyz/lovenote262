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
 * V52 expands the Celestial Storm sky with more life: extra comets, world sparks,
 * drifting dust and large soft haze billboards that hide the bright horizon band.
 */
public final class NativeMeteorSkyRenderer {
    private NativeMeteorSkyRenderer() {}

    private static final float CELL = 176.0f;
    private static final int GRID = 5;
    private static final int MAX_ITEMS_PER_CELL = 16;
    private static final GpuBuffer[] VERTICES = new GpuBuffer[3];
    private static final GpuBuffer[] INDICES = new GpuBuffer[3];
    private static boolean pipelineReady;
    private static boolean pipelineFailed;

    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/drainer_native_meteor"))
        .withVertexBinding(0, MeteorVertexFormats.POS3_TEXTURE_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(MeteorClient.identifier("shaders/native_meteor.vert"))
        .withFragmentShader(MeteorClient.identifier("shaders/native_meteor.frag"))
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("MeshData", UniformType.UNIFORM_BUFFER).build())
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("MeteorData", UniformType.UNIFORM_BUFFER).build())
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
                } catch (IOException e) {
                    throw new IllegalStateException("Missing native meteor shader: " + id, e);
                }
            });
            pipelineReady = true;
            return true;
        } catch (RuntimeException ex) {
            pipelineFailed = true;
            System.err.println("[Drainer V52] Celestial Storm shader failed to load:");
            ex.printStackTrace();
            return false;
        }
    }

    private static void ensureMesh(int quality) {
        if (VERTICES[quality] != null && INDICES[quality] != null) return;
        int count = quality == 0 ? 6 : quality == 2 ? MAX_ITEMS_PER_CELL : 11;
        MeshBuilder mesh = new MeshBuilder(PIPELINE);
        mesh.begin();
        var camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
        double ox = Utils.rendering3D ? camera.x : 0.0;
        double oz = Utils.rendering3D ? camera.z : 0.0;
        for (int cell = 0; cell < GRID * GRID; cell++) {
            for (int item = 0; item < count; item++) {
                float encoded = cell * MAX_ITEMS_PER_CELL + item;
                mesh.ensureQuadCapacity();
                int a = mesh.vec3(ox, 0, oz + encoded).vec2(0,0).color(Color.WHITE).next();
                int b = mesh.vec3(ox, 0, oz + encoded).vec2(1,0).color(Color.WHITE).next();
                int c = mesh.vec3(ox, 0, oz + encoded).vec2(1,1).color(Color.WHITE).next();
                int d = mesh.vec3(ox, 0, oz + encoded).vec2(0,1).color(Color.WHITE).next();
                mesh.quad(a,b,c,d);
            }
        }
        mesh.end();
        GpuBuffer vertices = mesh.getVertexBuffer();
        try {
            GpuBuffer indices = mesh.getIndexBuffer();
            VERTICES[quality] = vertices;
            INDICES[quality] = indices;
        } catch (RuntimeException ex) {
            vertices.close();
            throw ex;
        }
    }

    public static void release() {
        for (int i = 0; i < VERTICES.length; i++) {
            if (VERTICES[i] != null) { VERTICES[i].close(); VERTICES[i] = null; }
            if (INDICES[i] != null) { INDICES[i].close(); INDICES[i] = null; }
        }
        pipelineReady = false;
        pipelineFailed = false;
    }

    private static final int UBO_SIZE = new Std140SizeCalculator()
        .putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().get();
    private static final DynamicUniformStorage<MeteorData> STORAGE =
        new DynamicUniformStorage<>("Drainer World Meteor UBO", UBO_SIZE, 16);

    private record MeteorData(
        float cameraX, float cameraY, float cameraZ, float time,
        float tileX, float tileZ, float altitude, float length,
        float r1, float g1, float b1, float opacity,
        float r2, float g2, float b2, float activity,
        float brightness, float stardust, float cellSize, float quality,
        float cometBurst, float cosmicHaze, float horizonVeil, float life
    ) implements DynamicUniformStorage.DynamicUniform {
        @Override public void write(@NonNull ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec4(cameraX, cameraY, cameraZ, time)
                .putVec4(tileX, tileZ, altitude, length)
                .putVec4(r1, g1, b1, opacity)
                .putVec4(r2, g2, b2, activity)
                .putVec4(brightness, stardust, cellSize, quality)
                .putVec4(cometBurst, cosmicHaze, horizonVeil, life);
        }
    }

    public static void draw(Camera camera, PoseStack stack, float time, int quality,
                            float altitude, float length, float opacity, float activity,
                            boolean stardust, float brightness,
                            boolean cometBurst, boolean cosmicHaze,
                            float horizonVeil, float life, int primary, int secondary) {
        if (!ensurePipeline()) return;
        int q = Math.max(0, Math.min(2, quality));
        ensureMesh(q);
        var p = camera.position();
        float tileX = (float) Math.floor(p.x / CELL);
        float tileZ = (float) Math.floor(p.z / CELL);
        try {
            MeshRenderer.begin()
                .attachments(Minecraft.getInstance().gameRenderer.mainRenderTarget())
                .pipeline(PIPELINE)
                .mesh(VERTICES[q].slice(), INDICES[q].slice(), IndexType.INT)
                .transform(stack)
                .uniform("MeteorData", STORAGE.writeUniform(new MeteorData(
                    (float) p.x, (float) p.y, (float) p.z, time,
                    tileX, tileZ, altitude, length,
                    red(primary), green(primary), blue(primary), opacity,
                    red(secondary), green(secondary), blue(secondary), activity,
                    brightness, stardust ? 1f : 0f, CELL, q,
                    cometBurst ? 1f : 0f, cosmicHaze ? 1f : 0f, horizonVeil, life
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

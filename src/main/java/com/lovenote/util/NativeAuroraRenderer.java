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
 * V45 native atmospheric ribbons.  Unlike SkyGpuRenderer, vertices are placed
 * at fixed world coordinates and are depth-tested against Minecraft terrain.
 * The mesh is allocated/uploaded once per quality setting. Wave geometry,
 * colour gradients and animation run on the GPU; no per-frame mesh rebuilds.
 *
 * This is a first atmospheric renderer, NOT volumetric scattering or HDR bloom.
 */
public final class NativeAuroraRenderer {
    private NativeAuroraRenderer() {}

    private static final float CELL = 224.0f;
    private static final int GRID = 3;
    private static final int RIBBONS = 3;
    private static boolean pipelineReady;
    private static boolean pipelineFailed;
    private static final GpuBuffer[] VERTICES = new GpuBuffer[3];
    private static final GpuBuffer[] INDICES = new GpuBuffer[3];

    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/drainer_native_aurora"))
        .withVertexBinding(0, MeteorVertexFormats.POS3_TEXTURE_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(MeteorClient.identifier("shaders/native_aurora.vert"))
        .withFragmentShader(MeteorClient.identifier("shaders/native_aurora.frag"))
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("MeshData", UniformType.UNIFORM_BUFFER).build())
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("AuroraData", UniformType.UNIFORM_BUFFER).build())
        .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build();

    private static boolean ensurePipeline() {
        if (pipelineReady) return true;
        if (pipelineFailed) return false;
        try {
            var resources = Minecraft.getInstance().getResourceManager();
            RenderSystem.getDevice().precompilePipeline(PIPELINE, (id, _ignored) -> {
                var shader = resources.getResource(id).orElseThrow();
                try (var stream = shader.open()) {
                    return IOUtils.toString(stream, StandardCharsets.UTF_8);
                } catch (IOException error) {
                    throw new IllegalStateException("Drainer native aurora shader could not load: " + id, error);
                }
            });
            pipelineReady = true;
            return true;
        } catch (RuntimeException error) {
            pipelineFailed = true;
            System.err.println("[Drainer V45] Native aurora shader failed to compile; disable Native 3D curtains.");
            error.printStackTrace();
            return false;
        }
    }

    /** Cached unit grid: z stores the tile+curtain index, not a position. */
    private static void ensureMesh(int quality) {
        if (VERTICES[quality] != null && INDICES[quality] != null) return;
        int columns = quality == 0 ? 24 : quality == 2 ? 52 : 36;
        int rows = quality == 0 ? 8 : quality == 2 ? 14 : 11;
        int ribbons = quality == 0 ? 2 : RIBBONS;
        MeshBuilder mesh = new MeshBuilder(PIPELINE);
        mesh.begin();
        var camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
        // MeshBuilder subtracts current X/Z. Cancel that when uploading this
        // static parameter mesh; the GPU will calculate camera translation.
        double ox = Utils.rendering3D ? camera.x : 0.0;
        double oz = Utils.rendering3D ? camera.z : 0.0;
        Color white = Color.WHITE;
        for (int tile = 0; tile < GRID * GRID; tile++) {
            for (int ribbon = 0; ribbon < ribbons; ribbon++) {
                float encoded = tile * RIBBONS + ribbon;
                for (int i = 0; i < columns; i++) {
                    float u0 = i / (float) columns;
                    float u1 = (i + 1) / (float) columns;
                    for (int j = 0; j < rows; j++) {
                        float v0 = j / (float) rows;
                        float v1 = (j + 1) / (float) rows;
                        mesh.ensureQuadCapacity();
                        int a = mesh.vec3(ox + u0, v0, oz + encoded).vec2(u0, v0).color(white).next();
                        int b = mesh.vec3(ox + u1, v0, oz + encoded).vec2(u1, v0).color(white).next();
                        int c = mesh.vec3(ox + u1, v1, oz + encoded).vec2(u1, v1).color(white).next();
                        int d = mesh.vec3(ox + u0, v1, oz + encoded).vec2(u0, v1).color(white).next();
                        mesh.quad(a, b, c, d);
                    }
                }
            }
        }
        mesh.end();
        GpuBuffer vertex = mesh.getVertexBuffer();
        try {
            GpuBuffer index = mesh.getIndexBuffer();
            VERTICES[quality] = vertex;
            INDICES[quality] = index;
        } catch (RuntimeException error) {
            vertex.close();
            throw error;
        }
    }

    /** Call from render/resource reload integration when available. */
    public static void release() {
        for (int i = 0; i < VERTICES.length; i++) {
            if (VERTICES[i] != null) { VERTICES[i].close(); VERTICES[i] = null; }
            if (INDICES[i] != null) { INDICES[i].close(); INDICES[i] = null; }
        }
        pipelineReady = false;
        pipelineFailed = false;
    }

    private static final int UBO_SIZE = new Std140SizeCalculator()
        .putVec4().putVec4().putVec4().putVec4().putVec4().get();
    private static final DynamicUniformStorage<AuroraData> STORAGE =
        new DynamicUniformStorage<>("Drainer Native Aurora UBO", UBO_SIZE, 16);

    private record AuroraData(
        float cameraX, float cameraY, float cameraZ, float time,
        float cellX, float cellZ, float altitude, float height,
        float r1, float g1, float b1, float opacity,
        float r2, float g2, float b2, float flow,
        float brightness, float quality, float cellSize, float reserved
    ) implements DynamicUniformStorage.DynamicUniform {
        @Override public void write(@NonNull ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec4(cameraX, cameraY, cameraZ, time)
                .putVec4(cellX, cellZ, altitude, height)
                .putVec4(r1, g1, b1, opacity)
                .putVec4(r2, g2, b2, flow)
                .putVec4(brightness, quality, cellSize, reserved);
        }
    }

    public static void draw(Camera camera, PoseStack stack, float time, int quality,
                            float altitude, float curtainHeight, float opacity, float flow,
                            float brightness, int primary, int secondary) {
        if (!ensurePipeline()) return;
        int q = Math.max(0, Math.min(2, quality));
        ensureMesh(q);
        var p = camera.position();
        // Integer tile ID changes only at fixed WORLD boundaries, never in
        // response to direction, speed or the player's local rotation.
        float tileX = (float) Math.floor(p.x / CELL);
        float tileZ = (float) Math.floor(p.z / CELL);
        try {
            MeshRenderer.begin()
                .attachments(Minecraft.getInstance().gameRenderer.mainRenderTarget())
                .pipeline(PIPELINE)
                .mesh(VERTICES[q].slice(), INDICES[q].slice(), IndexType.INT)
                .transform(stack)
                .uniform("AuroraData", STORAGE.writeUniform(new AuroraData(
                    (float) p.x, (float) p.y, (float) p.z, time,
                    tileX, tileZ, altitude, curtainHeight,
                    red(primary), green(primary), blue(primary), opacity,
                    red(secondary), green(secondary), blue(secondary), flow,
                    brightness, q, CELL, 0f
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

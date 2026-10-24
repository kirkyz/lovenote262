package com.lovenote.util;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorVertexFormats;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;

/**
 * Isolated world-anchor diagnostic pipeline. The dome uses persistent vertex/index
 * buffers once; frame-to-frame animation is entirely shader driven.
 * Sky directions are camera independent so no geometry needs to be uploaded
 * just because the player moves. No direct OpenGL or Iris dependency.
 */
public final class SkyAnchorProbeRenderer {
    private SkyAnchorProbeRenderer() {}

    private static volatile boolean pipelineReady;
    private static volatile boolean pipelineFailed;

    /** Shader resources are loaded the first time the effect is used. */
    private static boolean ensurePipeline() {
        if (pipelineReady) return true;
        if (pipelineFailed) return false;
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        try {
            RenderSystem.getDevice().precompilePipeline(PIPELINE, (identifier, ignored) -> {
                var resource = resources.getResource(identifier).orElseThrow();
                try (var input = resource.open()) {
                    return IOUtils.toString(input, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to read Drainer sky shader " + identifier, e);
                }
            });
            pipelineReady = true;
            return true;
        } catch (RuntimeException error) {
            pipelineFailed = true;
            System.err.println("[Drainer V63 Anchor Probe] GPU sky shader compile failed; disable SkyShader and report this error:");
            error.printStackTrace();
            return false;
        }
    }

    // The sphere is a DIRECTIONS mesh, not a world-positioned object.
    // Translation is removed by the vertex shader and it always draws
    // at the far depth. One GPU mesh is sufficient for every frame.
    private static final int SEGS = 48;
    private static final int ROWS = 24;
    private static GpuBuffer skyVertices;
    private static GpuBuffer skyIndices;

    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/drainer_anchor_probe_test"))
        .withVertexBinding(0, MeteorVertexFormats.POS3_TEXTURE_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(MeteorClient.identifier("shaders/sky_studio.vert"))
        .withFragmentShader(MeteorClient.identifier("shaders/sky_anchor_probe.frag"))
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("MeshData", UniformType.UNIFORM_BUFFER).build())
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("SkyData", UniformType.UNIFORM_BUFFER).build())
        .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build();

    /**
     * Build the static directions mesh exactly once. MeshBuilder subtracts the
     * active camera X/Z during 3D rendering; adding it while emitting vertices
     * leaves camera-relative local directions inside the persistent VBO.
     */
    private static void ensureMesh() {
        if (skyVertices != null && skyIndices != null) return;
        MeshBuilder mesh = new MeshBuilder(PIPELINE);
        mesh.begin();
        var camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
        double originX = Utils.rendering3D ? camera.x : 0.0;
        double originZ = Utils.rendering3D ? camera.z : 0.0;
        for (int row = 0; row < ROWS; row++) {
            float v0 = row / (float) ROWS, v1 = (row + 1) / (float) ROWS;
            double a0 = Math.PI * v0, a1 = Math.PI * v1;
            double y0 = Math.cos(a0), y1 = Math.cos(a1);
            double r0 = Math.sin(a0), r1 = Math.sin(a1);
            for (int seg = 0; seg < SEGS; seg++) {
                float u0 = seg / (float) SEGS, u1 = (seg + 1) / (float) SEGS;
                double b0 = Math.PI * 2.0 * u0, b1 = Math.PI * 2.0 * u1;
                mesh.ensureQuadCapacity();
                int a = mesh.vec3(originX + Math.cos(b0)*r0, y0, originZ + Math.sin(b0)*r0)
                    .vec2(u0,v0).color(meteordevelopment.meteorclient.utils.render.color.Color.WHITE).next();
                int b = mesh.vec3(originX + Math.cos(b1)*r0, y0, originZ + Math.sin(b1)*r0)
                    .vec2(u1,v0).color(meteordevelopment.meteorclient.utils.render.color.Color.WHITE).next();
                int c = mesh.vec3(originX + Math.cos(b1)*r1, y1, originZ + Math.sin(b1)*r1)
                    .vec2(u1,v1).color(meteordevelopment.meteorclient.utils.render.color.Color.WHITE).next();
                int d = mesh.vec3(originX + Math.cos(b0)*r1, y1, originZ + Math.sin(b0)*r1)
                    .vec2(u0,v1).color(meteordevelopment.meteorclient.utils.render.color.Color.WHITE).next();
                mesh.quad(a,b,c,d);
            }
        }
        mesh.end();
        GpuBuffer vertices = mesh.getVertexBuffer();
        try {
            GpuBuffer indices = mesh.getIndexBuffer();
            skyVertices = vertices;
            skyIndices = indices;
        } catch (RuntimeException error) {
            vertices.close();
            throw error;
        }
    }

    /** Release GPU memory before a renderer/device teardown, if invoked by the host. */
    public static void release() {
        if (skyVertices != null) { skyVertices.close(); skyVertices = null; }
        if (skyIndices != null) { skyIndices.close(); skyIndices = null; }
        pipelineReady = false;
        pipelineFailed = false;
    }

    private static final int UBO_SIZE = new Std140SizeCalculator()
        .putVec4().putVec4().putVec4().putVec4().putVec4().get();
    private static final DynamicUniformStorage<SkyData> STORAGE =
        new DynamicUniformStorage<>("Drainer Polar Sky UBO", UBO_SIZE, 16);

    private record SkyData(
        float time, float mode, float quality, float brightness,
        float r1, float g1, float b1, float density,
        float r2, float g2, float b2, float flow,
        float scale, float size, float aurora, float expanse,
        float camX, float camY, float camZ, float altitude
    ) implements DynamicUniformStorage.DynamicUniform {
        @Override public void write(@NonNull ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec4(time, mode, quality, brightness)
                .putVec4(r1, g1, b1, density)
                .putVec4(r2, g2, b2, flow)
                .putVec4(scale, size, aurora, expanse)
                .putVec4(camX, camY, camZ, altitude);
        }
    }

    public static void draw(Camera camera, PoseStack stack, float time,
                            float anchorX, float anchorY, float anchorZ,
                            float hueA, float hueB, float saturation, float brightness) {
        if (!ensurePipeline()) return;
        var pos = camera.position();
        float cx = (float) pos.x, cy = (float) pos.y, cz = (float) pos.z;
        ensureMesh();
        int first = SkyColorMath.hsv(hueA, saturation / 100f, 1.0f);
        int second = SkyColorMath.hsv(hueB, saturation / 100f, 1.0f);
        try {
            MeshRenderer.begin()
                .attachments(Minecraft.getInstance().gameRenderer.mainRenderTarget())
                .pipeline(PIPELINE).mesh(skyVertices.slice(), skyIndices.slice(), IndexType.INT).transform(stack)
                .uniform("SkyData", STORAGE.writeUniform(new SkyData(
                    time, 5.0f, 1.0f, brightness,
                    red(first), green(first), blue(first), 1.0f,
                    red(second), green(second), blue(second), 1.0f,
                    1.0f, anchorX, anchorY, anchorZ,
                    cx, cy, cz, 0f
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

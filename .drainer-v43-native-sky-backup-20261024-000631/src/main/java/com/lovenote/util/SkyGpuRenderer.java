package com.lovenote.util;

import com.mojang.blaze3d.PrimitiveTopology;
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
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorVertexFormats;
import meteordevelopment.meteorclient.utils.render.color.Color;
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
 * First real shader-driven sky renderer. Geometry only establishes the sky surface;
 * all nebula/aurora/plasma animation happens in sky_studio.frag using GPU time.
 * Deliberately no OpenGL direct calls and no extra external runtime dependency.
 */
public final class SkyGpuRenderer {
    private SkyGpuRenderer() {}

    private static volatile boolean pipelineReady;
    private static volatile boolean pipelineFailed;

    /** Shader resources are loaded the first time the effect is used. */
    private static boolean ensurePipeline() {
        if (pipelineReady) return true;
        if (pipelineFailed) return false;
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        try {
            RenderSystem.getDevice().precompilePipeline(PIPELINE, (identifier, _ignored) -> {
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
            System.err.println("[Drainer V38] GPU sky shader compile failed; disable SkyShader and report this error:");
            error.printStackTrace();
            return false;
        }
    }

    // The mesh is camera-centred; radius does not affect perceived sky scale.
    // Keep it within practical far-plane limits; use shader angular scale instead.
    private static final float R = 240f;
    private static final int SEGS = 32;
    private static final int ROWS = 16;
    private static final float[] COS_A = new float[SEGS + 1];
    private static final float[] SIN_A = new float[SEGS + 1];
    private static final float[] SIN_Y = new float[ROWS + 1];
    private static final float[] COS_Y = new float[ROWS + 1];

    static {
        for (int i = 0; i <= SEGS; i++) {
            float a = (float) (i * Math.PI * 2.0 / SEGS);
            COS_A[i] = (float) Math.cos(a);
            SIN_A[i] = (float) Math.sin(a);
        }
        for (int j = 0; j <= ROWS; j++) {
            float a = (float) (j * Math.PI / ROWS);
            COS_Y[j] = (float) Math.cos(a);
            SIN_Y[j] = (float) Math.sin(a);
        }
    }

    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
        .withLocation(MeteorClient.identifier("pipeline/drainer_sky_gpu"))
        .withVertexBinding(0, MeteorVertexFormats.POS3_TEXTURE_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(MeteorClient.identifier("shaders/sky_studio.vert"))
        .withFragmentShader(MeteorClient.identifier("shaders/sky_studio.frag"))
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("MeshData", UniformType.UNIFORM_BUFFER).build())
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("SkyData", UniformType.UNIFORM_BUFFER).build())
        .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build();

    // Reuse the CPU-side vertex/index allocation. The transient GPU upload is still
    // performed each frame; a future dedicated sky pass can cache GPU buffers too.
    private static final MeshBuilder REUSABLE_MESH = new MeshBuilder(PIPELINE);

    private static final int UBO_SIZE = new Std140SizeCalculator()
        .putVec4().putVec4().putVec4().putVec4().putVec4().get();
    private static final DynamicUniformStorage<SkyData> STORAGE =
        new DynamicUniformStorage<>("Drainer Sky Studio UBO", UBO_SIZE, 16);

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

    public static void draw(Camera camera, PoseStack stack, float time, int mode, int quality,
                            float brightness, float density, float flow, float scale,
                            float blackHoleSize, float auroraIntensity, float expanse,
                            float auroraAltitude, int c1, int c2) {
        if (!ensurePipeline()) return;
        var pos = camera.position();
        float cx = (float) pos.x, cy = (float) pos.y, cz = (float) pos.z;
        MeshBuilder mesh = REUSABLE_MESH;
        mesh.begin();
        for (int row = 0; row < ROWS; row++) {
            float y0 = COS_Y[row] * R, y1 = COS_Y[row + 1] * R;
            float rad0 = SIN_Y[row] * R, rad1 = SIN_Y[row + 1] * R;
            float v0 = row / (float) ROWS, v1 = (row + 1) / (float) ROWS;
            for (int seg = 0; seg < SEGS; seg++) {
                float u0 = seg / (float) SEGS, u1 = (seg + 1) / (float) SEGS;
                mesh.ensureQuadCapacity();
                int a = mesh.vec3(cx + COS_A[seg] * rad0, cy + y0, cz + SIN_A[seg] * rad0)
                    .vec2(u0, v0).color(Color.WHITE).next();
                int b = mesh.vec3(cx + COS_A[seg+1] * rad0, cy + y0, cz + SIN_A[seg+1] * rad0)
                    .vec2(u1, v0).color(Color.WHITE).next();
                int c = mesh.vec3(cx + COS_A[seg+1] * rad1, cy + y1, cz + SIN_A[seg+1] * rad1)
                    .vec2(u1, v1).color(Color.WHITE).next();
                int d = mesh.vec3(cx + COS_A[seg] * rad1, cy + y1, cz + SIN_A[seg] * rad1)
                    .vec2(u0, v1).color(Color.WHITE).next();
                mesh.quad(a,b,c,d);
            }
        }
        mesh.end();
        try {
            MeshRenderer.begin()
                .attachments(Minecraft.getInstance().gameRenderer.mainRenderTarget())
                .pipeline(PIPELINE).mesh(mesh, stack)
                .uniform("SkyData", STORAGE.writeUniform(new SkyData(
                    time, mode, quality, brightness,
                    red(c1), green(c1), blue(c1), density,
                    red(c2), green(c2), blue(c2), flow,
                    scale, blackHoleSize, auroraIntensity, expanse,
                    cx, cy, cz, auroraAltitude
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

package meteordevelopment.meteorclient.mixin;

import com.lovenote.util.LovenoteRender;
import com.lovenote.util.WorldRender;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LovenoteLevelRendererMixin {

        @Inject(method = "render", at = @At("HEAD"))
        private void lovenote$frameStart(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker,
                                         boolean renderOutline, CameraRenderState cameraState,
                                         Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog,
                                         Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
                LovenoteRender.resetFrame();
                WorldRenderContext ctx = context(deltaTracker, modelViewMatrix);
                for (var consumer : WorldRender.START) consumer.accept(ctx);
        }

        @Inject(method = "render", at = @At("RETURN"))
        private void lovenote$frameEnd(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker,
                                       boolean renderOutline, CameraRenderState cameraState,
                                       Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog,
                                       Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
                WorldRenderContext ctx = context(deltaTracker, modelViewMatrix);
                for (var consumer : WorldRender.LAST) consumer.accept(ctx);
                for (var consumer : WorldRender.END) consumer.accept(ctx);
        }

        private static WorldRenderContext context(DeltaTracker deltaTracker, Matrix4fc modelViewMatrix) {
                PoseStack pose = new PoseStack();
                pose.last().pose().set(modelViewMatrix);
                return new WorldRenderContext(pose, MeteorClient.mc.gameRenderer.mainCamera(),
                                deltaTracker.getGameTimeDeltaPartialTick(false));
        }
}

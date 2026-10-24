package meteordevelopment.meteorclient.mixin;

import com.lovenote.Lovenote;
import com.lovenote.module.impl.SkyShader;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** V56: no vanilla sky colour takeover; Nuit controls custom sky rendering. */
@Mixin(SkyRenderer.class)
public abstract class LovenoteSkyRendererMixin {
    @Inject(method = "renderSun", at = @At("HEAD"), cancellable = true)
    private void drainer$hideVanillaSun(float rainBrightness, PoseStack poseStack, CallbackInfo ci) {
        if (Lovenote.modules == null) return;
        SkyShader sky = Lovenote.modules.get(SkyShader.class);
        if (sky != null && sky.hideVanillaSun()) ci.cancel();
    }
    @Inject(method = "renderMoon", at = @At("HEAD"), cancellable = true)
    private void drainer$hideVanillaMoon(MoonPhase phase, float rainBrightness, PoseStack poseStack, CallbackInfo ci) {
        if (Lovenote.modules == null) return;
        SkyShader sky = Lovenote.modules.get(SkyShader.class);
        if (sky != null && sky.hideVanillaMoon()) ci.cancel();
    }
}

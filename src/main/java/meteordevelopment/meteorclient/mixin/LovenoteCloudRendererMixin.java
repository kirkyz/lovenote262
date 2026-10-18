package meteordevelopment.meteorclient.mixin;

import com.lovenote.Lovenote;
import com.lovenote.module.impl.SkyShader;
import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CloudRenderer.class)
public abstract class LovenoteCloudRendererMixin {

	@Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
	private void lovenote$noClouds(int a0, net.minecraft.client.CloudStatus a1, float a2, int a3, net.minecraft.world.phys.Vec3 a4, long a5, float a6, CallbackInfo ci) {
		if (Lovenote.modules == null) return;

		SkyShader sky = Lovenote.modules.get(SkyShader.class);
		if (sky == null || !sky.active()) return;

		ci.cancel();
	}
}

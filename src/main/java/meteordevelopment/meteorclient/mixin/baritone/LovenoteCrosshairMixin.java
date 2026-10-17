package meteordevelopment.meteorclient.mixin.baritone;

import com.lovenote.module.impl.FunnyCrosshair;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class LovenoteCrosshairMixin {
	@Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
	private void lovenote$crosshairExtract(GuiGraphicsExtractor g, CallbackInfo ci) {
		if (FunnyCrosshair.hideVanilla()) ci.cancel();
	}

	@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
	private void lovenote$crosshairRender(GuiGraphicsExtractor g, CallbackInfo ci) {
		if (FunnyCrosshair.hideVanilla()) ci.cancel();
	}
}

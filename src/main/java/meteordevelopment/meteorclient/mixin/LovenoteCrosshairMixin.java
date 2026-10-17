package meteordevelopment.meteorclient.mixin;

import com.lovenote.module.impl.FunnyCrosshair;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class LovenoteCrosshairMixin {
        @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
        private void lovenote$crosshairExtract(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
                if (FunnyCrosshair.hideVanilla()) ci.cancel();
        }
}

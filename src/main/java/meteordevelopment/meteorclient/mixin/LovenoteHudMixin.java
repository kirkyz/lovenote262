package meteordevelopment.meteorclient.mixin;

import com.lovenote.Lovenote;
import com.lovenote.module.ModuleManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class LovenoteHudMixin {
        private static boolean errored;

        @Inject(method = "extractRenderState", at = @At("HEAD"), require = 0)
        private void lovenote$hud(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
                ModuleManager m = Lovenote.modules;
                if (m == null) return;
                try {
                        m.hud(g, 1f);
                } catch (Throwable t) {
                        if (!errored) { errored = true; t.printStackTrace(); }
                }
        }
}

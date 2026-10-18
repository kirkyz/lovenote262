package meteordevelopment.meteorclient.mixin;

import com.lovenote.util.MenuFx;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class LovenoteScreenFxMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
	private void lovenote$screenFx(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		MenuFx.onScreen(g, mouseX, mouseY, delta);
	}
}

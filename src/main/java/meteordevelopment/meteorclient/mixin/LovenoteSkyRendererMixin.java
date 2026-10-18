package meteordevelopment.meteorclient.mixin;

import com.lovenote.Lovenote;
import com.lovenote.module.impl.SkyShader;
import com.lovenote.util.LovenoteRender;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class LovenoteSkyRendererMixin {

        @Inject(method = "extractRenderState", at = @At("TAIL"))
        private void lovenote$claimSky(ClientLevel level, float partialTicks, Camera camera,
                                       SkyRenderState state, CallbackInfo ci) {
                if (Lovenote.modules == null) return;

                SkyShader sky = Lovenote.modules.get(SkyShader.class);
                if (sky == null || !sky.active()) return;

                LovenoteRender.claimSky();
                state.skyColor = 0;
        }
}

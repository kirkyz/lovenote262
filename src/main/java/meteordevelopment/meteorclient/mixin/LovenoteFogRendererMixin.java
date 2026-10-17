package meteordevelopment.meteorclient.mixin;

import com.lovenote.util.LovenoteRender;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FogRenderer.class)
public abstract class LovenoteFogRendererMixin {

        @ModifyVariable(method = "updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V",
                        at = @At("HEAD"), argsOnly = true, name = "fogColor")
        private Vector4f lovenote$fogColor(Vector4f fogColor) {
                if (!LovenoteRender.fogOverride) return fogColor;

                int c = LovenoteRender.fogColor;
                return new Vector4f(
                                ((c >> 16) & 255) / 255f,
                                ((c >> 8) & 255) / 255f,
                                (c & 255) / 255f,
                                1f);
        }
}

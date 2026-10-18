package meteordevelopment.meteorclient.mixin;

import com.lovenote.Lovenote;
import com.lovenote.module.impl.SwingAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LovenoteLivingEntityMixin {
        @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true, require = 0)
        private void lovenote$swingDuration(CallbackInfoReturnable<Integer> cir) {
                if (Lovenote.modules == null || (Object) this != Minecraft.getInstance().player) return;
                SwingAnimation sa = Lovenote.modules.get(SwingAnimation.class);
                if (sa != null && sa.enabled) cir.setReturnValue(Math.max(1, Math.round(cir.getReturnValue() / sa.speed.getF())));
        }
}

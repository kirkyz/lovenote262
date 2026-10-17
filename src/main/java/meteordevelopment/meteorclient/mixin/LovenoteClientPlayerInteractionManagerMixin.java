package meteordevelopment.meteorclient.mixin;

import com.lovenote.Lovenote;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class LovenoteClientPlayerInteractionManagerMixin {
        @Inject(method = "attack", at = @At("HEAD"))
        private void lovenote$onAttack(Player player, Entity target, CallbackInfo ci) {
                if (Lovenote.modules != null) Lovenote.modules.attack(target);
        }
}

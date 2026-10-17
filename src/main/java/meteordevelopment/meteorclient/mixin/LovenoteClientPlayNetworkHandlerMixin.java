package meteordevelopment.meteorclient.mixin;

import com.lovenote.Lovenote;
import com.lovenote.module.impl.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class LovenoteClientPlayNetworkHandlerMixin {

        @Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true)
        private void lovenote$velocity(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
                Minecraft mc = Minecraft.getInstance();
                if (Lovenote.modules == null || mc.player == null || packet.id() != mc.player.getId()) return;
                Velocity v = Lovenote.modules.get(Velocity.class);
                if (v == null || !v.enabled) return;

                ci.cancel();
                double h = v.horizontal.get() / 100.0, vv = v.vertical.get() / 100.0;
                if (h == 0 && vv == 0) return;
                Vec3 m = packet.movement();
                mc.execute(() -> {
                        if (mc.player != null) mc.player.lerpMotion(new Vec3(m.x * h, m.y * vv, m.z * h));
                });
        }

        @Inject(method = "handleEntityEvent", at = @At("HEAD"))
        private void lovenote$status(ClientboundEntityEventPacket packet, CallbackInfo ci) {
                if (packet.getEventId() != 35) return;
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                        if (mc.level == null || Lovenote.modules == null) return;
                        Entity e = packet.getEntity(mc.level);
                        if (e != null) Lovenote.modules.totemPop(e);
                });
        }
}

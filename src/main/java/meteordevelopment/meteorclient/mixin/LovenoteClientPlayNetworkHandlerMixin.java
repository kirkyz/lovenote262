package meteordevelopment.meteorclient.mixin;

import com.lovenote.Lovenote;
import com.lovenote.module.impl.Velocity;
import meteordevelopment.meteorclient.mixin.ClientboundSetEntityMotionPacketAccessor;
import meteordevelopment.meteorclient.systems.modules.Modules;
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

        @Inject(method = "handleSetEntityMotion", at = @At("HEAD"))
        private void lovenote$velocity(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
                Minecraft mc = Minecraft.getInstance();
                if (Lovenote.modules == null || mc.player == null || packet.id() != mc.player.getId()) return;
                Velocity v = Lovenote.modules.get(Velocity.class);
                if (v == null || !v.enabled) return;

                // The bundled Meteor Velocity module also edits this packet.
                // Never stack both modifiers: defer to Meteor if it is active.
                Modules meteorModules = Modules.get();
                if (meteorModules != null) {
                        meteordevelopment.meteorclient.systems.modules.movement.Velocity meteorVelocity =
                                meteorModules.get(meteordevelopment.meteorclient.systems.modules.movement.Velocity.class);
                        if (meteorVelocity != null && meteorVelocity.isActive()) return;
                }

                Vec3 current = mc.player.getDeltaMovement();
                Vec3 incoming = packet.movement();
                double h = v.horizontal.get() / 100.0;
                double vertical = v.vertical.get() / 100.0;
                Vec3 scaled = new Vec3(
                        current.x + (incoming.x - current.x) * h,
                        current.y + (incoming.y - current.y) * vertical,
                        current.z + (incoming.z - current.z) * h
                );
                ((ClientboundSetEntityMotionPacketAccessor) (Object) packet).meteor$setMovement(scaled);
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

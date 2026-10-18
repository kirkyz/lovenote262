package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;
import net.minecraft.core.BlockPos;
/** Jumps as the player moves towards a detected ledge. */
public final class Parkour extends Module {
    private final NumberSetting ahead = add(new NumberSetting("Look ahead", 1.0, .3, 2.0, .1));
    public Parkour() { super("Parkour", Category.MOVEMENT); describe("Automatically jump when walking towards a detected one-block ledge."); }
    @Override public void onTick() {
        if (mc.player == null || mc.level == null || mc.gui.screen() != null) return;
        if (!mc.player.onGround() || mc.player.isPassenger() || mc.options.keyShift.isDown()) return;
        if (!mc.options.keyUp.isDown()) return;
        var velocity = mc.player.getDeltaMovement();
        double vx = velocity.x, vz = velocity.z, speed = Math.hypot(vx, vz);
        if (speed < .03) return;
        double distance = Math.max(.3, ahead.get());
        BlockPos below = BlockPos.containing(mc.player.getX() + vx/speed*distance,
                    mc.player.getY() - .6, mc.player.getZ() + vz/speed*distance);
        if (mc.level.getBlockState(below).isAir() && mc.level.getBlockState(below.below()).isAir()) mc.player.jumpFromGround();
    }
}

package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;

/** Jump on landing while moving. Does not synthesize key presses. */
public final class AutoJump extends Module {
    private final BoolSetting requireForward = add(new BoolSetting("Require forward", true));

    public AutoJump() {
        super("AutoJump", Category.MOVEMENT);
        describe("Automatically jump on the ground while moving.");
    }

    @Override public void onTick() {
        if (mc.player == null || mc.level == null || mc.gui.screen() != null) return;
        if (requireForward.get() && !mc.options.keyUp.isDown()) return;
        if (mc.player.onGround() && !mc.player.isPassenger() && !mc.player.isInWater()
                && !mc.options.keyShift.isDown()) mc.player.jumpFromGround();
    }
}

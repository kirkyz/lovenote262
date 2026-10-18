package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;

/** Sprint while moving forward, respecting food and collisions. */
public final class AutoSprint extends Module {
    private final BoolSetting allowWater = add(new BoolSetting("Allow in water", false));

    public AutoSprint() {
        super("AutoSprint", Category.MOVEMENT);
        describe("Automatically sprint when moving forward and able to sprint.");
    }

    @Override public void onTick() {
        if (mc.player == null || mc.level == null || mc.gui.screen() != null) return;
        if (!mc.options.keyUp.isDown() || mc.options.keyShift.isDown()) return;
        if (!allowWater.get() && mc.player.isInWater()) return;
        if (mc.player.isUsingItem() || mc.player.isPassenger()) return;
        if (mc.player.getFoodData().getFoodLevel() <= 6 && !mc.player.getAbilities().mayfly) return;
        mc.player.setSprinting(true);
    }
}

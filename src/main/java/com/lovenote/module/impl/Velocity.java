package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;

/**
 * Client-side knockback scaling for the local player.
 * 0% cancels the incoming knockback impulse; 100% retains it.
 * Only attack/entity-motion knockback is handled here, not explosions or other pushes.
 */
public class Velocity extends Module {
    public final NumberSetting horizontal = add(new NumberSetting("Horizontal %", 0, 0, 100, 1));
    public final NumberSetting vertical = add(new NumberSetting("Vertical %", 0, 0, 100, 1));

    public Velocity() {
        super("Velocity", Category.COMBAT);
        describe("Scales incoming attack knockback (horizontal and vertical). Does not affect explosions.");
    }
}

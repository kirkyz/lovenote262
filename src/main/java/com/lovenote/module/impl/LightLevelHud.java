package com.lovenote.module.impl;

import com.lovenote.setting.BoolSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Light-level HUD with low-frequency sampling and no chunk searches. */
public final class LightLevelHud extends LegacyTextWidget {
    private final BoolSetting warning = add(new BoolSetting("Dim area warning", true));
    private int level, cooldown;
    public LightLevelHud() {
        super("LightLevelHud", .02, .61);
        describe("Shows local light level without repeatedly scanning nearby chunks.");
    }
    @Override public void onTick() {
        if(mc.level == null || mc.player == null) return;
        if(cooldown-- > 0) return;
        cooldown = 10;
        level = mc.level.getMaxLocalRawBrightness(mc.player.blockPosition());
    }
    @Override public void onHud(GuiGraphicsExtractor g, float dt) {
        if(mc.level == null || mc.player == null) return;
        drawText(g, "Light  " + level + "/15" + (warning.get() && level < 7 ? "  · dim" : ""));
    }
}

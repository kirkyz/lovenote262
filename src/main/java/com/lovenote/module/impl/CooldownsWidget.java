package com.lovenote.module.impl;

import com.lovenote.setting.BoolSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Attack recharge status. Does not claim to track all item or ability cooldowns. */
public final class CooldownsWidget extends LegacyTextWidget {
    private final BoolSetting showReady = add(new BoolSetting("Show when ready", false));

    public CooldownsWidget() {
        super("CooldownsWidget", 0.43, 0.79);
        describe("Display vanilla melee attack recharge percentage (not item ability cooldowns).");
    }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null) return;
        float charged = Math.max(0f, Math.min(1f, mc.player.getAttackStrengthScale(delta)));
        int percent = Math.round(charged * 100f);
        if (percent == 100 && !showReady.get() && !(mc.gui.screen() instanceof meteordevelopment.meteorclient.gui.screens.LovenoteModulesScreen hudScreen && hudScreen.isDrainerHudEditor())) return;
        drawText(g, "Attack recharge: " + percent + "%");
    }
}

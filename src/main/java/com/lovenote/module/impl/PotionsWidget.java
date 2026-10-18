package com.lovenote.module.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.effect.MobEffectInstance;

/** Compact names and durations for active player effects. */
public final class PotionsWidget extends LegacyTextWidget {
    public PotionsWidget() {
        super("PotionsWidget", 0.60, 0.30);
        describe("Show active potion effects with remaining seconds.");
    }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null) return;
        StringBuilder b = new StringBuilder();
        int count = 0;
        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            if (count++ > 0) b.append("  |  ");
            b.append(effect.getEffect().value().getDisplayName().getString());
            int seconds = effect.getDuration() / 20;
            if (!effect.isInfiniteDuration()) b.append(' ').append(seconds / 60).append(':').append(String.format("%02d", seconds % 60));
            if (count >= 3) break;
        }
        if (count > 0) drawText(g, b.toString());
    }
}

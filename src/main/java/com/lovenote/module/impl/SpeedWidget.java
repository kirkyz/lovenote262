package com.lovenote.module.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Ported text HUD from the original Lovenote, adapted to 26.2. */
public final class SpeedWidget extends LegacyTextWidget {
    public SpeedWidget() { super("SpeedWidget", 0.012, 0.82); describe("Legacy Lovenote HUD widget."); }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null) return;
        drawText(g, String.format(java.util.Locale.ROOT, "Speed %.2f b/s", Math.hypot(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().z) * 20.0));
    }
}

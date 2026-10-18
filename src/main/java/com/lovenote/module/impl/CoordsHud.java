package com.lovenote.module.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Ported text HUD from the original Lovenote, adapted to 26.2. */
public final class CoordsHud extends LegacyTextWidget {
    public CoordsHud() { super("CoordsHud", 0.012, 0.86); describe("Legacy Lovenote HUD widget."); }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null) return;
        drawText(g, String.format(java.util.Locale.ROOT, "XYZ %.1f / %.1f / %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
    }
}

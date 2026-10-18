package com.lovenote.module.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Ported text HUD from the original Lovenote, adapted to 26.2. */
public final class InfoHud extends LegacyTextWidget {
    public InfoHud() { super("InfoHud", 0.012, 0.78); describe("Legacy Lovenote HUD widget."); }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null) return;
        drawText(g, "FPS " + mc.getFps() + "  |  " + (mc.getCurrentServer() == null ? "Singleplayer" : mc.getCurrentServer().ip));
    }
}

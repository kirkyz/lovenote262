package com.lovenote.module.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;

/** 26.2 adaptation of the legacy PingHud, using the existing Drainer HUD shell. */
public final class PingHud extends LegacyTextWidget {
    private int shown = -1;

    public PingHud() {
        super("PingHud", 0.82, 0.92);
        describe("Connection ping with a smoothed on-screen readout.");
    }

    @Override
    public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null || mc.getConnection() == null) return;
        PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        if (info == null) {
            drawText(g, "Ping --");
            return;
        }
        int raw = info.getLatency();
        shown = shown < 0 ? raw : (int) Math.round(shown + (raw - shown) * 0.2);
        drawText(g, "Ping " + shown + " ms");
    }
}

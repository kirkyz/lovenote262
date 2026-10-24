package com.lovenote.module.impl;

import com.lovenote.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Lightweight count of players listed in the multiplayer tab. */
public final class TabList extends LegacyTextWidget {
    private final NumberSetting limit = add(new NumberSetting("Max listed", 6, 1, 12, 1));

    public TabList() {
        super("TabList", 0.38, 0.055);
        describe("Compact multiplayer tab overview (vanilla tab is unchanged).");
    }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null || mc.getConnection() == null || !mc.options.keyPlayerList.isDown()) return;
        StringBuilder message = new StringBuilder("Players ");
        var listed = mc.getConnection().getOnlinePlayers();
        message.append(listed.size()).append(": ");
        int count = 0;
        for (var info : listed) {
            if (count >= limit.getI()) break;
            if (count++ > 0) message.append(", ");
            message.append(info.getProfile().name());
        }
        drawText(g, message.toString());
    }
}

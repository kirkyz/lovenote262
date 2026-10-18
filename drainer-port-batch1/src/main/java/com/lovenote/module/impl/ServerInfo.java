package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;

/** Legacy server-address / latency / tab player count widget adapted to 26.2. */
public final class ServerInfo extends Module {
    private final BoolSetting players = add(new BoolSetting("Player count", true));
    private final BoolSetting ping = add(new BoolSetting("Ping", true));

    public ServerInfo() {
        super("ServerInfo", Category.HYPIXEL);
        alsoIn.add(Category.HUD);
        describe("Server, ping and player count HUD.");
    }

    @Override
    public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null) return;
        String server = mc.getCurrentServer() == null ? "Singleplayer" : mc.getCurrentServer().ip;
        StringBuilder detail = new StringBuilder();
        if (mc.getConnection() != null) {
            if (ping.get()) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if (info != null) detail.append(info.getLatency()).append(" ms");
            }
            if (players.get()) {
                if (!detail.isEmpty()) detail.append("  |  ");
                detail.append(mc.getConnection().getOnlinePlayers().size()).append(" players");
            }
        }
        int w = Math.max(Ui.textW(server), Ui.textW(detail.toString())) + 14;
        int x = 8, y = 8, h = detail.isEmpty() ? 21 : 33;
        Ui.pill(g, x, y, w, h);
        Ui.text(g, server, x + 7, y + 6, Ui.textCol);
        if (!detail.isEmpty()) Ui.text(g, detail.toString(), x + 7, y + 18, Ui.textDim);
    }
}

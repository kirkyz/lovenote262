package com.lovenote.module.impl;

import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Public tab-list title display only; cannot identify hidden staff or prove roles. */
public final class StaffList extends LegacyTextWidget {
    private final BoolSetting showEmpty = add(new BoolSetting("Show when none found", false));
    private final NumberSetting limit = add(new NumberSetting("Max names", 3, 1, 6, 1));
    private int scanTick;
    private String summary = "";

    public StaffList() {
        super("StaffList", 0.02, 0.77);
        describe("Display public staff-like tab-list labels only; not a vanish detector.");
    }
    @Override public void onTick() {
        if (mc.player == null || mc.getConnection() == null) { summary = ""; scanTick = 0; return; }
        if (++scanTick < 40) return;
        scanTick = 0;
        List<String> found = new ArrayList<>();
        for (var p : mc.getConnection().getOnlinePlayers()) {
            var title = p.getTabListDisplayName();
            if (title == null) continue;
            String line = title.getString().toLowerCase(Locale.ROOT);
            if (line.contains("[admin]") || line.contains("[mod]") || line.contains("[gm]")
                    || line.contains("[helper]")) {
                if (found.size() < limit.getI()) found.add(p.getProfile().name());
            }
        }
        summary = found.isEmpty() ? "" : "Public staff tags: " + String.join(", ", found);
    }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null || mc.getConnection() == null) return;
        if (summary.isEmpty() && !showEmpty.get() && !(mc.gui.screen() instanceof meteordevelopment.meteorclient.gui.screens.LovenoteModulesScreen hudScreen && hudScreen.isDrainerHudEditor())) return;
        drawText(g, summary.isEmpty() ? "Public staff tags: none listed" : summary);
    }
    @Override public void onDisable() { summary = ""; scanTick = 0; }
}

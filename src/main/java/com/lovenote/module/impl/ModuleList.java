package com.lovenote.module.impl;

import com.lovenote.Lovenote;
import com.lovenote.module.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.lovenote.setting.NumberSetting;

/** Compact read-only module overview; avoids changes to the existing click GUI. */
public final class ModuleList extends LegacyTextWidget {
    private final NumberSetting maximum = add(new NumberSetting("Max shown", 8, 1, 20, 1));
    public ModuleList() {
        super("ModuleList", 0.75, 0.05);
        describe("Names of enabled Lovenote modules in a compact HUD strip.");
    }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (Lovenote.modules == null) return;
        StringBuilder names = new StringBuilder();
        int count = 0;
        for (Module m : Lovenote.modules.all()) {
            if (!m.enabled || m == this || m.hidden) continue;
            if (count == maximum.getI()) break;
            if (count++ > 0) names.append(" · ");
            names.append(m.name);
        }
        if (count > 0) drawText(g, names.toString());
    }
}

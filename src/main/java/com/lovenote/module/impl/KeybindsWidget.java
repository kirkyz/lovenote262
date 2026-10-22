package com.lovenote.module.impl;

import com.lovenote.Lovenote;
import com.lovenote.module.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.Locale;

/** HUD of enabled user-bound modules. */
public final class KeybindsWidget extends LegacyTextWidget {
    public KeybindsWidget() {
        super("KeybindsWidget", 0.60, 0.13);
        describe("Display active modules with their configured key codes.");
    }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (Lovenote.modules == null) return;
        int n = 0;
        StringBuilder b = new StringBuilder("Binds: ");
        for (Module m : Lovenote.modules.all()) {
            if (m == this || !m.enabled || m.key < 0) continue;
            if (n++ > 0) b.append(" · ");
            b.append(m.name).append("[").append(m.key).append("]");
            if (n >= 6) break;
        }
        if (n > 0) drawText(g, b.toString());
        else if (mc.gui.screen() instanceof meteordevelopment.meteorclient.gui.screens.LovenoteModulesScreen hudScreen && hudScreen.isDrainerHudEditor()) drawText(g, "Keybinds  |  No active binds");
    }
}

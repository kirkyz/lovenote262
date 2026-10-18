package com.lovenote.module.impl;

import com.lovenote.setting.ModeSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A simple watermark that follows the existing Drainer HUD theme. */
public final class Watermark extends LegacyTextWidget {
    private final ModeSetting style = add(new ModeSetting("Style", "Drainer", "Drainer", "Lovenote"));

    public Watermark() {
        super("Watermark", 0.015, 0.025);
        describe("Minimal themed client watermark.");
    }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        drawText(g, style.get().toUpperCase(java.util.Locale.ROOT) + "  •  26.2");
    }
}

package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Common positioning and frame rendering for ported 1.21 text HUD widgets. */
abstract class LegacyTextWidget extends Module {
    protected final NumberSetting x;
    protected final NumberSetting y;
    protected final BoolSetting frame = add(new BoolSetting("Frame", true));
    protected LegacyTextWidget(String name, double px, double py) {
        super(name, Category.HUD);
        x = add(new NumberSetting("X", px, 0, 0.98, 0.005));
        y = add(new NumberSetting("Y", py, 0, 0.98, 0.005));
    }
    protected final void drawText(GuiGraphicsExtractor g, String value) {
        if (value == null || value.isEmpty()) return;
        int w = Ui.textW(value) + 12, h = 20;
        int px = Math.max(0, Math.min((int)(x.get() * Gfx.guiW()), Math.max(0, Gfx.guiW() - w)));
        int py = Math.max(0, Math.min((int)(y.get() * Gfx.guiH()), Math.max(0, Gfx.guiH() - h)));
        if (frame.get()) Ui.pill(g, px, py, w, h);
        Ui.text(g, value, px + 6, py + 6, Ui.textCol);
    }
}

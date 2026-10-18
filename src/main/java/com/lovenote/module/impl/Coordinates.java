package com.lovenote.module.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.Locale;

/** Ported world-position widget with draggable-style X/Y options from LegacyTextWidget. */
public final class Coordinates extends LegacyTextWidget {
    public Coordinates() {
        super("Coordinates", 0.20, 0.39);
        describe("Compact XYZ HUD, editable X/Y layout coordinates.");
    }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null) return;
        drawText(g, String.format(Locale.ROOT, "x %d   y %d   z %d",
            (int)Math.floor(mc.player.getX()), (int)Math.floor(mc.player.getY()), (int)Math.floor(mc.player.getZ())));
    }
}

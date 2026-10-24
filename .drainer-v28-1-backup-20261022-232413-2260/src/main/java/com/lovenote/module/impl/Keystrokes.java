package com.lovenote.module.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Noninteractive WASD / sprint / jump keyboard-state HUD. */
public final class Keystrokes extends LegacyTextWidget {
    public Keystrokes() {
        super("Keystrokes", 0.79, 0.77);
        describe("Display movement-key presses on the HUD.");
    }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null || mc.options == null) return;
        String keys = (mc.options.keyUp.isDown() ? "W" : "-") + " "
                + (mc.options.keyLeft.isDown() ? "A" : "-") + " "
                + (mc.options.keyDown.isDown() ? "S" : "-") + " "
                + (mc.options.keyRight.isDown() ? "D" : "-");
        if (mc.options.keyJump.isDown()) keys += "  SPACE";
        if (mc.options.keySprint.isDown()) keys += "  SPRINT";
        drawText(g, keys);
    }
}

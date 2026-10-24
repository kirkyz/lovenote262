package com.lovenote.module.impl;

import com.lovenote.setting.BoolSetting;
import com.lovenote.util.HudGlass;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Claude HUD design translated to native key-state tiles with real input data. */
public final class Keystrokes extends LegacyTextWidget {
    private final BoolSetting mouse = add(new BoolSetting("Mouse buttons", true));
    private final BoolSetting sprint = add(new BoolSetting("Sprint indicator", false));

    public Keystrokes() {
        super("Keystrokes", 0.79, 0.77);
        describe("Draggable native WASD, jump and mouse HUD, with live key state.");
    }

    private void key(GuiGraphicsExtractor g, int px, int py, int w, int h, String label, boolean pressed) {
        HudGlass.pill(g, px, py, w, h,
                pressed ? HudGlass.accent() : 0x73323B51);
        int tx = px + (w - Ui.textW(label)) / 2;
        Ui.text(g, label, tx, py + (h - 8) / 2,
                pressed ? 0xFFFFFFFF : 0xFFBFC7D8);
    }

    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null || mc.options == null) return;
        boolean small = compact.get();
        int cell = small ? 27 : 33;
        int gap = small ? 4 : 5;
        int frameW = cell * 3 + gap * 2 + 20;
        int frameH = small ? 123 : 146;
        drawCustomCard(g, frameW, frameH);
        int x = editorX(), y = editorY();
        Ui.text(g, "KEYSTROKES", x + 10, y + 7, HudGlass.muted());
        int left = x + 10, firstY = y + 23;
        key(g, left + cell + gap, firstY, cell, cell, "W", mc.options.keyUp.isDown());
        int secondY = firstY + cell + gap;
        key(g, left, secondY, cell, cell, "A", mc.options.keyLeft.isDown());
        key(g, left + cell + gap, secondY, cell, cell, "S", mc.options.keyDown.isDown());
        key(g, left + 2 * (cell + gap), secondY, cell, cell, "D", mc.options.keyRight.isDown());
        int barY = secondY + cell + gap;
        boolean sprinting = sprint.get() && mc.options.keySprint.isDown();
        key(g, left, barY, cell * 3 + gap * 2, small ? 14 : 17,
                sprinting ? "SPRINT" : "SPACE",
                sprinting || mc.options.keyJump.isDown());
        if (mouse.get()) {
            int rowY = barY + (small ? 17 : 21);
            int allW = cell * 3 + gap * 2;
            int half = (allW - gap) / 2;
            key(g, left, rowY, half, small ? 13 : 15, "LMB", mc.options.keyAttack.isDown());
            key(g, left + half + gap, rowY, allW - half - gap, small ? 13 : 15, "RMB", mc.options.keyUse.isDown());
        }
    }
}

package com.lovenote.module.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Keybinding-based CPS approximation, sampled every game tick. */
public final class CpsHud extends LegacyTextWidget {
    private final boolean[] left = new boolean[20];
    private final boolean[] right = new boolean[20];
    private int index;
    private boolean lastLeft, lastRight;
    public CpsHud() {
        super("CpsHud", 0.44, 0.88);
        describe("Left/right clicks per second (20 tick samples, keybind based).");
    }
    @Override public void onTick() {
        if (mc.options == null || mc.player == null) {
            lastLeft = false; lastRight = false;
            java.util.Arrays.fill(left, false); java.util.Arrays.fill(right, false);
            return;
        }
        boolean l = mc.options.keyAttack.isDown();
        boolean r = mc.options.keyUse.isDown();
        left[index] = l && !lastLeft;
        right[index] = r && !lastRight;
        index = (index + 1) % left.length;
        lastLeft = l; lastRight = r;
    }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null) return;
        int lc = 0, rc = 0;
        for (boolean event : left) if (event) lc++;
        for (boolean event : right) if (event) rc++;
        drawText(g, "L " + lc + "  /  R " + rc + " CPS");
    }
}

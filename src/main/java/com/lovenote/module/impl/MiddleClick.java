package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

/** Coordinate helper. Does not cancel or alter vanilla pick-block input. */
public final class MiddleClick extends Module {
    private final BoolSetting requireSneak = add(new BoolSetting("Require sneak + middle click", true));
    private boolean down;

    public MiddleClick() {
        super("MiddleClick", Category.MISC);
        describe("Copy targeted block XYZ on sneak + middle click; keeps vanilla pick-block.");
        alsoIn.add(Category.HUD);
    }
    @Override public void onTick() {
        if (mc.options == null || mc.player == null || mc.gui.screen() != null) { down = false; return; }
        boolean pressed = mc.options.keyPickItem.isDown();
        boolean risingEdge = pressed && !down;
        down = pressed;
        if (!risingEdge || (requireSneak.get() && !mc.options.keyShift.isDown())) return;
        if (!(mc.hitResult instanceof BlockHitResult target)) return;
        BlockPos pos = target.getBlockPos();
        String coordinates = pos.getX() + " " + pos.getY() + " " + pos.getZ();
        mc.keyboardHandler.setClipboard(coordinates);
        // Clipboard copy is the user-visible action; avoid the removed Gui overlay API.
    }
    @Override public void onDisable() { down = false; }
}

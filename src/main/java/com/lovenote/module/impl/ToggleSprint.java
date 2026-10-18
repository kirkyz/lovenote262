package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;

/** Uses the built-in vanilla toggle-sprint option, rather than forcing movement. */
public final class ToggleSprint extends Module {
    private Boolean previous;
    public ToggleSprint() {
        super("ToggleSprint", Category.PLAYER);
        describe("Use Minecraft's native toggle-to-sprint key behaviour.");
    }
    @Override public void onEnable() {
        if (mc.options == null) return;
        if (previous == null) previous = mc.options.toggleSprint().get();
        mc.options.toggleSprint().set(true);
    }
    @Override public void onTick() {
        if (mc.options != null && !mc.options.toggleSprint().get()) mc.options.toggleSprint().set(true);
    }
    @Override public void onDisable() {
        if (previous != null && mc.options != null) mc.options.toggleSprint().set(previous);
        previous = null;
    }
}

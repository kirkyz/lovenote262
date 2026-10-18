package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;

/** Disables vanilla vignette (newer video setting). */
public final class Vignette extends Module {
    private Boolean previous;
    public Vignette() {
        super("Vignette", Category.VISUALS);
        describe("Hide the vanilla screen-edge vignette.");
    }
    @Override public void onEnable() {
        if (mc.options == null) return;
        if (previous == null) previous = mc.options.vignette().get();
        apply();
    }
    @Override public void onTick() { apply(); }
    private void apply() {
        if (mc.options != null && mc.options.vignette().get()) mc.options.vignette().set(false);
    }
    @Override public void onDisable() {
        if (previous != null && mc.options != null) mc.options.vignette().set(previous);
        previous = null;
    }
}

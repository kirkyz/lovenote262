package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;

/** Disables the vanilla walking-camera bob effect. */
public final class NoViewBob extends Module {
    private Boolean previous;
    public NoViewBob() {
        super("NoViewBob", Category.RENDER);
        describe("Remove camera bob while walking.");
    }
    @Override public void onEnable() {
        if (mc.options == null) return;
        if (previous == null) previous = mc.options.bobView().get();
        apply();
    }
    @Override public void onTick() { apply(); }
    private void apply() {
        if (mc.options != null && mc.options.bobView().get()) mc.options.bobView().set(false);
    }
    @Override public void onDisable() {
        if (previous != null && mc.options != null) mc.options.bobView().set(previous);
        previous = null;
    }
}

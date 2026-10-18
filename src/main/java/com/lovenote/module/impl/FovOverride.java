package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;

/** Keeps a selected FOV until disabled. */
public final class FovOverride extends Module {
    private final NumberSetting fov = add(new NumberSetting("FOV", 90, 30, 110, 1));
    private Integer previous;

    public FovOverride() {
        super("FovOverride", Category.RENDER);
        describe("Lock your field of view and restore your original FOV on disable.");
    }
    @Override public void onEnable() {
        if (mc.options == null) return;
        if (previous == null) previous = mc.options.fov().get();
        apply();
    }
    @Override public void onTick() { apply(); }
    private void apply() {
        if (mc.options == null) return;
        if (mc.options.fov().get() != fov.getI()) mc.options.fov().set(fov.getI());
    }
    @Override public void onDisable() {
        if (previous != null && mc.options != null) mc.options.fov().set(previous);
        previous = null;
    }
}

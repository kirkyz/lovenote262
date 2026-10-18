package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;

/** Client-side FPS cap. Restores the previous cap on disable. */
public final class MaxFps extends Module {
    private final NumberSetting fps = add(new NumberSetting("FPS", 144, 30, 500, 5));
    private Integer previous;

    public MaxFps() {
        super("MaxFps", Category.RENDER);
        describe("Override the maximum FPS; restores the old value when disabled.");
    }
    @Override public void onEnable() {
        if (mc.options == null) return;
        if (previous == null) previous = mc.options.framerateLimit().get();
        apply();
    }
    @Override public void onTick() { apply(); }
    private void apply() {
        if (mc.options == null) return;
        if (mc.options.framerateLimit().get() != fps.getI()) mc.options.framerateLimit().set(fps.getI());
    }
    @Override public void onDisable() {
        if (previous != null && mc.options != null) mc.options.framerateLimit().set(previous);
        previous = null;
    }
}

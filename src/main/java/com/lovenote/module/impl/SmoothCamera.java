package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;

/** Toggles vanilla smooth camera. */
public final class SmoothCamera extends Module {
    private Boolean previous;
    public SmoothCamera() {
        super("SmoothCamera", Category.RENDER);
        describe("Enable vanilla cinematic smooth camera movement.");
    }
    @Override public void onEnable() {
        if (mc.options == null) return;
        if (previous == null) previous = mc.options.smoothCamera;
        mc.options.smoothCamera = true;
    }
    @Override public void onTick() {
        if (mc.options != null) mc.options.smoothCamera = true;
    }
    @Override public void onDisable() {
        if (previous != null && mc.options != null) mc.options.smoothCamera = previous;
        previous = null;
    }
}

package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.ModeSetting;
import net.minecraft.client.CloudStatus;

/** Vanilla cloud quality override. */
public final class Clouds extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Off", "Off", "Fast", "Fancy"));
    private CloudStatus previous;

    public Clouds() {
        super("Clouds", Category.RENDER);
        describe("Choose vanilla cloud style.");
    }
    @Override public void onEnable() {
        if (mc.options == null) return;
        if (previous == null) previous = mc.options.cloudStatus().get();
        apply();
    }
    @Override public void onTick() { apply(); }
    private void apply() {
        if (mc.options == null) return;
        CloudStatus value = mode.is("Fancy") ? CloudStatus.FANCY : mode.is("Fast") ? CloudStatus.FAST : CloudStatus.OFF;
        if (mc.options.cloudStatus().get() != value) mc.options.cloudStatus().set(value);
    }
    @Override public void onDisable() {
        if (previous != null && mc.options != null) mc.options.cloudStatus().set(previous);
        previous = null;
    }
}

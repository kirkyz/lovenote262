package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;

/** Affects vanilla chat text opacity, not your custom UI. */
public final class ChatOpacity extends Module {
    private final NumberSetting opacity = add(new NumberSetting("Opacity %", 100, 0, 100, 5));
    private Double previous;

    public ChatOpacity() {
        super("ChatOpacity", Category.RENDER);
        describe("Adjust vanilla chat text opacity; restore on disable.");
    }
    @Override public void onEnable() {
        if (mc.options == null) return;
        if (previous == null) previous = mc.options.chatOpacity().get();
        apply();
    }
    @Override public void onTick() { apply(); }
    private void apply() {
        if (mc.options == null) return;
        double target = opacity.get() / 100.0;
        if (Math.abs(mc.options.chatOpacity().get() - target) > 0.0001) mc.options.chatOpacity().set(target);
    }
    @Override public void onDisable() {
        if (previous != null && mc.options != null) mc.options.chatOpacity().set(previous);
        previous = null;
    }
}

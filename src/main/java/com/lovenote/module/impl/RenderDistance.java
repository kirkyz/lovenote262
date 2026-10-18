package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;

/** Client view-distance setting; server visibility limits still apply. */
public final class RenderDistance extends Module {
    private final NumberSetting chunks = add(new NumberSetting("Chunks", 12, 2, 32, 1));
    private Integer previous;

    public RenderDistance() {
        super("RenderDistance", Category.RENDER);
        describe("Client chunk render distance (subject to server limits).");
    }
    @Override public void onEnable() {
        if (mc.options == null) return;
        if (previous == null) previous = mc.options.renderDistance().get();
        apply();
    }
    @Override public void onTick() { apply(); }
    private void apply() {
        if (mc.options == null) return;
        if (mc.options.renderDistance().get() != chunks.getI()) mc.options.renderDistance().set(chunks.getI());
    }
    @Override public void onDisable() {
        if (previous != null && mc.options != null) mc.options.renderDistance().set(previous);
        previous = null;
    }
}

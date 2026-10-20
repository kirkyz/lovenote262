package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;

/** Uses the vanilla biome blend radius option, no per-tick block sampling. */
public final class BiomeBlend extends Module {
    private final NumberSetting radius = add(new NumberSetting("Blend radius", 2, 0, 7, 1));
    private Integer before;

    public BiomeBlend() {
        super("BiomeBlend", Category.RENDER);
        describe("Set vanilla biome-colour blend radius (lower values can help FPS).");
    }
    @Override public void onEnable() {
        if (mc.options == null) return;
        if (before == null) before = mc.options.biomeBlendRadius().get();
        apply();
    }
    @Override public void onTick() { apply(); }
    private void apply() {
        if (mc.options == null) return;
        if (mc.options.biomeBlendRadius().get() != radius.getI())
            mc.options.biomeBlendRadius().set(radius.getI());
    }
    @Override public void onDisable() {
        if (before != null && mc.options != null) mc.options.biomeBlendRadius().set(before);
        before = null;
    }
}

package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;

/** Vanilla option wrapper: no new shaders, draw calls or entity scans. */
public final class EntityShadows extends Module {
    private final BoolSetting show = add(new BoolSetting("Draw shadows", false));
    private Boolean before;

    public EntityShadows() {
        super("EntityShadows", Category.RENDER);
        describe("Show/hide vanilla entity shadows; remembers your original preference.");
    }
    @Override public void onEnable() {
        if (mc.options == null) return;
        if (before == null) before = mc.options.entityShadows().get();
        apply();
    }
    @Override public void onTick() { apply(); }
    private void apply() {
        if (mc.options != null && mc.options.entityShadows().get() != show.get())
            mc.options.entityShadows().set(show.get());
    }
    @Override public void onDisable() {
        if (before != null && mc.options != null) mc.options.entityShadows().set(before);
        before = null;
    }
}

package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.ModeSetting;
import net.minecraft.server.level.ParticleStatus;

/** Port of the legacy particle limiter using vanilla 26.2 options, not per-frame rendering. */
public final class Particles extends Module {
    private final ModeSetting amount = add(new ModeSetting("Vanilla particles", "Reduced", "All", "Reduced", "Minimal"));
    private ParticleStatus before;

    public Particles() {
        super("Particles", Category.RENDER);
        describe("Limit vanilla particles for smoother FPS; restores the old setting on disable.");
    }

    @Override public void onEnable() {
        if (mc.options == null) return;
        if (before == null) before = mc.options.particles().get();
        apply();
    }
    @Override public void onTick() { apply(); }
    private void apply() {
        if (mc.options == null) return;
        ParticleStatus wanted = amount.is("Minimal") ? ParticleStatus.MINIMAL
                : amount.is("All") ? ParticleStatus.ALL : ParticleStatus.DECREASED;
        if (mc.options.particles().get() != wanted) mc.options.particles().set(wanted);
    }
    @Override public void onDisable() {
        if (before != null && mc.options != null) mc.options.particles().set(before);
        before = null;
    }
}

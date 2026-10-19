package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;

/** Adaptive budget for expensive optional visual effects. Does not change vanilla graphics. */
public final class VisualOptimizer extends Module {
    private final ModeSetting quality = add(new ModeSetting("Effect quality", "Balanced", "Performance", "Balanced", "Quality"));
    private final BoolSetting adaptive = add(new BoolSetting("Adaptive FPS guard", true));
    private final NumberSetting targetFps = add(new NumberSetting("FPS target", 75, 30, 240, 5));
    private static float factor = 1.0f;
    private long nextSample;

    public VisualOptimizer() {
        super("VisualOptimizer", Category.VISUALS);
        describe("Reduce optional Drainer particle and tracer density when FPS drops; doesn't disable modules.");
    }
    @Override public void onEnable() { nextSample = 0; factor = 0.75f; }
    @Override public void onDisable() { factor = 1.0f; }
    @Override public void onTick() {
        long now = System.currentTimeMillis();
        if (now < nextSample) return;
        nextSample = now + 1100;
        float base = quality.is("Performance") ? 0.35f : quality.is("Quality") ? 1f : 0.70f;
        float desired = base;
        if (adaptive.get() && mc.player != null) {
            int fps = mc.getFps();
            if (fps > 0 && fps < targetFps.getI() * 0.70f) desired *= 0.45f;
            else if (fps > 0 && fps < targetFps.getI()) desired *= 0.7f;
        }
        factor = Math.max(0.12f, Math.min(1f, factor * .70f + desired * .30f));
    }
    public static int budget(int requested, int minimum) {
        return Math.min(requested, Math.max(minimum, Math.round(requested * factor)));
    }
}

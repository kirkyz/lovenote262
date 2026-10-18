package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;
/** Reversible FOV zoom; bind the module to a key in Drainer. */
public final class Zoom extends Module {
    private final NumberSetting fov = add(new NumberSetting("FOV", 30, 15, 75, 1));
    private Integer previous;
    public Zoom() { super("Zoom", Category.VISUALS); describe("Hold/toggle a reduced FOV, restoring your own setting when disabled."); }
    @Override public void onEnable() {
        if (mc.options != null && previous == null) previous = mc.options.fov().get();
        apply();
    }
    @Override public void onTick() { apply(); }
    private void apply() { if (mc.options != null && mc.options.fov().get() != fov.getI()) mc.options.fov().set(fov.getI()); }
    @Override public void onDisable() {
        if (previous != null && mc.options != null) mc.options.fov().set(previous);
        previous = null;
    }
}

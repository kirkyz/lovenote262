package com.lovenote.module.impl;
import com.lovenote.Lovenote;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
/** Disables active non-HUD Lovenote modules when toggled. */
public final class Panic extends Module {
    public Panic() { super("Panic", Category.MISC); describe("Turn off all active non-HUD Lovenote modules."); }
    @Override public void onEnable() {
        if (Lovenote.modules != null) {
            for (Module module : Lovenote.modules.all()) {
                if (module != this && module.category != Category.HUD && module.enabled) module.setEnabled(false);
            }
        }
        setEnabled(false);
    }
}

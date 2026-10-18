package com.lovenote.module;

/**
 * Visible registry backbone for an old Lovenote feature that still requires a 26.2 port.
 * It cannot be enabled until the old implementation is actually adapted and tested.
 */
public abstract class UnportedModule extends Module {
    protected UnportedModule(String name, Category category) {
        super(name, category);
        describe("NOT PORTED: legacy feature awaiting Minecraft 26.2 implementation.");
    }

    @Override
    public final void setEnabled(boolean enabled) {
        if (!enabled) super.setEnabled(false);
        // Deliberately refuse enabling unfinished behavior.
    }
}

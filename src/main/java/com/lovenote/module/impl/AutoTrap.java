package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.UnportedModule;

/** Registry-only placeholder. Legacy source: legacy-1.21-reference/AutoTrap.java */
public final class AutoTrap extends UnportedModule {
    public AutoTrap() {
        super("AutoTrap", Category.COMBAT);
        alsoIn.add(Category.HYPIXEL);
    }
}

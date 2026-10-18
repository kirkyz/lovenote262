package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.UnportedModule;

/** Registry-only placeholder. Legacy source: legacy-1.21-reference/OldCombat.java */
public final class OldCombat extends UnportedModule {
    public OldCombat() {
        super("OldCombat", Category.COMBAT);
        alsoIn.add(Category.HYPIXEL);
    }
}

package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.UnportedModule;

/** Registry-only placeholder. Legacy source: legacy-1.21-reference/Criticals.java */
public final class Criticals extends UnportedModule {
    public Criticals() {
        super("Criticals", Category.COMBAT);
        alsoIn.add(Category.HYPIXEL);
    }
}

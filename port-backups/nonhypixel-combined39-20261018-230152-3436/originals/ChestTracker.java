package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.UnportedModule;

/** Registry-only placeholder. Legacy source: legacy-1.21-reference/ChestTracker.java */
public final class ChestTracker extends UnportedModule {
    public ChestTracker() {
        super("ChestTracker", Category.MISC);
        alsoIn.add(Category.DONUT);
    }
}

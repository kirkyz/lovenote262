package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.UnportedModule;

/** Registry-only placeholder. Legacy source: legacy-1.21-reference/AutoFish.java */
public final class AutoFish extends UnportedModule {
    public AutoFish() {
        super("AutoFish", Category.HYPIXEL);
        alsoIn.add(Category.DONUT);
    }
}

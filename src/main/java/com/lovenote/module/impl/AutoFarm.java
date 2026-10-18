package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.UnportedModule;

/** Registry-only placeholder. Legacy source: legacy-1.21-reference/AutoFarm.java */
public final class AutoFarm extends UnportedModule {
    public AutoFarm() {
        super("AutoFarm", Category.HYPIXEL);
        alsoIn.add(Category.DONUT);
    }
}

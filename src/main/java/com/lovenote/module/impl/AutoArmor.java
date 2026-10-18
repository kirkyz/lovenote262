package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.UnportedModule;

/** Registry-only placeholder. Legacy source: legacy-1.21-reference/AutoArmor.java */
public final class AutoArmor extends UnportedModule {
    public AutoArmor() {
        super("AutoArmor", Category.COMBAT);
        alsoIn.add(Category.DONUT);
    }
}

package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.UnportedModule;

/** Registry-only placeholder. Legacy source: legacy-1.21-reference/AutoWeb.java */
public final class AutoWeb extends UnportedModule {
    public AutoWeb() {
        super("AutoWeb", Category.COMBAT);
        alsoIn.add(Category.HYPIXEL);
    }
}

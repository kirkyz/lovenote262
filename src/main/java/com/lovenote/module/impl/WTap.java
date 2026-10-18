package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.UnportedModule;

/** Registry-only placeholder. Legacy source: legacy-1.21-reference/WTap.java */
public final class WTap extends UnportedModule {
    public WTap() {
        super("WTap", Category.COMBAT);
        alsoIn.add(Category.HYPIXEL);
    }
}

package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.UnportedModule;

/** Registry-only placeholder. Legacy source: legacy-1.21-reference/AutoTool.java */
public final class AutoTool extends UnportedModule {
    public AutoTool() {
        super("AutoTool", Category.HYPIXEL);
        alsoIn.add(Category.DONUT);
    }
}

package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.ModeSetting;

/** A persistent server-profile selector. Old module category IDs remain unchanged for saved configs. */
public final class ServerModules extends Module {
    public final ModeSetting server = add(new ModeSetting("Server", "CubeCraft", "Donut", "Hypixel", "CubeCraft"));
    public ServerModules() {
        super("Server Profile", Category.SERVER_MODULES);
        describe("Select Donut, Hypixel, or CubeCraft to show only that server's tools on this page.");
        enabled = true;
    }
    public Category selectedCategory() {
        return switch (server.get()) {
            case "Donut" -> Category.DONUT;
            case "Hypixel" -> Category.HYPIXEL;
            default -> Category.SERVER_MODULES;
        };
    }
}

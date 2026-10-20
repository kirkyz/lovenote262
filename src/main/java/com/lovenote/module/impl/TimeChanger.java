package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import net.minecraft.client.multiplayer.ClientLevel;

/** Local time override; the server's actual clock is never modified. */
public final class TimeChanger extends Module {
    private final ModeSetting preset = add(new ModeSetting("Sky time", "Sunset", "Sunrise", "Noon", "Sunset", "Midnight", "Custom"));
    private final NumberSetting ticks = add(new NumberSetting("Custom day ticks", 12000, 0, 23999, 100));
    private final BoolSetting freeze = add(new BoolSetting("Freeze time", true));
    private ClientLevel level;
    private long originalTime, ticksEnabled;

    public TimeChanger() {
        super("TimeChanger", Category.RENDER);
        describe("Display a custom local day/night time, with presets and optional running clock.");
    }
    @Override public void onEnable() { level = null; ticksEnabled = 0; }
    @Override public void onTick() {
        if(mc.level == null) { level = null; return; }
        if(level != mc.level) {
            level = mc.level;
            originalTime = level.getGameTime();
            ticksEnabled = 0;
        }
        long base = switch(preset.get()) {
            case "Sunrise" -> 0;
            case "Noon" -> 6000;
            case "Midnight" -> 18000;
            case "Custom" -> ticks.getI();
            default -> 12000;
        };
        // Minecraft's client-side level data is used to render its day/night cycle.
        level.getLevelData().setGameTime(base + (freeze.get() ? 0 : ticksEnabled));
        ticksEnabled++;
    }
    @Override public void onDisable() {
        if(level != null && level == mc.level) level.getLevelData().setGameTime(originalTime + ticksEnabled);
        level = null;
    }
}

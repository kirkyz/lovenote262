package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import net.minecraft.client.multiplayer.ClientLevel;

/** Client-side local weather override, inspired by existing Meteor weather controls. */
public final class Weather extends Module {
    private final ModeSetting mode = add(new ModeSetting("Weather preset", "Clear", "Clear", "Drizzle", "Rain", "Thunder", "Custom"));
    private final NumberSetting customRain = add(new NumberSetting("Custom rain", 0.7, 0, 1, 0.05));
    private final NumberSetting customThunder = add(new NumberSetting("Custom thunder", 0, 0, 1, 0.05));
    private final BoolSetting instant = add(new BoolSetting("Instant transitions", false));
    private final NumberSetting smoothing = add(new NumberSetting("Transition speed", 0.15, 0.05, 1, 0.05));
    private ClientLevel trackedLevel;
    private float originalRain, originalThunder;

    public Weather() {
        super("Weather", Category.RENDER);
        describe("Override visible local rain and thunder with presets and smooth transitions. Does not change server weather.");
    }
    @Override public void onEnable() { trackedLevel = null; }
    @Override public void onTick() {
        if(mc.level == null) { trackedLevel = null; return; }
        if(trackedLevel != mc.level) {
            trackedLevel = mc.level;
            originalRain = trackedLevel.getRainLevel(1f);
            originalThunder = trackedLevel.getThunderLevel(1f);
        }
        float rain = switch(mode.get()) {
            case "Drizzle" -> 0.28f;
            case "Rain", "Thunder" -> 1f;
            case "Custom" -> customRain.getF();
            default -> 0f;
        };
        float thunder = mode.is("Thunder") ? 1f : mode.is("Custom") ? customThunder.getF() : 0f;
        float factor = instant.get() ? 1f : smoothing.getF();
        trackedLevel.setRainLevel(trackedLevel.getRainLevel(1f) + (rain - trackedLevel.getRainLevel(1f)) * factor);
        trackedLevel.setThunderLevel(trackedLevel.getThunderLevel(1f) + (thunder - trackedLevel.getThunderLevel(1f)) * factor);
    }
    @Override public void onDisable() {
        if(trackedLevel != null && trackedLevel == mc.level) {
            trackedLevel.setRainLevel(originalRain);
            trackedLevel.setThunderLevel(originalThunder);
        }
        trackedLevel = null;
    }
}

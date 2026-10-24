package com.lovenote.module.impl;

import com.lovenote.Lovenote;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Opt-in convenience grouping over EXISTING registered modules; no duplicate handlers. */
public final class VisualCollection extends Module {
    private final ModeSetting preset = add(new ModeSetting("Collection", "Cosmic", "Cosmic", "Particles", "Combat FX", "Atmosphere", "Everything"));
    private final BoolSetting includeSky = add(new BoolSetting("Include GPU sky", true));
    private final Map<Module, Boolean> original = new IdentityHashMap<>();
    private String activePreset = "";

    public VisualCollection() {
        super("VisualCollection", Category.VISUALS);
        describe("Opt-in visual collection: enables existing Drainer FX without duplicating modules. Disable to restore previous toggles.");
    }
    @Override public void onEnable() { activePreset = ""; apply(); }
    @Override public void onTick() {
        String key = preset.get() + ":" + includeSky.get();
        if (!key.equals(activePreset)) apply();
    }
    @Override public void onDisable() { restore(); activePreset = ""; }

    private void restore() {
        for (var entry : original.entrySet()) {
            Module m = entry.getKey();
            if (m != this && m.enabled != entry.getValue()) m.setEnabled(entry.getValue());
        }
        original.clear();
    }
    private void apply() {
        if (Lovenote.modules == null) return;
        restore();
        Set<String> names = switch (preset.get()) {
            case "Particles" -> Set.of("CubeField", "RibbonTrail", "SparkBurst", "PrismRain", "JumpCircles");
            case "Combat FX" -> Set.of("HitParticles", "HitMarker", "KillEffects", "TotemSpirit", "Glow");
            case "Atmosphere" -> Set.of("AuroraVeil", "GroundFog", "ColorGrading", "Starfield");
            case "Everything" -> Set.of("CubeField", "RibbonTrail", "SparkBurst", "PrismRain", "JumpCircles", "HitParticles", "HitMarker", "KillEffects", "TotemSpirit", "Glow", "AuroraVeil", "GroundFog", "ColorGrading", "Starfield", "SkyShader");
            default -> Set.of("AuroraVeil", "GroundFog", "Starfield", "SkyShader");
        };
        for (Module m : Lovenote.modules.all()) {
            if (m == this || !names.contains(m.name) || (m.name.equals("SkyShader") && !includeSky.get())) continue;
            original.put(m, m.enabled);
            if (!m.enabled) m.setEnabled(true);
        }
        activePreset = preset.get() + ":" + includeSky.get();
    }
}

package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.SkyGpuRenderer;
import com.lovenote.util.CelestialGpuRenderer;
import com.lovenote.util.NativeAuroraRenderer;
import com.lovenote.util.NativeNebulaRenderer;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.level.dimension.DimensionType;

/** V38: GPU-animated, procedural sky prototype. No per-frame cloud sprites. */
public final class SkyShader extends Module {
    private final ModeSetting preset = add(new ModeSetting("Preset", "Galactic Dream",
        "Off", "Galactic Dream", "Event Horizon", "Aurora Nights"));
    private final ModeSetting palette = add(new ModeSetting("Palette", "Violet & Cyan",
        "Violet & Cyan", "Pink & Blue", "Emerald", "Crimson", "Client Theme"));
    private final ModeSetting quality = add(new ModeSetting("Sky quality", "Balanced",
        "Performance", "Balanced", "Cinematic"));
    private final BoolSetting overworldOnly = add(new BoolSetting("Overworld only", true));
    private final BoolSetting hideInMenus = add(new BoolSetting("Hide in menus", false));
    private final BoolSetting vanillaClouds = add(new BoolSetting("Vanilla clouds", false));
    private final BoolSetting animated = add(new BoolSetting("Animate sky", true));
    // V42 is opt-in until the real-world movement and compatibility tests pass.
    private final BoolSetting worldAurora = add(new BoolSetting("World-space aurora V2", false));
    // Native, depth-tested WORLD geometry rather than the directional sky dome.
    // V45 defaults on for Aurora Nights; disable to compare against the legacy effect.
    private final BoolSetting nativeCurtains = add(new BoolSetting("Native 3D curtains", true));
    private final NumberSetting nativeAltitude = add(new NumberSetting("Native curtain altitude", 188, 100, 320, 4));
    private final NumberSetting nativeHeight = add(new NumberSetting("Native curtain height", 75, 25, 145, 5));
    private final NumberSetting nativeOpacity = add(new NumberSetting("Native curtain opacity", 0.46, 0.05, 0.85, 0.05));
    private final BoolSetting nativeNebula = add(new BoolSetting("Native world nebula", true));
    private final NumberSetting nebulaAltitude = add(new NumberSetting("Nebula altitude", 205, 120, 320, 5));
    private final NumberSetting nebulaSpacing = add(new NumberSetting("Nebula layer spacing", 28, 10, 90, 2));
    private final NumberSetting nebulaOpacity = add(new NumberSetting("Nebula opacity", 0.68, 0.05, 1.0, 0.05));
    private final NumberSetting auroraAltitude = add(new NumberSetting("Aurora altitude", 320, 180, 480, 20));
    private final NumberSetting animationSpeed = add(new NumberSetting("Animation speed", 1.0, 0, 3.0, 0.05));
    private final NumberSetting brightness = add(new NumberSetting("Sky brightness", 0.9, 0.15, 1.6, 0.05));
    private final NumberSetting density = add(new NumberSetting("Nebula density", 0.75, 0, 1, 0.05));
    private final NumberSetting flow = add(new NumberSetting("Flow strength", 1, 0, 3, 0.05));
    private final NumberSetting detail = add(new NumberSetting("Cloud scale", 1, 0.4, 2.5, 0.05));
    private final NumberSetting skyExpanse = add(new NumberSetting("Sky expanse", 1.65, 0.70, 3.00, 0.05));
    private final NumberSetting celestialSize = add(new NumberSetting("Black hole size", 1, 0.45, 1.8, 0.05));
    private final NumberSetting auroraIntensity = add(new NumberSetting("Aurora intensity", 0.9, 0, 2, 0.05));
    private long start = System.nanoTime();

    public SkyShader() {
        super("SkyShader", Category.RENDER);
        describe("GPU sky panorama: animated nebulae, plasma disc and auroras with adjustable visual scale.");
    }

    @Override public void onEnable() { start = System.nanoTime(); }
    public boolean active() { return enabled && !preset.is("Off"); }
    public boolean vanillaClouds() { return vanillaClouds.get(); }

    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if (mc.level == null || !active()) return;
        if (overworldOnly.get() && mc.level.dimensionType().skybox() != DimensionType.Skybox.OVERWORLD) return;
        if (hideInMenus.get() && mc.gui.screen() != null) return;

        int mode = switch (preset.get()) {
            case "Event Horizon" -> 1;
            case "Aurora Nights" -> worldAurora.get() ? 3 : 2;
            default -> 0;
        };
        int q = quality.is("Performance") ? 0 : quality.is("Cinematic") ? 2 : 1;
        float t = animated.get() ? (float) ((System.nanoTime() - start) * 1e-9) * animationSpeed.getF() : 0f;
        int first = switch (palette.get()) {
            case "Pink & Blue" -> 0xFFFF65C7;
            case "Emerald" -> 0xFF36DDAE;
            case "Crimson" -> 0xFFFF537B;
            case "Client Theme" -> Colors.C1;
            default -> 0xFF9A6DFF;
        };
        int second = switch (palette.get()) {
            case "Pink & Blue" -> 0xFF5CA4FF;
            case "Emerald" -> 0xFF56D9F5;
            case "Crimson" -> 0xFFFFA761;
            case "Client Theme" -> Colors.C3;
            default -> 0xFF5DE3ED;
        };
        if (preset.is("Aurora Nights") && nativeCurtains.get()) {
            NativeAuroraRenderer.draw(ctx.camera(), stack, t, q,
                nativeAltitude.getF(), nativeHeight.getF(), nativeOpacity.getF(),
                flow.getF(), brightness.getF(), first, second);
            return;
        }
        if (preset.is("Galactic Dream") && nativeNebula.get()) {
            NativeNebulaRenderer.draw(ctx.camera(), stack, t, q,
                nebulaAltitude.getF(), nebulaSpacing.getF(), nebulaOpacity.getF(),
                density.getF(), flow.getF(), brightness.getF(), first, second);
            return;
        }
        // Use the proven world geometry for Aurora Nights, but a dedicated
        // infinite-distance celestial pass for galaxies/black holes.  An astronomical
        // background must follow camera TRANSLATION; unlike curtains it must not
        // exhibit nearby parallax.  The old renderer remains as aurora fallback.
        if (preset.is("Aurora Nights")) {
            SkyGpuRenderer.draw(ctx.camera(), stack, t, mode, q,
                brightness.getF(), density.getF(), flow.getF(), detail.getF(),
                celestialSize.getF(), auroraIntensity.getF(), skyExpanse.getF(),
                auroraAltitude.getF(), first, second);
        } else {
            CelestialGpuRenderer.draw(ctx.camera(), stack, t, mode, q,
                brightness.getF(), density.getF(), flow.getF(), detail.getF(),
                celestialSize.getF(), auroraIntensity.getF(), skyExpanse.getF(),
                auroraAltitude.getF(), first, second);
        }
    }
}

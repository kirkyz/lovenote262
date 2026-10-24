package com.lovenote.module.impl;

import com.google.gson.JsonObject;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Method;

/**
 * Drainer V58: animated layered sky system, registered with Nuit at runtime.
 * No external resource packs. Compatible with the separately installed Nuit Fabric mod.
 * Does not touch the old GPU planet renderer.
 */
public final class SkyShader extends Module {
    private static final Identifier SKY_ID = Identifier.fromNamespaceAndPath("lovenote262", "drainer_selected_sky");
    private final ModeSetting sky = add(new ModeSetting("Sky", "Galaxy Sky",
        "Galaxy Sky", "Pink Nebula", "Galaxy Prime", "Galaxxy Sky", "Sky V1", "Better Sky", "GPU Plasma", "GPU Ocean", "GPU Water", "GPU Nebula", "GPU Aurora", "GPU Anchor Test"));
    private final NumberSetting gpuSpeed = add(new NumberSetting("GPU animation speed", 1.0, 0.0, 4.0, 0.1));
    private final NumberSetting gpuScale = add(new NumberSetting("GPU pattern scale", 2.4, 0.4, 8.0, 0.1));
    private final NumberSetting gpuBrightness = add(new NumberSetting("GPU brightness", 0.85, 0.05, 1.5, 0.05));
    private final NumberSetting gpuDetail = add(new NumberSetting("GPU detail", 2, 1, 3, 1));
    private final NumberSetting gpuDensity = add(new NumberSetting("GPU density", 0.75, 0.1, 1.5, 0.05));
    private final NumberSetting gpuHueA = add(new NumberSetting("GPU primary hue", 270, 0, 360, 1));
    private final NumberSetting gpuHueB = add(new NumberSetting("GPU secondary hue", 192, 0, 360, 1));
    private final NumberSetting gpuSaturation = add(new NumberSetting("GPU saturation", 85, 0, 100, 1));
    private final BoolSetting showClouds = add(new BoolSetting("Show vanilla clouds", false));
    private final BoolSetting animate = add(new BoolSetting("Rotate sky slowly", true));
    private final NumberSetting speed = add(new NumberSetting("Sky rotation", 0.12, 0, 2.0, 0.02));
    private static final Identifier STARS_ID = Identifier.fromNamespaceAndPath("lovenote262", "drainer_v58_stars");
    private static final Identifier GLOW_ID = Identifier.fromNamespaceAndPath("lovenote262", "drainer_v58_atmosphere");
    private final BoolSetting livingStars = add(new BoolSetting("Animated star layer", true));
    private final NumberSetting starsSpeed = add(new NumberSetting("Star movement", 0.65, 0.0, 5.0, 0.05));
    private final BoolSetting movingHaze = add(new BoolSetting("Moving atmosphere", true));
    private final NumberSetting hazeSpeed = add(new NumberSetting("Atmosphere movement", 0.3, 0.0, 3.0, 0.05));
    private final BoolSetting hideSun = add(new BoolSetting("Hide vanilla sun", true));
    private final BoolSetting hideMoon = add(new BoolSetting("Hide vanilla moon", true));
    private String applied = "";
    private boolean anchorReady;
    private Object anchorWorld;
    private double anchorX, anchorY, anchorZ;
    private boolean missingLogged;

    public SkyShader() {
        super("SkyShader", Category.RENDER);
        describe("Client-integrated GPU skies and optional Nuit skyboxes. Each sky can be selected without resource packs.");
    }

    @Override public void onEnable() { applied = ""; anchorReady = false; }
    @Override public void onDisable() { unregister(); applied = ""; anchorReady = false; anchorWorld = null; }
    @Override public void onTick() {
        if (!enabled || mc.level == null) return;
        // Entering/leaving the test resets the one-time landmark placement.
        if (!sky.get().equals("GPU Anchor Test") || anchorWorld != mc.level) anchorReady = false;
        if (sky.get().startsWith("GPU ")) {
            if (!"gpu".equals(applied)) unregister();
            applied = "gpu";
            return;
        }
        String key = sky.get() + ":" + animate.get() + ":" + speed.getF() + ":" + livingStars.get() + ":" + starsSpeed.getF() + ":" + movingHaze.get() + ":" + hazeSpeed.getF();
        if (key.equals(applied)) return;
        try {
            Object api = api();
            if (api == null) {
                if (!missingLogged) {
                    System.err.println("[Drainer] Built-in skies require Nuit in this game instance.");
                    missingLogged = true;
                }
                return;
            }
            unregister(api);
            JsonObject json = new JsonObject();
            json.addProperty("schemaVersion", 1);
            json.addProperty("type", "square-textured");
            json.addProperty("texture", "lovenote262:textures/sky/" + filename(sky.get()) + ".png");
            JsonObject properties = new JsonObject();
            properties.addProperty("layer", 90);
            properties.addProperty("transitionInDuration", 30);
            properties.addProperty("transitionOutDuration", 30);
            JsonObject rotation = new JsonObject();
            rotation.addProperty("skyboxRotation", true);
            rotation.addProperty("duration", 24000);
            rotation.addProperty("speed", animate.get() ? speed.getF() : 0.0f);
            JsonObject axis = new JsonObject();
            com.google.gson.JsonArray direction = new com.google.gson.JsonArray();
            direction.add(0); direction.add(1); direction.add(0);
            axis.add("0", direction);
            rotation.add("axis", axis);
            properties.add("rotation", rotation);
            json.add("properties", properties);
            Object skybox = parse(api, json);
            if (skybox instanceof java.util.Optional<?> optional) skybox = optional.orElse(null);
            if (skybox == null) throw new IllegalStateException("Nuit could not parse built-in sky: " + sky.get());
            for (Method method : api.getClass().getMethods()) {
                if (method.getName().equals("addPermanentSkybox") && method.getParameterCount() == 2) {
                    method.invoke(api, SKY_ID, skybox);
                    if (livingStars.get()) registerLayer(api, STARS_ID, "lovenote262:textures/sky/v58_stars.png", 110, starsSpeed.getF(), 0, 0);
                    if (movingHaze.get()) registerLayer(api, GLOW_ID, "lovenote262:textures/sky/v58_haze.png", 105, -hazeSpeed.getF(), 6000, 1);
                    applied = key;
                    System.out.println("[Drainer] Loaded animated sky layers for: " + sky.get());
                    return;
                }
            }
            throw new NoSuchMethodException("Nuit.addPermanentSkybox");
        } catch (Exception e) {
            System.err.println("[Drainer] Failed to load built-in sky: " + e);
            // Retry on next setting change or after restarting, not every tick.
            applied = key;
        }
    }

    /** Two independently animated, alpha-blended skybox atlases. No per-tick mesh rebuilding. */
    private void registerLayer(Object api, Identifier id, String texture, int layer, double velocity, int phase, int axisMode) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("schemaVersion", 1);
        json.addProperty("type", "square-textured");
        json.addProperty("texture", texture);
        JsonObject props = new JsonObject();
        props.addProperty("layer", layer);
        props.addProperty("blend", "normal");
        props.addProperty("transitionInDuration", 50);
        props.addProperty("transitionOutDuration", 50);
        JsonObject rotation = new JsonObject();
        rotation.addProperty("skyboxRotation", true);
        rotation.addProperty("duration", 24000);
        rotation.addProperty("speed", velocity);
        JsonObject axis = new JsonObject();
        com.google.gson.JsonArray direction = new com.google.gson.JsonArray();
        direction.add(axisMode == 0 ? 0 : 17); direction.add(1); direction.add(axisMode == 0 ? 0 : 8);
        axis.add("0", direction);
        rotation.add("axis", axis);
        props.add("rotation", rotation);
        JsonObject fade = new JsonObject();
        fade.addProperty("duration", 2400);
        JsonObject frames = new JsonObject();
        if (axisMode == 0) {
            frames.addProperty("0", 0.34); frames.addProperty("600", 0.9);
            frames.addProperty("1200", 0.45); frames.addProperty("1800", 0.95);
        } else {
            frames.addProperty("0", 0.14); frames.addProperty("600", 0.34);
            frames.addProperty("1200", 0.22); frames.addProperty("1800", 0.4);
        }
        fade.add("keyFrames", frames);
        props.add("fade", fade);
        json.add("properties", props);
        Object obj = parseWithId(api, id, json);
        if (obj instanceof java.util.Optional<?> opt) obj = opt.orElse(null);
        if (obj == null) throw new IllegalStateException("Nuit did not parse: " + texture);
        for (Method method : api.getClass().getMethods()) {
            if (method.getName().equals("addPermanentSkybox") && method.getParameterCount() == 2) {
                method.invoke(api, id, obj); return;
            }
        }
        throw new NoSuchMethodException("Nuit.addPermanentSkybox");
    }

    private Object parseWithId(Object api, Identifier id, JsonObject json) throws Exception {
        for (Method method : api.getClass().getMethods()) {
            if (method.getName().equals("parseSkybox") && method.getParameterCount() == 2) {
                return method.invoke(api, id, json);
            }
        }
        throw new NoSuchMethodException("Nuit.parseSkybox");
    }
    private Object parse(Object api, JsonObject json) throws Exception {
        for (Method method : api.getClass().getMethods()) {
            if (method.getName().equals("parseSkybox") && method.getParameterCount() == 2) {
                return method.invoke(api, SKY_ID, json);
            }
        }
        throw new NoSuchMethodException("Nuit.parseSkybox");
    }
    private static Object api() {
        try {
            Class<?> cls = Class.forName("me.flashyreese.mods.nuit.api.NuitApi");
            return cls.getMethod("getInstance").invoke(null);
        } catch (ReflectiveOperationException | LinkageError e) { return null; }
    }
    private static void unregister(Object api) {
        try {
            for (Method m : api.getClass().getMethods()) {
                if (m.getName().equals("removePermanentSkybox") && m.getParameterCount() == 1) {
                    m.invoke(api, SKY_ID);
                    m.invoke(api, STARS_ID);
                    m.invoke(api, GLOW_ID);
                    return;
                }
            }
        } catch (ReflectiveOperationException e) {
            System.err.println("[Drainer] Could not remove previous sky: " + e);
        }
    }
    private static void unregister() { Object a = api(); if (a != null) unregister(a); }
    private static String filename(String name) {
        return switch (name) {
            case "Pink Nebula" -> "pink_nebula";
            case "Galaxy Prime" -> "galaxy_prime";
            case "Galaxxy Sky" -> "galaxxy_sky";
            case "Sky V1" -> "sky_v1";
            case "Better Sky" -> "better_sky";
            default -> "galaxy_sky";
        };
    }
    public boolean active() { return enabled; } // Sky ownership is controlled by Nuit, not the old claimSky mixin.
    public boolean vanillaClouds() { return !enabled || showClouds.get(); }
    public boolean hideVanillaSun() { return enabled && hideSun.get(); }
    public boolean hideVanillaMoon() { return enabled && hideMoon.get(); }
    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if (!enabled || mc.level == null || !sky.get().startsWith("GPU ")) return;
        if (sky.get().equals("GPU Anchor Test")) {
            // Capture THREE FIXED WORLD landmarks at the moment the test starts.
            // They never receive the player's position again until you disable
            // and re-enable SkyShader, change worlds, or leave the test mode.
            if (!anchorReady || anchorWorld != mc.level) {
                var cameraPos = mc.gameRenderer.mainCamera().position();
                var look = mc.player != null ? mc.player.getLookAngle() : new net.minecraft.world.phys.Vec3(0, 0, 1);
                anchorX = cameraPos.x + look.x * 230.0;
                anchorY = Math.max(cameraPos.y + 85.0, cameraPos.y + look.y * 230.0 + 55.0);
                anchorZ = cameraPos.z + look.z * 230.0;
                anchorWorld = mc.level;
                anchorReady = true;
            }
            float seconds = (float) ((System.nanoTime() / 1_000_000_000.0) % 36000.0);
            com.lovenote.util.SkyAnchorProbeRenderer.draw(
                mc.gameRenderer.mainCamera(), stack, seconds * gpuSpeed.getF(),
                (float) anchorX, (float) anchorY, (float) anchorZ,
                gpuHueA.getF(), gpuHueB.getF(), gpuSaturation.getF(), gpuBrightness.getF());
            return;
        }
        int mode = switch (sky.get()) {
            case "GPU Ocean" -> 1;
            case "GPU Water" -> 2;
            case "GPU Nebula" -> 3;
            case "GPU Aurora" -> 4;
            default -> 0;
        };
        float seconds = (float) ((System.nanoTime() / 1_000_000_000.0) % 36000.0);
        com.lovenote.util.PolarGpuSkyRenderer.draw(
            mc.gameRenderer.mainCamera(), stack, seconds * gpuSpeed.getF(), mode,
            gpuScale.getF(), gpuBrightness.getF(), gpuDetail.getF(), gpuDensity.getF(),
            gpuHueA.getF(), gpuHueB.getF(), gpuSaturation.getF());
    }
}

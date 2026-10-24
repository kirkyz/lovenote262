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
 * Drainer V56: bundled skyboxes registered with Nuit at runtime.
 * No external resource packs. Compatible with the separately installed Nuit Fabric mod.
 * Does not touch the old GPU planet renderer.
 */
public final class SkyShader extends Module {
    private static final Identifier SKY_ID = Identifier.fromNamespaceAndPath("lovenote262", "drainer_selected_sky");
    private final ModeSetting sky = add(new ModeSetting("Sky", "Deep Space",
        "Deep Space", "Aurora Dream", "Crimson Eclipse", "Blue Hour", "Starfall", "Obsidian"));
    private final BoolSetting animate = add(new BoolSetting("Rotate sky slowly", true));
    private final NumberSetting speed = add(new NumberSetting("Sky rotation", 0.12, 0, 2.0, 0.02));
    private final BoolSetting hideSun = add(new BoolSetting("Hide vanilla sun", true));
    private final BoolSetting hideMoon = add(new BoolSetting("Hide vanilla moon", true));
    private String applied = "";
    private boolean missingLogged;

    public SkyShader() {
        super("SkyShader", Category.RENDER);
        describe("Built-in animated sky collection powered by Nuit. No resource packs required.");
    }

    @Override public void onEnable() { applied = ""; }
    @Override public void onDisable() { unregister(); applied = ""; }
    @Override public void onTick() {
        if (!enabled || mc.level == null) return;
        String key = sky.get() + ":" + animate.get() + ":" + speed.getF();
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
                    applied = key;
                    System.out.println("[Drainer] Loaded built-in sky: " + sky.get());
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
                    m.invoke(api, SKY_ID); return;
                }
            }
        } catch (ReflectiveOperationException e) {
            System.err.println("[Drainer] Could not remove previous sky: " + e);
        }
    }
    private static void unregister() { Object a = api(); if (a != null) unregister(a); }
    private static String filename(String name) {
        return switch (name) {
            case "Aurora Dream" -> "aurora_dream";
            case "Crimson Eclipse" -> "crimson_eclipse";
            case "Blue Hour" -> "blue_hour";
            case "Starfall" -> "starfall";
            case "Obsidian" -> "obsidian";
            default -> "deep_space";
        };
    }
    public boolean active() { return enabled; } // Sky ownership is controlled by Nuit, not the old claimSky mixin.
    public boolean vanillaClouds() { return true; }
    public boolean hideVanillaSun() { return enabled && hideSun.get(); }
    public boolean hideVanillaMoon() { return enabled && hideMoon.get(); }
    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) { /* Nuit draws sky layers. */ }
}

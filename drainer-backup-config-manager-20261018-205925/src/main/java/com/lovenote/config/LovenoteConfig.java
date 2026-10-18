package com.lovenote.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lovenote.Lovenote;
import com.lovenote.module.Module;
import com.lovenote.setting.Setting;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenotePalette;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class LovenoteConfig {
        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
        private static final File FILE = new File(MeteorClient.FOLDER, "lovenote.json");

        private static String lastWritten = "";
        private static long lastCheck;

        private LovenoteConfig() {}

        public static void load() {
                if (Lovenote.modules == null) return;
                if (!FILE.exists()) { save(true); return; }

                try {
                        String raw = Files.readString(FILE.toPath(), StandardCharsets.UTF_8);
                        JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
                        if (root.has("menuTheme") && root.get("menuTheme").isJsonObject()) {
                                JsonObject theme = root.getAsJsonObject("menuTheme");
                                if (theme.has("accentIndex")) {
                                        int idx = theme.get("accentIndex").getAsInt();
                                        if (idx >= -1 && idx < LovenotePalette.PRESETS.length)
                                                LovenoteMenuConfig.accentIndex = idx;
                                }
                                if (theme.has("customPrimary")) LovenoteMenuConfig.customPrimary = theme.get("customPrimary").getAsString();
                                if (theme.has("customSecondary")) LovenoteMenuConfig.customSecondary = theme.get("customSecondary").getAsString();
                        }
                        if (root.has("menuAppearance") && root.get("menuAppearance").isJsonObject()) {
                                JsonObject appearance = root.getAsJsonObject("menuAppearance");
                                if (appearance.has("menuScale")) LovenoteMenuConfig.menuScale = Math.max(20, Math.min(110, appearance.get("menuScale").getAsInt()));
                                if (appearance.has("compact")) LovenoteMenuConfig.compact = appearance.get("compact").getAsBoolean();
                                if (appearance.has("descriptions")) LovenoteMenuConfig.descriptions = appearance.get("descriptions").getAsBoolean();
                                if (appearance.has("tooltips")) LovenoteMenuConfig.tooltips = appearance.get("tooltips").getAsBoolean();
                                if (appearance.has("logo")) LovenoteMenuConfig.logo = appearance.get("logo").getAsBoolean();
                                if (appearance.has("shadow")) LovenoteMenuConfig.shadow = appearance.get("shadow").getAsBoolean();
                                if (appearance.has("sheen")) LovenoteMenuConfig.sheen = appearance.get("sheen").getAsBoolean();
                                if (appearance.has("ripples")) LovenoteMenuConfig.ripples = appearance.get("ripples").getAsBoolean();
                                if (appearance.has("footer")) LovenoteMenuConfig.footer = appearance.get("footer").getAsBoolean();
                                if (appearance.has("headerLine")) LovenoteMenuConfig.headerLine = appearance.get("headerLine").getAsBoolean();
                        }
                        LovenoteMenuConfig.apply();
                        JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();

                        for (Module m : Lovenote.modules.all()) {
                                if (!mods.has(m.name)) continue;
                                JsonObject o = mods.getAsJsonObject(m.name);

                                if (o.has("key")) m.key = o.get("key").getAsInt();
                                if (o.has("favorite")) m.favorite = o.get("favorite").getAsBoolean();
                                if (o.has("hidden")) m.hidden = o.get("hidden").getAsBoolean();

                                if (o.has("settings")) {
                                        JsonObject s = o.getAsJsonObject("settings");
                                        for (Setting st : m.settings) st.load(s);
                                }

                                m.loadExtra(o);

                                if (o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean());
                        }
                        lastWritten = raw;
                } catch (Exception ex) {
                        System.out.println("[Lovenote] config load failed: " + ex);
                }
        }

        public static void tick() {
                long now = System.currentTimeMillis();
                if (now - lastCheck < 5000) return;
                lastCheck = now;
                save(false);
        }

        public static void save(boolean force) {
                if (Lovenote.modules == null) return;

                JsonObject root = new JsonObject();
                root.addProperty("version", 1);

                JsonObject mods = new JsonObject();
                for (Module m : Lovenote.modules.all()) {
                        JsonObject o = new JsonObject();
                        o.addProperty("enabled", m.enabled);
                        o.addProperty("key", m.key);
                        o.addProperty("favorite", m.favorite);
                        o.addProperty("hidden", m.hidden);

                        JsonObject s = new JsonObject();
                        for (Setting st : m.settings) st.save(s);
                        o.add("settings", s);

                        m.saveExtra(o);
                        mods.add(m.name, o);
                }
                root.add("modules", mods);
                JsonObject theme = new JsonObject();
                theme.addProperty("accentIndex", LovenoteMenuConfig.accentIndex);
                theme.addProperty("customPrimary", LovenoteMenuConfig.customPrimary);
                theme.addProperty("customSecondary", LovenoteMenuConfig.customSecondary);
                root.add("menuTheme", theme);
                JsonObject appearance = new JsonObject();
                appearance.addProperty("menuScale", LovenoteMenuConfig.menuScale);
                appearance.addProperty("compact", LovenoteMenuConfig.compact);
                appearance.addProperty("descriptions", LovenoteMenuConfig.descriptions);
                appearance.addProperty("tooltips", LovenoteMenuConfig.tooltips);
                appearance.addProperty("logo", LovenoteMenuConfig.logo);
                appearance.addProperty("shadow", LovenoteMenuConfig.shadow);
                appearance.addProperty("sheen", LovenoteMenuConfig.sheen);
                appearance.addProperty("ripples", LovenoteMenuConfig.ripples);
                appearance.addProperty("footer", LovenoteMenuConfig.footer);
                appearance.addProperty("headerLine", LovenoteMenuConfig.headerLine);
                root.add("menuAppearance", appearance);

                String out = GSON.toJson(root);
                if (!force && out.equals(lastWritten)) return;

                try {
                        if (!MeteorClient.FOLDER.exists()) MeteorClient.FOLDER.mkdirs();
                        Files.writeString(FILE.toPath(), out, StandardCharsets.UTF_8);
                        lastWritten = out;
                } catch (IOException ex) {
                        System.out.println("[Lovenote] config save failed: " + ex);
                }
        }
}

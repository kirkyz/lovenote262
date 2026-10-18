package com.lovenote.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import meteordevelopment.meteorclient.MeteorClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Named snapshots of Lovenote modules, separate from global UI/theme settings. */
public final class DrainerProfiles {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR = new java.io.File(MeteorClient.FOLDER, "drainer-profiles").toPath();
    private static final Path META = DIR.resolve("_profiles.json");
    private static String active = "";
    private static boolean autoSave = false, autoLoad = false;
    private static String lastSnapshot = "";
    private static boolean started;

    private DrainerProfiles() {}

    private static String safe(String name) {
        if (name == null) return "";
        String s = name.trim().replaceAll("[^a-zA-Z0-9 _-]", "");
        return s.length() > 32 ? s.substring(0, 32).trim() : s;
    }
    private static Path file(String name) { return DIR.resolve(safe(name) + ".json"); }
    public static String active() { return active; }
    public static boolean autoSave() { return autoSave; }
    public static boolean autoLoad() { return autoLoad; }
    public static void setAutoSave(boolean value) { autoSave = value; persistMeta(); }
    public static void setAutoLoad(boolean value) { autoLoad = value; persistMeta(); }
    public static String status = "";

    public static void startup() {
        if (started) return;
        started = true;
        try {
            if (Files.exists(META)) {
                JsonObject o = JsonParser.parseString(Files.readString(META)).getAsJsonObject();
                active = o.has("active") ? safe(o.get("active").getAsString()) : "";
                autoSave = o.has("autoSave") && o.get("autoSave").getAsBoolean();
                autoLoad = o.has("autoLoad") && o.get("autoLoad").getAsBoolean();
            }
            if (autoLoad && !active.isEmpty()) loadInternal(active, false);
            else lastSnapshot = GSON.toJson(LovenoteConfig.snapshotModules());
        } catch (Exception e) { status = "Profile startup failed: " + e.getMessage(); }
    }
    public static List<String> names() {
        try {
            Files.createDirectories(DIR);
            List<String> names = new ArrayList<>();
            try (var stream = Files.list(DIR)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .filter(p -> !p.getFileName().toString().equals("_profiles.json"))
                    .forEach(p -> names.add(p.getFileName().toString().replaceFirst("\\.json$", "")));
            }
            Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
            return names;
        } catch (Exception e) { status = "Cannot list profiles"; return List.of(); }
    }
    public static void create(String name) {
        String n = safe(name);
        if (n.isEmpty()) { status = "Enter a profile name"; return; }
        if (Files.exists(file(n))) { status = "That name already exists"; return; }
        saveAs(n);
    }
    private static void saveAs(String n) {
        try {
            Files.createDirectories(DIR);
            JsonObject root = new JsonObject();
            root.addProperty("version", 1);
            root.add("modules", LovenoteConfig.snapshotModules());
            Files.writeString(file(n), GSON.toJson(root), StandardCharsets.UTF_8);
            active = n;
            lastSnapshot = GSON.toJson(root.getAsJsonObject("modules"));
            persistMeta();
            status = "Saved " + n;
        } catch (Exception e) { status = "Save failed: " + e.getMessage(); }
    }
    public static void saveActive() {
        if (active.isEmpty()) { status = "Select a profile first"; return; }
        saveAs(active);
    }
    public static void load(String name) {
        String n = safe(name);
        if (autoSave && !active.isEmpty() && !active.equals(n)) saveActive();
        loadInternal(n, true);
    }
    private static void loadInternal(String n, boolean updateMeta) {
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file(n))).getAsJsonObject();
            JsonObject modules = root.getAsJsonObject("modules");
            if (modules == null) throw new IllegalArgumentException("Missing modules");
            LovenoteConfig.applyModules(modules);
            active = n;
            lastSnapshot = GSON.toJson(LovenoteConfig.snapshotModules());
            if (updateMeta) persistMeta();
            LovenoteConfig.save(true);
            status = "Loaded " + n;
        } catch (Exception e) { status = "Load failed: " + e.getMessage(); }
    }
    public static void delete(String name) {
        try {
            String n = safe(name);
            Files.deleteIfExists(file(n));
            if (active.equals(n)) { active = ""; autoSave = false; persistMeta(); }
            status = "Deleted " + n;
        } catch (Exception e) { status = "Delete failed"; }
    }
    public static void tick() {
        if (!started || !autoSave || active.isEmpty()) return;
        String current = GSON.toJson(LovenoteConfig.snapshotModules());
        if (!current.equals(lastSnapshot)) saveActive();
    }
    private static void persistMeta() {
        try {
            Files.createDirectories(DIR);
            JsonObject o = new JsonObject();
            o.addProperty("active", active);
            o.addProperty("autoSave", autoSave);
            o.addProperty("autoLoad", autoLoad);
            Files.writeString(META, GSON.toJson(o), StandardCharsets.UTF_8);
        } catch (Exception e) { status = "Cannot save profile preferences"; }
    }
}

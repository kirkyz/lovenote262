package com.lovenote.module.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lovenote.Lovenote;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.module.impl.ported.DrainerServerMacros.MacroRecorder;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Named, durable movement presets for the existing native MacroRecorder. */
public final class MacroLibrary extends Module {
    public record Preset(String name, String group, String detail, boolean saved) { }
    private record Saved(int[] frames, String group) { }
    private static final int LIMIT = 48;
    private final Map<String, Saved> custom = new LinkedHashMap<>();
    private final ModeSetting template = add(new ModeSetting("Template", "Walk 2s",
            "Walk 2s", "Left and right", "Jump once", "Stand still",
            "Garden row practice", "Mining strafe practice"));
    private final BoolSetting load = add(new BoolSetting("Load into recorder slot", false));

    public MacroLibrary() {
        super("MacroLibrary", Category.MISC);
        alsoIn.add(Category.HYPIXEL); alsoIn.add(Category.DONUT);
        describe("Load movement practice templates or save named recordings in the library. Routes must be recorded for your own world.");
    }
    @Override public void onTick() {
        if (!load.get()) return;
        load.set(false);
        loadPreset(template.get());
    }
    public synchronized List<Preset> presets() {
        List<Preset> result = new ArrayList<>();
        result.add(new Preset("Walk 2s", "Movement", "Moves forward for 40 ticks.", false));
        result.add(new Preset("Left and right", "Movement", "Strafes left for 40 ticks, then right for 40 ticks.", false));
        result.add(new Preset("Jump once", "Movement", "Waits 10 ticks, jumps briefly, then stops.", false));
        result.add(new Preset("Stand still", "Movement", "40 ticks of stationary movement input.", false));
        result.add(new Preset("Garden row practice", "SkyBlock • practice", "Sample 5-second forward input. NOT a complete farm route; record your own path.", false));
        result.add(new Preset("Mining strafe practice", "SkyBlock • practice", "Alternating horizontal movement for testing. Does not mine or turn.", false));
        custom.forEach((name, saved) -> result.add(new Preset(name, saved.group(),
            "Your saved recording (" + saved.frames().length + " ticks). Load to the selected recorder slot.", true)));
        return List.copyOf(result);
    }
    public synchronized boolean saveSelected(String name) {
        if (name == null) return false;
        String cleaned = name.trim().replaceAll("[\\p{Cntrl}]", "");
        if (cleaned.isBlank() || cleaned.length() > 36 || isBuiltin(cleaned)) return false;
        if (custom.size() >= LIMIT && !custom.containsKey(cleaned)) return false;
        MacroRecorder recorder = recorder();
        if (recorder == null || recorder.isBusy()) return false;
        int[] frames = recorder.selectedFrames();
        if (frames.length == 0) return false;
        custom.put(cleaned, new Saved(frames.clone(), "My recordings"));
        return true;
    }
    public synchronized boolean deleteSaved(String name) { return custom.remove(name) != null; }
    public synchronized boolean loadPreset(String name) {
        MacroRecorder recorder = recorder();
        if (recorder == null || recorder.isBusy()) return false;
        Saved stored = custom.get(name);
        int[] frames = stored != null ? stored.frames().clone() : switch (name) {
            case "Walk 2s" -> fill(40, 1);
            case "Left and right" -> alternating();
            case "Jump once" -> jumping();
            case "Stand still" -> fill(40, 0);
            case "Garden row practice" -> fill(100, 1);
            case "Mining strafe practice" -> mining();
            default -> null;
        };
        if (frames == null) return false;
        recorder.loadTemplate(frames);
        return true;
    }
    @Override public synchronized void saveExtra(JsonObject o) {
        JsonArray presets = new JsonArray();
        custom.forEach((name, saved) -> {
            JsonObject item = new JsonObject();
            item.addProperty("name", name);
            item.addProperty("group", saved.group());
            JsonArray frames = new JsonArray();
            for (int value : saved.frames()) frames.add(value);
            item.add("frames", frames);
            presets.add(item);
        });
        o.add("namedMacrosV13", presets);
    }
    @Override public synchronized void loadExtra(JsonObject o) {
        custom.clear();
        if (!o.has("namedMacrosV13") || !o.get("namedMacrosV13").isJsonArray()) return;
        for (var item : o.getAsJsonArray("namedMacrosV13")) {
            try {
                if (custom.size() >= LIMIT || !item.isJsonObject()) break;
                JsonObject entry = item.getAsJsonObject();
                String name = entry.get("name").getAsString().trim();
                if (name.isBlank() || name.length() > 36 || isBuiltin(name)) continue;
                JsonArray samples = entry.getAsJsonArray("frames");
                int[] frames = new int[Math.min(com.lovenote.macro.MacroTape.MAX_TICKS, samples.size())];
                for (int i = 0; i < frames.length; i++) frames[i] = samples.get(i).getAsInt() & 63;
                String group = entry.has("group") ? entry.get("group").getAsString() : "My recordings";
                if (frames.length > 0) custom.put(name, new Saved(frames, group));
            } catch (RuntimeException ignored) { }
        }
    }
    private static boolean isBuiltin(String s) {
        return switch(s) {
            case "Walk 2s", "Left and right", "Jump once", "Stand still", "Garden row practice", "Mining strafe practice" -> true;
            default -> false;
        };
    }
    private static MacroRecorder recorder() {
        return Lovenote.modules == null ? null : Lovenote.modules.get(MacroRecorder.class);
    }
    private static int[] fill(int ticks, int mask) { int[] a = new int[ticks]; Arrays.fill(a, mask); return a; }
    private static int[] alternating() { int[] a = new int[80]; for(int i=0;i<a.length;i++)a[i]=i<40?4:8; return a; }
    private static int[] jumping() { int[] a = new int[45]; for(int i=10;i<16;i++)a[i]=16; return a; }
    private static int[] mining() { int[] a = new int[120]; for(int i=0;i<a.length;i++)a[i]=(i/30)%2==0?4:8;return a; }
}

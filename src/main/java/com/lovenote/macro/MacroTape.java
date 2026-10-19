package com.lovenote.macro;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;

/** Bounded, version-independent recorded movement tape. One sample per Minecraft client tick. */
public final class MacroTape {
    public static final int MAX_TICKS = 2400;
    private final List<Integer> frames = new ArrayList<>();

    public int size() { return frames.size(); }
    public boolean isEmpty() { return frames.isEmpty(); }
    public int get(int i) { return frames.get(i); }
    public void clear() { frames.clear(); }
    public boolean append(int mask) {
        if (frames.size() >= MAX_TICKS) return false;
        frames.add(mask & 63);
        return true;
    }
    public void setFrom(int[] values) {
        clear();
        for (int value : values) if (!append(value)) break;
    }
    public JsonArray toJson() {
        JsonArray array = new JsonArray();
        for (int value : frames) array.add(value);
        return array;
    }
    public void fromJson(JsonElement element) {
        clear();
        if (!element.isJsonArray()) return;
        for (JsonElement e : element.getAsJsonArray()) {
            if (frames.size() >= MAX_TICKS) break;
            try { append(e.getAsInt()); } catch (RuntimeException ignored) { }
        }
    }
}

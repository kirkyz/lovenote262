package com.lovenote.macro;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import java.util.ArrayList;
import java.util.List;

/** Bounded movement + actions + optional look tape. Old integer-only saves still load. */
public final class MacroTape {
    public static final int MAX_TICKS = 2400;
    private final List<Integer> frames = new ArrayList<>();
    private final List<Float> yaw = new ArrayList<>();
    private final List<Float> pitch = new ArrayList<>();

    public int size() { return frames.size(); }
    public boolean isEmpty() { return frames.isEmpty(); }
    public int get(int i) { return frames.get(i); }
    public float getYaw(int i) { return yaw.get(i); }
    public float getPitch(int i) { return pitch.get(i); }
    public void clear() { frames.clear(); yaw.clear(); pitch.clear(); }
    public boolean append(int mask) { return append(mask, Float.NaN, Float.NaN); }
    public boolean append(int mask, float y, float p) {
        if (frames.size() >= MAX_TICKS) return false;
        frames.add(mask & 8191); // bits 0-7 inputs, bit 12 slot present, bits 8-11 selected slot
        yaw.add(Float.isFinite(y) ? y : Float.NaN);
        pitch.add(Float.isFinite(p) ? Math.max(-90f, Math.min(90f, p)) : Float.NaN);
        return true;
    }
    public void setFrom(int[] values) { setFrom(values, null, null); }
    public void setFrom(int[] values, float[] ys, float[] ps) {
        clear();
        for (int i = 0; i < values.length && i < MAX_TICKS; i++)
            append(values[i], ys == null || i >= ys.length ? Float.NaN : ys[i],
                ps == null || i >= ps.length ? Float.NaN : ps[i]);
    }
    public JsonArray toJson() {
        JsonArray out = new JsonArray();
        for (int mask : frames) out.add(mask);
        return out;
    }
    /** Optional metadata is separate to allow V14 rollback to read the V11 integer tape. */
    public JsonArray rotationsToJson() {
        JsonArray out = new JsonArray();
        for (int i = 0; i < frames.size(); i++) {
            if (Float.isFinite(yaw.get(i)) && Float.isFinite(pitch.get(i))) {
                JsonArray angles = new JsonArray();
                angles.add(yaw.get(i)); angles.add(pitch.get(i)); out.add(angles);
            } else out.add(JsonNull.INSTANCE);
        }
        return out;
    }
    public void fromJson(JsonElement element) {
        clear();
        if (element == null || !element.isJsonArray()) return;
        for (JsonElement e : element.getAsJsonArray()) {
            if (frames.size() >= MAX_TICKS) break;
            try { append(e.getAsInt()); } catch (RuntimeException ignored) { }
        }
    }
    public void loadRotations(JsonElement element) {
        if (element == null || !element.isJsonArray()) return;
        JsonArray values = element.getAsJsonArray();
        for (int i = 0; i < Math.min(size(), values.size()); i++) {
            try {
                if (!values.get(i).isJsonArray()) continue;
                JsonArray pair = values.get(i).getAsJsonArray();
                if (pair.size() < 2) continue;
                float y = pair.get(0).getAsFloat(), p = pair.get(1).getAsFloat();
                if (Float.isFinite(y) && Float.isFinite(p)) {
                    yaw.set(i, y); pitch.set(i, Math.max(-90f, Math.min(90f, p)));
                }
            } catch (RuntimeException ignored) { }
        }
    }
}

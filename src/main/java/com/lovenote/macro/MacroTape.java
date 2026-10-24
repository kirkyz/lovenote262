package com.lovenote.macro;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/** Bounded movement + actions + optional look tape. Old integer-only saves still load. */
public final class MacroTape {
    public static final int MAX_TICKS = 2400;
    private final List<Integer> frames = new ArrayList<>();
    private final List<Float> yaw = new ArrayList<>();
    private final List<Float> pitch = new ArrayList<>();
    /** Input press counts survive presses shorter than 50ms (one game tick). */
    private final List<Integer> taps = new ArrayList<>();
    /** 0 world, 1 player's own inventory, 2 another container. */
    private final List<Integer> screens = new ArrayList<>();
    public record ContainerClick(int slot, int button, String type, int slotCount) { }
    private final List<List<ContainerClick>> containerClicks = new ArrayList<>();
    public int getTaps(int i) { return taps.get(i); }
    public int getScreen(int i) { return screens.get(i); }
    public List<ContainerClick> getContainerClicks(int i) { return List.copyOf(containerClicks.get(i)); }
    public void markLastFrame(int screenMode, int attackPresses, int usePresses) {
        if (isEmpty()) return;
        int at = size() - 1;
        screens.set(at, Math.max(0, Math.min(2, screenMode)));
        taps.set(at, Math.min(8, Math.max(0, attackPresses)) | (Math.min(8, Math.max(0, usePresses)) << 4));
    }
    public void addClickToLastFrame(int slot, int button, String type, int slots) {
        if (isEmpty() || type == null || type.length() > 32) return;
        List<ContainerClick> actions = containerClicks.get(size() - 1);
        if (actions.size() >= 24) return;
        actions.add(new ContainerClick(slot, button, type, slots));
    }

    public int size() { return frames.size(); }
    public boolean isEmpty() { return frames.isEmpty(); }
    public int get(int i) { return frames.get(i); }
    public float getYaw(int i) { return yaw.get(i); }
    public float getPitch(int i) { return pitch.get(i); }
    public void clear() { frames.clear(); yaw.clear(); pitch.clear(); taps.clear(); screens.clear(); containerClicks.clear(); }
    public boolean append(int mask) { return append(mask, Float.NaN, Float.NaN); }
    public boolean append(int mask, float y, float p) {
        if (frames.size() >= MAX_TICKS) return false;
        frames.add(mask & 8191); // bits 0-7 inputs, bit 12 slot present, bits 8-11 selected slot
        yaw.add(Float.isFinite(y) ? y : Float.NaN);
        pitch.add(Float.isFinite(p) ? Math.max(-90f, Math.min(90f, p)) : Float.NaN);
        taps.add(0);
        screens.add(0);
        containerClicks.add(new ArrayList<>());
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
    /** Metadata is separate: V11 integer tapes can still be read by older builds. */
    public JsonArray eventsToJson() {
        JsonArray result = new JsonArray();
        for (int i = 0; i < size(); i++) {
            JsonObject frame = new JsonObject();
            if (taps.get(i) != 0) frame.addProperty("tap", taps.get(i));
            if (screens.get(i) != 0) frame.addProperty("screen", screens.get(i));
            if (!containerClicks.get(i).isEmpty()) {
                JsonArray clicks = new JsonArray();
                for (ContainerClick c : containerClicks.get(i)) {
                    JsonObject action = new JsonObject();
                    action.addProperty("slot", c.slot());
                    action.addProperty("button", c.button());
                    action.addProperty("type", c.type());
                    action.addProperty("slots", c.slotCount());
                    clicks.add(action);
                }
                frame.add("clicks", clicks);
            }
            result.add(frame);
        }
        return result;
    }
    public void loadEvents(JsonElement element) {
        if (element == null || !element.isJsonArray()) return;
        JsonArray arr = element.getAsJsonArray();
        for (int i = 0; i < Math.min(size(), arr.size()); i++) {
            try {
                if (!arr.get(i).isJsonObject()) continue;
                JsonObject frame = arr.get(i).getAsJsonObject();
                if (frame.has("tap")) taps.set(i, frame.get("tap").getAsInt() & 0xff);
                if (frame.has("screen")) screens.set(i, Math.max(0, Math.min(2, frame.get("screen").getAsInt())));
                if (!frame.has("clicks") || !frame.get("clicks").isJsonArray()) continue;
                for (JsonElement item : frame.getAsJsonArray("clicks")) {
                    if (!item.isJsonObject()) continue;
                    JsonObject c = item.getAsJsonObject();
                    if (!c.has("slot") || !c.has("button") || !c.has("type") || !c.has("slots")) continue;
                    addClickAt(i, c.get("slot").getAsInt(), c.get("button").getAsInt(),
                        c.get("type").getAsString(), c.get("slots").getAsInt());
                }
            } catch (RuntimeException ignored) { }
        }
    }
    private void addClickAt(int at, int slot, int button, String type, int slots) {
        if (at < 0 || at >= size() || type.length() > 32 || containerClicks.get(at).size() >= 24) return;
        containerClicks.get(at).add(new ContainerClick(slot, button, type, slots));
    }
    public void loadFrom(MacroTape other) {
        clear();
        for (int i = 0; i < other.size(); i++) {
            append(other.get(i), other.getYaw(i), other.getPitch(i));
            taps.set(i, other.getTaps(i));
            screens.set(i, other.getScreen(i));
            containerClicks.get(i).addAll(other.getContainerClicks(i));
        }
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

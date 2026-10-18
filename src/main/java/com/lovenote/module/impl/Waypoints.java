package com.lovenote.module.impl;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
/** Persistent waypoint coordinates and distance labels; no 3D beams yet. */
public final class Waypoints extends Module {
    private final BoolSetting addHere = add(new BoolSetting("Add here", false));
    private final BoolSetting clearAll = add(new BoolSetting("Clear all", false));
    private final NumberSetting maxVisible = add(new NumberSetting("Visible points", 4, 1, 12, 1));
    private final NumberSetting x = add(new NumberSetting("X", .02, 0, .98, .005));
    private final NumberSetting y = add(new NumberSetting("Y", .23, 0, .98, .005));
    private final List<double[]> points = new ArrayList<>();
    private String worldKey = "";
    public Waypoints() { super("Waypoints", Category.VISUALS); describe("Save locations using Add here; display coordinates and distances on the HUD."); }
    @Override public void onTick() {
        if (mc.player == null || mc.level == null) return;
        String current = scope();
        if (worldKey.isEmpty()) worldKey = current;
        if (clearAll.get()) { points.clear(); worldKey = current; clearAll.set(false); }
        if (!current.equals(worldKey)) return;
        if (addHere.get()) {
            if (points.size() >= 64) points.remove(0);
            points.add(new double[]{mc.player.getX(), mc.player.getY(), mc.player.getZ()});
            addHere.set(false);
        }
    }
    private String scope() {
        String server = mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "singleplayer";
        return server + "/" + mc.level.dimension().identifier();
    }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null || mc.level == null || points.isEmpty() || !scope().equals(worldKey)) return;
        int px = (int)(x.get() * Gfx.guiW()), py = (int)(y.get() * Gfx.guiH());
        int n = Math.min(points.size(), maxVisible.getI());
        for (int k = 0; k < n; k++) {
            int i = points.size() - n + k; double[] p = points.get(i);
            double dx = p[0] - mc.player.getX(), dy = p[1] - mc.player.getY(), dz = p[2] - mc.player.getZ();
            String s = String.format(Locale.ROOT, "WP%d (%.0f %.0f %.0f)  %.0fm", i + 1,p[0],p[1],p[2],Math.sqrt(dx*dx+dy*dy+dz*dz));
            int tx = Math.min(px, Math.max(0, Gfx.guiW() - Ui.textW(s) - 8));
            Ui.pill(g,tx,py+k*20,Ui.textW(s)+12,19);
            Ui.text(g,s,tx+6,py+k*20+5,Ui.textCol);
        }
    }
    @Override public void saveExtra(JsonObject obj) {
        JsonArray all = new JsonArray();
        for (double[] p : points) { JsonArray entry=new JsonArray(); for(double v:p) entry.add(v); all.add(entry); }
        obj.add("points",all);
        obj.addProperty("worldKey",worldKey);
    }
    @Override public void loadExtra(JsonObject obj) {
        points.clear(); worldKey = obj.has("worldKey") ? obj.get("worldKey").getAsString() : "";
        if (!obj.has("points") || !obj.get("points").isJsonArray()) return;
        for(JsonElement element:obj.getAsJsonArray("points")) {
            if(!element.isJsonArray()) continue;
            JsonArray a=element.getAsJsonArray();
            if(a.size()<3 || points.size()>=64) break;
            try { points.add(new double[]{a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble()}); }
            catch(RuntimeException ignored) {}
        }
    }
}

package meteordevelopment.meteorclient.gui.screens;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.InputConstants;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteDraw;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenotePalette;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Faithful port of the old client's LovenoteScreen, on vanilla GuiGraphicsExtractor. */
public class LovenoteModulesScreen extends TabScreen {
    private static final int PW = 208, PH = 320, ACC_COLS = 11, RAIL_MIN = 46;
    private static int mainX = Integer.MIN_VALUE, mainY, panX = Integer.MIN_VALUE, panY, page;
    private static long sessionStart;

    private static final int K_NONE = -1, K_CARD = 0, K_SWITCH = 1, K_BOOL = 2, K_SLIDER = 3, K_MODE = 4,
            K_PTOGGLE = 6, K_PCLOSE = 7, K_BIND = 8, K_ACCENT = 9, K_EDITOR = 10, K_CLOSE = 12, K_SEARCH = 13, K_MULTI = 5;

    private static final class Hit {
        int x, y, w, h, kind, idx;
        Row module;
        Object setting;
    }

    private static final class Ripple { int x, y; long t; }

    private final List<com.lovenote.module.Category> cats = new ArrayList<>();
    private final List<Row> allModules = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private final List<int[]> pageRects = new ArrayList<>();
    private final List<Ripple> ripples = new ArrayList<>();
    private final Map<String, Float> anim = new HashMap<>();

    private int wx, wy, winW = 600, winH = 372, railW = 118;
    private float gs = 1f;
    private int vw, vh;
    private float dt, ga = 1f, openMs, closeMs, tabMs, scrollTarget, scrollShown, panelScrollTarget, panelScrollShown;
    private float pillY = -1f;
    private int maxScroll, panelMax;
    private boolean closing, finishClose, animating;
    private long lastNano;
    private Row selected, panelModule, listening;
    private Hit dragging;
    private int dragWin, grabX, grabY;
    private boolean settingFields;
    private String tooltip;
    private float sbAlpha = 0.45f;

    private float[] px, py, pvx, pvy;
    private int particleCount = 45;
    private String searchText = "";
    private boolean searchFocused;

    public LovenoteModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
    }

    private static final ItemStack[] RAIL_ICONS = {
            new ItemStack(Items.NETHERITE_SWORD), new ItemStack(Items.FEATHER), new ItemStack(Items.LEATHER_BOOTS),
            new ItemStack(Items.ENDER_EYE), new ItemStack(Items.SPYGLASS), new ItemStack(Items.PAPER),
            new ItemStack(Items.REDSTONE), new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLDEN_APPLE),
            new ItemStack(Items.JUKEBOX), new ItemStack(Items.PAINTING)
    };

    private int pages() { return cats.size() + 2; }
    private boolean isSpecial(int p) { return p >= cats.size(); }
    private String pageLabel(int p) {
        if (p < cats.size()) return cats.get(p).label;
        return p == cats.size() ? "Spotify" : "Themes";
    }

    private interface Row {
        String title();
        String description();
        int catIndex();
        boolean active();
        void toggle();
        int key();
        void setKey(int k);
        List<Group> groups();
    }

    private record Group(String name, List<Object> settings) {}

    private static final class LRow implements Row {
        final com.lovenote.module.Module m;
        final int cat;
        LRow(com.lovenote.module.Module m, int cat) { this.m = m; this.cat = cat; }
        public String title() { return m.name; }
        public String description() { return m.description; }
        public int catIndex() { return cat; }
        public boolean active() { return m.enabled; }
        public void toggle() { m.toggle(); }
        public int key() { return m.key; }
        public void setKey(int k) { m.key = k; }
        public List<Group> groups() {
            Map<String, List<Object>> by = new LinkedHashMap<>();
            for (com.lovenote.setting.Setting st : m.settings) {
                String g = st instanceof com.lovenote.setting.BoolSetting ? "Toggles"
                        : st instanceof com.lovenote.setting.NumberSetting ? "Values"
                        : st instanceof com.lovenote.setting.ModeSetting ? "Modes" : "Lists";
                by.computeIfAbsent(g, k -> new ArrayList<>()).add(st);
            }
            List<Group> out = new ArrayList<>();
            for (Map.Entry<String, List<Object>> e : by.entrySet()) out.add(new Group(e.getKey(), e.getValue()));
            return out;
        }
    }

    private static final class MRow implements Row {
        final Module m;
        final int cat;
        MRow(Module m, int cat) { this.m = m; this.cat = cat; }
        public String title() { return m.title; }
        public String description() { return m.description; }
        public int catIndex() { return cat; }
        public boolean active() { return m.isActive(); }
        public void toggle() { m.toggle(); }
        public int key() { return m.keybind.getValue(); }
        public void setKey(int k) { m.keybind.set(true, k, 0); }
        public List<Group> groups() {
            List<Group> out = new ArrayList<>();
            for (SettingGroup g : m.settings) {
                List<Object> list = new ArrayList<>();
                for (Setting<?> st : g) list.add(st);
                out.add(new Group(g.name, list));
            }
            return out;
        }
    }

    private int catIndex(com.lovenote.module.Category c) {
        for (int i = 0; i < cats.size(); i++) if (cats.get(i) == c) return i;
        return -1;
    }

    private static com.lovenote.module.Category mapCat(String name) {
        for (com.lovenote.module.Category c : com.lovenote.module.Category.values()) {
            if (c.label.equalsIgnoreCase(name) || c.name().equalsIgnoreCase(name)) return c;
        }
        return name.equalsIgnoreCase("world")
                ? com.lovenote.module.Category.RENDER
                : com.lovenote.module.Category.MISC;
    }

    private void buildRows() {
        allModules.clear();
        for (com.lovenote.module.Module m : com.lovenote.Lovenote.modules.all()) {
            int i = catIndex(m.category);
            if (i >= 0) allModules.add(new LRow(m, i));
        }
        for (Module m : Modules.get().getAll()) {
            int i = catIndex(mapCat(m.category.name));
            if (i >= 0) allModules.add(new MRow(m, i));
        }
    }

    @Override
    public void initWidgets() {
        if (sessionStart == 0L) sessionStart = System.currentTimeMillis();

        cats.clear();
        for (com.lovenote.module.Category c : com.lovenote.module.Category.values()) {
            if (c == com.lovenote.module.Category.SPOTIFY || c == com.lovenote.module.Category.THEMES) continue;
            cats.add(c);
        }
        buildRows();

        readLayout();
        vw = Math.max(160, Math.round(width / gs));
        vh = Math.max(120, Math.round(height / gs));

        if (mainX == Integer.MIN_VALUE) { mainX = (vw - winW) / 2; mainY = (vh - winH) / 2; }
        if (panX == Integer.MIN_VALUE) {
            panX = mainX + winW + 8 + PW <= vw - 4 ? mainX + winW + 8 : mainX + winW - PW - 10;
            panY = mainY + 10;
        }
        clampWindows();
        lastNano = System.nanoTime();

        Random r = new Random();
        px = new float[140]; py = new float[140]; pvx = new float[140]; pvy = new float[140];
        for (int i = 0; i < px.length; i++) {
            px[i] = r.nextFloat() * width; py[i] = r.nextFloat() * height;
            pvx[i] = (r.nextFloat() - 0.5f) * 0.25f; pvy[i] = -0.08f - r.nextFloat() * 0.35f;
        }

    }

    private void onSearch() { scrollTarget = 0; scrollShown = 0; tabMs = 0; }

    private static Window windowHandle() { return mc.getWindow(); }

    private EditBox mkField(String hint, int max, String initial) {
        EditBox f = new EditBox(font, 0, 0, 100, 12, Component.literal(hint));
        f.setBordered(false);
        f.setMaxLength(max);
        f.setValue(initial);
        f.setFocused(false);
        return f;
    }

    private void readLayout() {
        gs = Mth.clamp(LovenoteMenuConfig.menuScale / 100f, 0.4f, 1.1f);
        winW = Mth.clamp(LovenoteMenuConfig.windowWidth, 460, 900);
        winH = Mth.clamp(LovenoteMenuConfig.windowHeight, 280, 560);
        railW = Mth.clamp(LovenoteMenuConfig.railWidth, 92, 168);
        particleCount = Mth.clamp(LovenoteMenuConfig.particles, 0, 140);
        LovenoteMenuConfig.apply();
    }

    private int contentX() { return LovenoteMenuConfig.railRight ? wx + 14 : wx + railW + 14; }
    private int contentW() { return winW - railW - 28; }
    private int railX() { return LovenoteMenuConfig.railRight ? wx + winW - railW : wx; }

    private void clampWindows() {
        mainX = Mth.clamp(mainX, Math.min(0, vw - winW), Math.max(0, vw - winW));
        mainY = Mth.clamp(mainY, Math.min(0, vh - winH), Math.max(0, vh - winH));
        panX = Mth.clamp(panX, 0, Math.max(0, vw - PW));
        panY = Mth.clamp(panY, 0, Math.max(0, vh - PH));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() { close(); }

    private void close() {
        if (finishClose) { mc.gui.setScreen(null); return; }
        if (!closing) { closing = true; closeMs = 0; }
    }

    // ---------------------------------------------------------------- helpers

    private int a(int c) { return ((int) (((c >>> 24) * ga)) << 24) | (c & 0xFFFFFF); }
    private void fill(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int c) { ctx.fill(x1, y1, x2, y2, a(c)); }
    private void rr(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int c) { LovenoteDraw.rounded(ctx, x, y, w, h, 0, a(c)); }
    private void rr(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int r, int c) { LovenoteDraw.rounded(ctx, x, y, w, h, r, a(c)); }
    private void grad(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int c1, int c2) { LovenoteDraw.gradientH(ctx, x1, y1, x2, y2, a(c1), a(c2)); }
    private void txt(GuiGraphicsExtractor ctx, String s, int x, int y, int c) {
        int cc = a(c); if ((cc >>> 24) < 12) return;
        ctx.text(font, s, x, y, cc, false);
    }
    private void txtS(GuiGraphicsExtractor ctx, String s, int x, int y, int c) {
        int cc = a(c); if ((cc >>> 24) < 12) return;
        ctx.text(font, s, x, y, cc, true);
    }
    private void circle(GuiGraphicsExtractor ctx, int cx, int cy, int r, int c) { LovenoteDraw.circle(ctx, cx, cy, r, a(c)); }

    private float ease(String key, float target, float durMs) {
        float v = anim.getOrDefault(key, target);
        v += (target - v) * (1f - (float) Math.exp(-dt / Math.max(8f, durMs * 0.25f)));
        if (Math.abs(target - v) < 0.004f) v = target;
        anim.put(key, v);
        return v;
    }

    private void addHit(int x, int y, int w, int h, int kind, Row m, Object s, int idx) {
        Hit hit = new Hit();
        hit.x = x; hit.y = y; hit.w = w; hit.h = h; hit.kind = kind; hit.module = m; hit.setting = s; hit.idx = idx;
        hits.add(hit);
    }

    private String keyName(int key) {
        if (key == -1) return "none";
        String n = InputConstants.getKey(new KeyEvent(key, 0, 0)).getDisplayName().getString().toUpperCase();
        return n.length() > 7 ? n.substring(0, 7) : n;
    }

    private void scrollbar(GuiGraphicsExtractor ctx, int x, int top, int bottom, float shown, int max) {
        if (max <= 0) return;
        int trackH = bottom - top;
        int barH = Math.max(18, (int) (trackH * (trackH / (float) (trackH + max))));
        int barY = top + (int) ((trackH - barH) * (shown / max));
        rr(ctx, x, top, 3, trackH, 2, LovenotePalette.alpha(LovenotePalette.track(), (int) (120 * sbAlpha)));
        rr(ctx, x, barY, 3, barH, 2, LovenotePalette.alpha(LovenotePalette.wave(0.1f), (int) (255 * sbAlpha)));
    }

    private float stagger(int index) {
        if (!LovenoteMenuConfig.stagger) return 1f;
        return Mth.clamp((tabMs - index * LovenoteMenuConfig.staggerSpeed) / 170f, 0f, 1f);
    }

    // ---------------------------------------------------------------- settings rows

    private void drawSwitch(GuiGraphicsExtractor ctx, int x, int y, float t) {
        rr(ctx, x, y, 22, 10, 5, LovenotePalette.lerp(LovenotePalette.track(), LovenotePalette.C2, t));
        if (t > 0.02f) rr(ctx, x + 2, y + 1, 18, 8, 4, LovenotePalette.alpha(LovenotePalette.C2, (int) (255 * t)));
        rr(ctx, x + 1 + (int) (t * 12), y + 1, 8, 8, 4, 0xFFFFFFFF);
    }

    private int chipsHeight(String[] names, int w) {
        int cx = 0, rows = 1;
        for (String n : names) {
            int cw = font.width(n) + 12;
            if (cx + cw > w && cx > 0) { rows++; cx = 0; }
            cx += cw + 4;
        }
        return rows * 15 + (rows - 1) * 3;
    }

    private int rowH(Object s, int w) {
        if (s instanceof com.lovenote.setting.BoolSetting) return 20;
        if (s instanceof com.lovenote.setting.NumberSetting) return 29;
        if (s instanceof com.lovenote.setting.ModeSetting e) return 14 + chipsHeight(e.modes, w) + 7;
        if (s instanceof com.lovenote.setting.MultiSetting m) return 14 + chipsHeight(m.names, w) + 7;
        if (s instanceof Setting<?> ss) {
            if (ss instanceof BoolSetting || ss instanceof KeybindSetting) return 20;
            if (ss instanceof IntSetting || ss instanceof DoubleSetting) return 29;
            if (ss instanceof EnumSetting<?> e) return 14 + chipsHeight(enumNames(e), w) + 7;
        }
        return 0;
    }

    private static Object[] enumValues(EnumSetting<?> e) {
        Object cur = e.get();
        return cur == null ? new Object[0] : cur.getClass().getEnumConstants();
    }

    private String[] enumNames(EnumSetting<?> e) {
        Object[] v = enumValues(e);
        String[] out = new String[v.length];
        for (int i = 0; i < v.length; i++) out[i] = String.valueOf(v[i]);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static void setEnum(EnumSetting<?> e, Object v) { ((Setting<Object>) e).set(v); }

    private int drawSetting(GuiGraphicsExtractor ctx, Row owner, Object s, int x, int y, int w) {
        if (s instanceof com.lovenote.setting.BoolSetting b) {
            float t = ease(b.hashCode() + "b", b.get() ? 1f : 0f, 150);
            txt(ctx, b.name, x, y + 4, LovenotePalette.lerp(LovenotePalette.dim(), LovenotePalette.text(), t));
            drawSwitch(ctx, x + w - 24, y + 4, t);
            addHit(x - 2, y, w + 4, 18, K_BOOL, owner, b, 0);
            return 20;
        }
        if (s instanceof com.lovenote.setting.NumberSetting n) {
            txt(ctx, n.name, x, y + 2, LovenotePalette.dim());
            String v = n.step >= 1 ? String.valueOf(n.getI()) : String.format("%.2f", n.get());
            txt(ctx, v, x + w - font.width(v), y + 2, LovenotePalette.faint());
            drawSlider(ctx, x, y, w, (float) ((n.get() - n.min) / (n.max - n.min)));
            addHit(x, y + 11, w, 13, K_SLIDER, owner, n, 0);
            return 29;
        }
        if (s instanceof com.lovenote.setting.ModeSetting e) {
            txt(ctx, e.name, x, y + 2, LovenotePalette.dim());
            chips(ctx, e.modes, e.index(), x, y + 14, w, K_MODE, owner, e);
            return 14 + chipsHeight(e.modes, w) + 7;
        }
        if (s instanceof com.lovenote.setting.MultiSetting ms) {
            txt(ctx, ms.name, x, y + 2, LovenotePalette.dim());
            chipsMulti(ctx, ms, x, y + 14, w, owner);
            return 14 + chipsHeight(ms.names, w) + 7;
        }
        if (!(s instanceof Setting<?> st)) return 0;
        Setting<?> s2 = st;
        if (!s2.isVisible()) return 0;
        if (s instanceof BoolSetting b) {
            float t = ease(b.hashCode() + "b", b.get() ? 1f : 0f, 150);
            txt(ctx, b.name, x, y + 4, LovenotePalette.lerp(LovenotePalette.dim(), LovenotePalette.text(), t));
            drawSwitch(ctx, x + w - 24, y + 4, t);
            addHit(x - 2, y, w + 4, 18, K_BOOL, owner, b, 0);
        } else if (s instanceof IntSetting n) {
            txt(ctx, n.name, x, y + 2, LovenotePalette.dim());
            txt(ctx, String.valueOf(n.get()), x + w - font.width(String.valueOf(n.get())), y + 2, LovenotePalette.faint());
            float t = (float) ((n.get() - n.sliderMin) / (double) (n.sliderMax - n.sliderMin));
            drawSlider(ctx, x, y, w, t);
            addHit(x, y + 11, w, 13, K_SLIDER, owner, n, 0);
        } else if (s instanceof DoubleSetting n) {
            txt(ctx, n.name, x, y + 2, LovenotePalette.dim());
            String val = String.format("%.2f", n.get());
            txt(ctx, val, x + w - font.width(val), y + 2, LovenotePalette.faint());
            float t = (float) ((n.get() - n.sliderMin) / (n.sliderMax - n.sliderMin));
            drawSlider(ctx, x, y, w, t);
            addHit(x, y + 11, w, 13, K_SLIDER, owner, n, 0);
        } else if (s instanceof EnumSetting<?> e) {
            txt(ctx, e.name, x, y + 2, LovenotePalette.dim());
            String[] names = enumNames(e);
            int active = 0;
            Object[] vals = enumValues(e);
            for (int i = 0; i < vals.length; i++) if (vals[i] == e.get()) active = i;
            chips(ctx, names, active, x, y + 14, w, K_MODE, owner, e);
        } else if (s instanceof KeybindSetting k) {
            txt(ctx, k.name, x, y + 4, LovenotePalette.dim());
            String kn = listening == owner ? "..." : keyName(k.get() == null ? -1 : k.get().getValue());
            int kw = font.width(kn) + 16;
            rr(ctx, x + w - kw, y + 2, kw, 16, 8, LovenotePalette.chip());
            txt(ctx, kn, x + w - kw + 8, y + 6, LovenotePalette.text());
            addHit(x + w - kw, y + 2, kw, 16, K_BIND, owner, k, 0);
        }
        return rowH(s, w);
    }

    private void drawSlider(GuiGraphicsExtractor ctx, int x, int y, int w, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int sy = y + 16;
        fill(ctx, x, sy, x + w, sy + 3, LovenotePalette.track());
        int fx = x + (int) (w * t);
        grad(ctx, x, sy, Math.max(x + 1, fx), sy + 3, LovenotePalette.C1, LovenotePalette.skeet(t));
        rr(ctx, Mth.clamp(fx - 3, x, x + w - 6), sy - 3, 6, 9, 3, 0xFFFFFFFF);
    }

    private void chipsMulti(GuiGraphicsExtractor ctx, com.lovenote.setting.MultiSetting ms, int x, int y, int w, Row owner) {
        int cx = 0, cy = 0;
        for (int i = 0; i < ms.names.length; i++) {
            int cw = font.width(ms.names[i]) + 12;
            if (cx + cw > w && cx > 0) { cx = 0; cy += 18; }
            int rx = x + cx, ry = y + cy;
            float t = ease(ms.hashCode() + ":" + i, ms.get(i) ? 1f : 0f, 150);
            rr(ctx, rx, ry, cw, 15, 3, LovenotePalette.lerp(LovenotePalette.chip(), LovenotePalette.C2, t));
            txt(ctx, ms.names[i], rx + 6, ry + 4, LovenotePalette.lerp(LovenotePalette.dim(), LovenotePalette.text(), t));
            addHit(rx, ry, cw, 15, K_MULTI, owner, ms, i);
            cx += cw + 4;
        }
    }

    private void chips(GuiGraphicsExtractor ctx, String[] names, int active, int x, int y, int w, int kind, Row owner, Object set) {
        int cx = 0, cy = 0;
        for (int i = 0; i < names.length; i++) {
            int cw = font.width(names[i]) + 12;
            if (cx + cw > w && cx > 0) { cx = 0; cy += 18; }
            int rx = x + cx, ry = y + cy;
            float t = ease(set.hashCode() + ":" + i, i == active ? 1f : 0f, 150);
            rr(ctx, rx, ry, cw, 15, 3, LovenotePalette.lerp(LovenotePalette.chip(), LovenotePalette.C2, t));
            if (t > 0.02f) rr(ctx, rx + 2, ry + 1, cw - 4, 13, 3, LovenotePalette.alpha(LovenotePalette.C2, (int) (255 * t)));
            txt(ctx, names[i], rx + 6, ry + 4, LovenotePalette.lerp(LovenotePalette.dim(), LovenotePalette.text(), t));
            addHit(rx, ry, cw, 15, kind, owner, set, i);
            cx += cw + 4;
        }
    }

    private int panelTotal(Row m, int w) {
        int total = 0;
        for (Group g : m.groups()) {
            boolean any = false;
            int sum = 0;
            for (Object s : g.settings()) { int h = rowH(s, w); if (h > 0) { any = true; sum += h; } }
            if (any) total += 18 + sum;
        }
        return total;
    }

    // ---------------------------------------------------------------- render

    @Override
    public void renderCustom(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        dt = Math.min(100f, (now - lastNano) / 1_000_000f);
        lastNano = now;

        readLayout();
        vw = Math.max(160, Math.round(width / gs));
        vh = Math.max(120, Math.round(height / gs));
        clampWindows();

        double s = mc.getWindow().getGuiScale();
        int mx = (int) (mouseX / s), my = (int) (mouseY / s);

        hits.clear();
        pageRects.clear();
        tooltip = null;
        wx = mainX; wy = mainY;
        tabMs += dt;
        sbAlpha += (0.45f - sbAlpha) * 0.12f;

        float animMs = LovenoteMenuConfig.animSpeed;
        int corner = LovenoteMenuConfig.corner;

        float open;
        if (closing) {
            closeMs += dt;
            open = 1f - Math.min(1f, closeMs / Math.max(60f, animMs * 0.7f));
            if (closeMs >= animMs * 0.7f) { finishClose = true; close(); return; }
        } else {
            openMs += dt;
            open = Math.min(1f, openMs / Math.max(60f, animMs));
        }
        float eo = 1f - (1f - open) * (1f - open) * (1f - open);
        animating = open < 0.999f;
        ga = eo;

        ctx.fill(0, 0, width, height, LovenotePalette.alpha(0x000000, (int) (130 * eo)));
        drawDrainerAtmosphere(ctx, eo);
        for (int i = 0; i < particleCount && i < px.length; i++) {
            float sp = LovenoteMenuConfig.particleSpeed / 100f;
            px[i] += pvx[i] * sp; py[i] += pvy[i] * sp;
            if (py[i] < 0) { py[i] = height; px[i] = (float) (Math.random() * width); }
            if (px[i] < 0) px[i] = width;
            if (px[i] > width) px[i] = 0;
            int sz = 1 + (i % 2);
            ctx.fill((int) px[i], (int) py[i], (int) px[i] + sz, (int) py[i] + sz,
                    LovenotePalette.alpha(LovenotePalette.skeet(i / (float) Math.max(1, particleCount)), (int) (130 * eo)));
        }

        Matrix3x2fStack ms = ctx.pose();
        ms.pushMatrix();
        ms.scale(gs, gs);
        if (LovenoteMenuConfig.animation.equals("Tilt")) {
            float sc = 0.9f + 0.1f * eo;
            float cx = wx + winW / 2f, cy = wy + winH / 2f;
            ms.translate(cx, cy + (1f - eo) * 14f); ms.scale(sc, sc); ms.translate(-cx, -cy);
        } else if (LovenoteMenuConfig.animation.equals("Scale")) {
            float sc = 0.92f + 0.08f * eo;
            float cx = wx + winW / 2f, cy = wy + winH / 2f;
            ms.translate(cx, cy); ms.scale(sc, sc); ms.translate(-cx, -cy);
        }

        boolean interactive = !animating;
        if (selected != null) panelModule = selected;
        float panelA = ease("panel", selected != null ? 1f : 0f, 220);

        drawWindow(ctx, mx, my, interactive, corner);
        if (panelModule != null && panelA > 0.01f) drawPanel(ctx, mx, my, panelA, corner);
        drawRail(ctx, mx, my, corner);
        drawRipples(ctx);

        if (listening != null) txtS(ctx, "press a key to bind " + listening.title() + "   (esc = clear)", wx + winW / 2 - 100, wy + winH + 8, 0xFFFFD27A);
        if (tooltip != null) {
            int tw = font.width(tooltip) + 10;
            rr(ctx, mx + 10, my + 12, tw, 16, 5, LovenotePalette.alpha(0x0A0A10, 240));
            txt(ctx, tooltip, mx + 15, my + 16, LovenotePalette.text());
        }
        ms.popMatrix();
    }

    // Drainer ClickGUI-only ambience. Drawn behind particles and window controls.
    private void drawDrainerAtmosphere(GuiGraphicsExtractor ctx, float openAlpha) {
        float time = (System.nanoTime() % 20000000000L) / 1_000_000_000f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 0.8f);
        int w = width;
        int h = height;

        // Restrained violet glow along the outer screen edges.
        for (int i = 0; i < 9; i++) {
            float fade = (1f - i / 9f) * (0.35f + pulse * 0.25f) * openAlpha;
            int alpha = Math.min(24, Math.max(0, (int) (fade * 30)));
            if (alpha == 0) continue;
            int col = LovenotePalette.alpha(LovenotePalette.C2, alpha);
            ctx.fill(i, 0, i + 1, h, col);
            ctx.fill(w - i - 1, 0, w - i, h, col);
            if (i < 5) {
                ctx.fill(0, i, w, i + 1, LovenotePalette.alpha(LovenotePalette.C1, alpha));
            }
        }

        // Tiny moving glints on the top perimeter, away from the logo/search area.
        for (int i = 0; i < 7; i++) {
            float phase = (time * 0.055f + i * 0.173f) % 1f;
            int x = (int) (phase * Math.max(1, w));
            int y = 5 + (i % 3) * 3;
            int alpha = (int) ((35 + 45 * pulse) * openAlpha);
            int c = LovenotePalette.alpha(LovenotePalette.C3, alpha);
            ctx.fill(x, y, x + 2, y + 3, c);
            ctx.fill(Math.max(0, x - 2), y + 1, x + 4, y + 2,
                    LovenotePalette.alpha(LovenotePalette.C2, alpha / 3));
        }
    }
    private void addRipple(int x, int y) {
        if (ripples.size() > 24) ripples.remove(0);
        Ripple r = new Ripple();
        r.x = x; r.y = y; r.t = System.currentTimeMillis();
        ripples.add(r);
    }

    private void drawRipples(GuiGraphicsExtractor ctx) {
        long now = System.currentTimeMillis();
        for (int i = ripples.size() - 1; i >= 0; i--) {
            Ripple r = ripples.get(i);
            float age = (now - r.t) / 420f;
            if (age >= 1f) { ripples.remove(i); continue; }
            int rad = 6 + (int) (age * 34);
            int alpha = (int) ((1f - age) * 110);
            for (int ang = 0; ang < 40; ang++) {
                double an = ang / 40.0 * Math.PI * 2.0;
                int x = r.x + (int) Math.round(Math.cos(an) * rad);
                int y = r.y + (int) Math.round(Math.sin(an) * rad);
                ctx.fill(x, y, x + 2, y + 2, LovenotePalette.alpha(LovenotePalette.wave(0f), alpha));
            }
        }
    }

    private void drawWindow(GuiGraphicsExtractor ctx, int mx, int my, boolean interactive, int corner) {
        int styleA = LovenoteMenuConfig.style.equals("Premium") ? 0xE8 : LovenoteMenuConfig.style.equals("Classic") ? 0xF8 : LovenoteMenuConfig.style.equals("Liquid") ? 0xCC : 0x8C;
        int alpha = (int) (styleA * LovenoteMenuConfig.opacity / 100.0);

        for (int i = LovenoteMenuConfig.glow; i >= 1; i--) rr(ctx, wx - i, wy - i, winW + i * 2, winH + i * 2, corner + 2, LovenotePalette.alpha(LovenotePalette.C2, 5));
        if (LovenoteMenuConfig.shadow) for (int i = 4; i >= 1; i--) rr(ctx, wx - i, wy + 2, winW + i * 2, winH + i * 2, corner + 2, LovenotePalette.alpha(0x000000, 8));
        rr(ctx, wx - 1, wy - 1, winW + 2, winH + 2, corner + 1, LovenotePalette.alpha(LovenotePalette.C2, 64));
        rr(ctx, wx, wy, winW, winH, corner, LovenotePalette.alpha(LovenotePalette.bg(), alpha));

        // Premium glass: restrained inner chrome rim and violet highlights.
        if (LovenoteMenuConfig.style.equals("Premium")) {
            rr(ctx, wx + 2, wy + 2, winW - 4, winH - 4,
                Math.max(4, corner - 2),
                LovenotePalette.alpha(LovenotePalette.C2, 18));

            rr(ctx, wx + 4, wy + 4, winW - 8, winH - 8,
                Math.max(4, corner - 4),
                LovenotePalette.alpha(LovenotePalette.bg(), 220));

            fill(ctx, wx + corner + 8, wy + 4,
                wx + winW - corner - 8, wy + 5,
                LovenotePalette.alpha(LovenotePalette.C3, 75));
        }
        if (LovenoteMenuConfig.headerLine) grad(ctx, wx + corner, wy, wx + winW - corner, wy + 1, LovenotePalette.alpha(LovenotePalette.C1, 150), LovenotePalette.alpha(LovenotePalette.C3, 150));
        if (LovenoteMenuConfig.sheen) for (int i = 0; i < 26; i++) {
            int yy = wy + 2 + i * 2;
            if (yy >= wy + winH - 2) break;
            fill(ctx, wx + 2, yy, wx + winW - 2, yy + 2, LovenotePalette.alpha(0xFFFFFF, (int) (10 * (1f - i / 26f))));
        }

        int cx = contentX(), cw = contentW();
        int hdr = wy + 107;
        int ftr = wy + winH - (LovenoteMenuConfig.footer ? 22 : 8);
        fill(ctx, cx - 2, hdr, cx + cw + 2, hdr + 1, LovenotePalette.alpha(LovenotePalette.track(), 150));
        if (LovenoteMenuConfig.footer) fill(ctx, cx - 2, ftr, cx + cw + 2, ftr + 1, LovenotePalette.alpha(LovenotePalette.track(), 150));

        String q = searchText.trim().toLowerCase();
        boolean moduleGrid = !q.isEmpty() || !isSpecial(page);

        // DRAINER: centred animated logo, above the search bar.
        if (LovenoteMenuConfig.logo) {
            double headerLogoAngle = System.nanoTime() / 1000000000.0
                * Math.PI * 2.0 * 0.18;

            com.lovenote.util.Logos.drawSmoothWide(ctx, cx + (cw - 230) / 2, wy, 230, 76, 0.0);
        }

        int sw = Math.min(200, cw / 2);
        rr(ctx, cx, wy + 78, sw, 18, 9, LovenotePalette.alpha(LovenotePalette.chip(), 210));
        circle(ctx, cx + 11, wy + 87, 4, LovenotePalette.faint());
        circle(ctx, cx + 11, wy + 87, 2, LovenotePalette.alpha(LovenotePalette.bg(), 255));
        fill(ctx, cx + 14, wy + 90, cx + 17, wy + 91, LovenotePalette.faint());
        addHit(cx, wy + 78, sw, 18, K_SEARCH, null, null, 0);
        if (searchText.isEmpty() && !searchFocused) {
            txt(ctx, "Search...", cx + 22, wy + 83, LovenotePalette.faint());
        } else {
            txt(ctx, searchText, cx + 22, wy + 83, LovenotePalette.text());
            if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
                int caretX = cx + 22 + font.width(searchText);
                fill(ctx, caretX, wy + 82, caretX + 1, wy + 92, LovenotePalette.text());
            }
        }

        String crumb = "drainer";
        int crumbX = cx + sw + 10;
        txt(ctx, crumb, crumbX, wy + 83, LovenotePalette.alpha(LovenotePalette.faint(), 170));
        crumbX += font.width(crumb) + 4;
        txt(ctx, "\u203A", crumbX, wy + 83, LovenotePalette.alpha(LovenotePalette.faint(), 120));
        crumbX += 8;
        txt(ctx, !q.isEmpty() ? "search" : pageLabel(page).toLowerCase(), crumbX, wy + 83, LovenotePalette.wave(0f));

        int bx = wx + winW - 16;
        boolean cHov = interactive && mx >= bx - 4 && mx <= bx + 10 && my >= wy + 10 && my <= wy + 26;
        circle(ctx, bx + 3, wy + 18, 5, LovenotePalette.alpha(cHov ? 0xFFEF4444 : LovenotePalette.chip(), 255));
        addHit(bx - 3, wy + 10, 14, 14, K_CLOSE, null, null, 0);

        float tSlide = Mth.clamp(tabMs / 220f, 0f, 1f);
        float tEase = 1f - (1f - tSlide) * (1f - tSlide);
        int titleY = wy + 116 + (int) ((1f - tEase) * 8f);
        String title = !q.isEmpty() ? "All" : pageLabel(page);
        txtS(ctx, title, cx, titleY, LovenotePalette.text());
        int count = visibleModules(q).size();
        String sub = count + (count == 1 ? " module" : " modules");
        int subX = cx + font.width(title) + 8;
        if (subX + font.width(sub) < cx + cw) txt(ctx, sub, subX, titleY + 2, LovenotePalette.faint());

        int top = wy + 136, bottom = ftr - 6;
        if (!q.isEmpty() || !isSpecial(page)) renderCards(ctx, mx, my, cx, top, cw, bottom, q, interactive, corner);
        else if (page == cats.size() + 1) {
            drawThemePresets(ctx, cx, top, cw, bottom);
        } else {
            txt(ctx, pageLabel(page) + " page",
                cx + 4, top + 8, LovenotePalette.faint());
        }

        if (LovenoteMenuConfig.footer) drawFooter(ctx, cx, cw, ftr);
    }

    private void drawFooter(GuiGraphicsExtractor ctx, int cx, int cw, int ftr) {
        int y = ftr + 7;
        txt(ctx, "drainer", cx, y, LovenotePalette.faint());
        long secs = (System.currentTimeMillis() - sessionStart) / 1000L;
        txt(ctx, String.format("%02d:%02d", secs / 60, secs % 60), cx + font.width("drainer") + 6, y, LovenotePalette.alpha(LovenotePalette.faint(), 140));
        int enabled = 0;
        for (Row m : allModules) if (m.active()) enabled++;
        String[] seg = { enabled + " on", mc.getFps() + " fps", "ctrl+scroll" };
        int total = 0;
        for (String t : seg) total += font.width(t) + 10;
        total -= 10;
        int x = cx + cw - total;
        for (String t : seg) { if (x > cx + 80) txt(ctx, t, x, y, LovenotePalette.alpha(LovenotePalette.faint(), 160)); x += font.width(t) + 10; }
    }

    private List<Row> visibleModules(String q) {
        List<Row> out = new ArrayList<>();
        for (Row m : allModules) {
            if (!q.isEmpty()) { if (m.title().toLowerCase().contains(q) || m.description().toLowerCase().contains(q)) out.add(m); }
            else if (page < cats.size() && m.catIndex() == page) out.add(m);
        }
        out.sort((x, y) -> x.title().compareToIgnoreCase(y.title()));
        return out;
    }

    private void renderCards(GuiGraphicsExtractor ctx, int mx, int my,
                             int cx, int top, int cw, int bottom,
                             String q, boolean interactive, int corner) {
        List<Row> list = visibleModules(q);

        int ncols = Mth.clamp(LovenoteMenuConfig.columns, 1, 3);
        boolean compact = LovenoteMenuConfig.compact;
        int gap = 8;
        int cardW = (cw - (ncols - 1) * gap) / ncols;
        int cardH = compact ? 48 : 68;

        int rows = (list.size() + ncols - 1) / ncols;
        maxScroll = Math.max(0, rows * (cardH + gap) - (bottom - top));

        scrollTarget = Mth.clamp(scrollTarget, 0, maxScroll);
        scrollShown += (scrollTarget - scrollShown)
            * (1f - (float) Math.exp(-dt / 40f));

        if (Math.abs(scrollTarget - scrollShown) < 0.2f)
            scrollShown = scrollTarget;

        if (mx > cx - 6 && mx < cx + cw + 12
            && my > top && my < bottom)
            sbAlpha = 1f;

        int cr = Math.max(7, corner);

        ctx.enableScissor(cx - 2, top - 2, cx + cw + 4, bottom);

        for (int i = 0; i < list.size(); i++) {
            Row m = list.get(i);

            int col = i % ncols;
            int x = cx + col * (cardW + gap);
            int y = top + (i / ncols) * (cardH + gap)
                - Math.round(scrollShown);

            if (y + cardH < top - 8 || y > bottom + 8)
                continue;

            float st = stagger(i);
            int sy = y + (int) ((1f - st) * 10f);

            boolean hov = interactive
                && mx >= x && mx < x + cardW
                && my >= sy && my < sy + cardH
                && my >= top && my <= bottom;

            float hv = ease(m.title() + ":h", hov ? 1f : 0f, 150);
            float on = ease(m.title() + ":s", m.active() ? 1f : 0f, 180);
            float sel = ease(m.title() + ":sel",
                selected == m ? 1f : 0f, 180);

            int ly = sy - (int) (hv * LovenoteMenuConfig.hoverLift);
            int accent = LovenotePalette.wave(i * 0.07f);

            // Animated breathing glow
            float pulse = (float) (
                0.5 + 0.5 * Math.sin(
                    System.currentTimeMillis() / 650.0 + i * 0.65
                )
            );

            if (on > 0.01f || hv > 0.01f || sel > 0.01f) {
                int glowAlpha = (int) (
                    22 + 40 * pulse * on + 65 * hv + 75 * sel
                );

                rr(ctx, x - 2, ly - 2,
                    cardW + 4, cardH + 4, cr + 2,
                    LovenotePalette.alpha(accent, Math.min(110, glowAlpha / 2)));
            }

            // Glass card surface
            rr(ctx, x, ly, cardW, cardH, cr,
                LovenotePalette.lerp(
                    LovenotePalette.card(),
                    LovenotePalette.cardHover(), hv));

            // Accent line
            if (on > 0.01f) {
                rr(ctx, x + 12, ly + 1, cardW - 24, 2, 1,
                    LovenotePalette.alpha(accent,
                        (int) (110 * on)));
            }

            // Rounded illuminated icon tile
            int tile = compact ? 23 : 28;
            int ix = x + 12;
            int iy = ly + 9;

            rr(ctx, ix, iy, tile, tile, 8,
                LovenotePalette.alpha(accent,
                    (int) (65 + 70 * on + 40 * hv)));

            rr(ctx, ix + 2, iy + 2,
                tile - 4, tile - 4, 6,
                LovenotePalette.alpha(
                    LovenotePalette.card(), 160));

            // Minimal geometric module emblem
            circle(ctx, ix + tile / 2, iy + tile / 2,
                compact ? 5 : 6,
                LovenotePalette.alpha(accent, 235));

            circle(ctx, ix + tile / 2, iy + tile / 2,
                2, LovenotePalette.card());

            // Module title
            int textX = ix + tile + 9;
            int textW = Math.max(10, cardW - (textX - x) - 12);

            String name = trim(m.title(), textW);

            txt(ctx, name, textX, ly + 11,
                LovenotePalette.text());

            // Description
            if (!compact && LovenoteMenuConfig.descriptions) {
                txt(ctx,
                    trim(m.description(), Math.max(10, cardW - 26)),
                    x + 13, ly + 36,
                    LovenotePalette.dim());
            }

            // Status and animated switch
            String status = m.active() ? "ENABLED" : "DISABLED";

            txt(ctx, status, x + 13, ly + cardH - 15,
                LovenotePalette.lerp(
                    LovenotePalette.faint(), accent, on));

            int switchX = x + cardW - 35;
            int switchY = ly + cardH - 18;

            drawSwitch(ctx, switchX, switchY, on);

            // Existing interaction handling
            addHit(x, ly, cardW, cardH,
                K_CARD, m, null, 0);

            addHit(switchX - 4, switchY - 5,
                34, 22, K_SWITCH, m, null, 0);

            if (hov && LovenoteMenuConfig.tooltips
                && !name.equals(m.title()))
                tooltip = m.title();
        }

        ctx.disableScissor();

        scrollbar(ctx, cx + cw + 6,
            top, bottom, scrollShown, maxScroll);

        if (list.isEmpty())
            txt(ctx, "No modules found",
                cx + 4, top + 8, LovenotePalette.faint());
    }
    private void drawThemePresets(GuiGraphicsExtractor ctx,
                                  int x, int top, int width, int bottom) {
        txt(ctx, "APPEARANCE", x + 8, top + 8, LovenotePalette.text());
        txt(ctx, "Choose your accent colour",
            x + 8, top + 25, LovenotePalette.dim());

        int cols = 7;
        int gap = 8;
        int size = Math.min(34, Math.max(18,
            (width - 16 - gap * (cols - 1)) / cols));

        int startX = x + 8;
        int startY = top + 50;

        ctx.enableScissor(x, top, x + width, bottom);

        for (int i = 0; i < LovenotePalette.PRESETS.length; i++) {
            int bx = startX + (i % cols) * (size + gap);
            int by = startY + (i / cols) * (size + gap);

            if (by + size > bottom) break;

            int color = 0xFF000000 | LovenotePalette.PRESETS[i][0];

            if (LovenoteMenuConfig.accentIndex == i) {
                rr(ctx, bx - 2, by - 2, size + 4, size + 4,
                    9, LovenotePalette.alpha(color, 140));
            }

            rr(ctx, bx, by, size, size, 8, color);
            addHit(bx, by, size, size, K_ACCENT, null, null, i);
        }

        ctx.disableScissor();
    }
    private String trim(String s, int w) {
        if (font.width(s) <= w) return s;
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) { if (font.width(b.toString() + c + "..") > w) break; b.append(c); }
        return b + "..";
    }

    private void drawRail(GuiGraphicsExtractor ctx, int mx, int my, int corner) {
        int rx = railX();
        boolean hover = dragWin == 0 && !animating && mx >= rx && mx < rx + railW && my >= wy && my < wy + winH;
        float sb = ease("sb", hover ? 1f : 0f, LovenoteMenuConfig.railSpeed);
        int sbw = RAIL_MIN + (int) ((railW - RAIL_MIN) * sb);
        int sx = LovenoteMenuConfig.railRight ? rx + railW - sbw : rx;

        if (sb > 0.02f) for (int i = 5; i >= 1; i--) rr(ctx, sx + sbw - 2, wy + 4, i * 2 + 2, winH - 8, 8, LovenotePalette.alpha(0x000000, (int) (9 * sb)));
        rr(ctx, sx, wy, sbw, winH, corner, LovenotePalette.dark ? 0xFA0C0C12 : 0xFAEDEDF4);
        grad(ctx, sx + 2, wy + 8, sx + sbw - 2, wy + 9, LovenotePalette.alpha(LovenotePalette.C1, 90), LovenotePalette.alpha(LovenotePalette.C3, 90));

        String q = searchText.trim();
        int avail = winH - 56 - 40;
        int rowH = Mth.clamp(avail / Math.max(1, pages()), 16, 24);
        int y = wy + 56;
        int[] ys = new int[pages()];
        for (int i = 0; i < pages(); i++) { ys[i] = y; y += rowH; }

        int targetY = ys[Mth.clamp(page, 0, pages() - 1)];
        pillY = pillY < 0f ? targetY : pillY + (targetY - pillY) * (1f - (float) Math.exp(-dt / 70f));
        if (q.isEmpty() && sb > 0.05f) {
            rr(ctx, sx + 5, Math.round(pillY), sbw - 10, rowH, 6, LovenotePalette.alpha(LovenotePalette.C2, (int) (70 * sb)));
            rr(ctx, sx + 5, Math.round(pillY) + rowH / 2 - 7, 2, 14, 1, LovenotePalette.alpha(LovenotePalette.wave(0f), (int) (255 * sb)));
        }

        for (int i = 0; i < pages(); i++) {
            int ry = ys[i];
            boolean hov = hover && mx >= sx + 5 && mx < sx + sbw - 5 && my >= ry && my < ry + rowH;
            float hv = ease("pg" + i, hov ? 1f : 0f, 120);
            if (hv > 0.02f && i != page) rr(ctx, sx + 5, ry, sbw - 10, rowH, 6, LovenotePalette.alpha(LovenotePalette.C2, (int) (26 * hv)));
            float slide = (1f - Mth.clamp((tabMs - i * 12f) / 220f, 0f, 1f)) * 10f;
            ItemStack icon = i < RAIL_ICONS.length ? RAIL_ICONS[i] : new ItemStack(Items.PAINTING);
            ctx.item(icon, sx + 13 + (int) slide, ry + (rowH - 16) / 2);
            if (sb > 0.05f) txt(ctx, pageLabel(i), sx + 42 + (int) slide, ry + (rowH - 8) / 2,
                    LovenotePalette.alpha(LovenotePalette.lerp(LovenotePalette.dim(), LovenotePalette.text(), Math.max(hv * 0.7f, i == page ? 1f : 0f)), (int) (255 * sb)));
            pageRects.add(new int[]{sx + 5, ry, sbw - 10, rowH, i});
        }
    }

    private void drawPanel(GuiGraphicsExtractor ctx, int mx, int my, float panelA, int corner) {
        Row m = panelModule;
        float saved = ga;
        ga = saved * panelA;

        Matrix3x2fStack ms = ctx.pose();
        float sc = 0.94f + 0.06f * panelA;
        float ccx = panX + PW / 2f, ccy = panY + PH / 2f;
        ms.pushMatrix(); ms.translate(ccx, ccy); ms.scale(sc, sc); ms.translate(-ccx, -ccy);

        int px = panX + (int) ((1f - panelA) * 22), py = panY;
        for (int i = 7; i >= 1; i--) rr(ctx, px - i, py - i, PW + i * 2, PH + i * 2, corner, LovenotePalette.alpha(LovenotePalette.C2, 5));
        rr(ctx, px - 1, py - 1, PW + 2, PH + 2, corner + 1, LovenotePalette.alpha(LovenotePalette.C2, 70));
        rr(ctx, px, py, PW, PH, corner, LovenotePalette.alpha(LovenotePalette.card(), 0xF8));

        txtS(ctx, m.title(), px + 13, py + 10, LovenotePalette.text());
        txt(ctx, trim(m.description(), PW - 84), px + 13, py + 23, LovenotePalette.faint());

        float on = ease(m.title() + ":s", m.active() ? 1f : 0f, 170);
        drawSwitch(ctx, px + PW - 74, py + 12, on);
        addHit(px + PW - 77, py + 8, 28, 18, K_PTOGGLE, m, null, 0);
        circle(ctx, px + PW - 40, py + 17, 4, 0xFF3DD68C);
        circle(ctx, px + PW - 28, py + 17, 4, 0xFFF5B83D);
        circle(ctx, px + PW - 16, py + 17, 4, 0xFFEF4444);
        addHit(px + PW - 22, py + 11, 12, 12, K_PCLOSE, m, null, 0);

        fill(ctx, px + 9, py + 37, px + PW - 9, py + 38, LovenotePalette.alpha(LovenotePalette.track(), 170));

        int top = py + 42, bottom = py + PH - 34;
        int total = 4 + panelTotal(m, PW - 26);
        panelMax = Math.max(0, total - (bottom - top));
        panelScrollTarget = Mth.clamp(panelScrollTarget, 0, panelMax);
        panelScrollShown += (panelScrollTarget - panelScrollShown) * (1f - (float) Math.exp(-dt / 40f));
        if (Math.abs(panelScrollTarget - panelScrollShown) < 0.2f) panelScrollShown = panelScrollTarget;

        ctx.enableScissor(px, top, px + PW, bottom);
        int y = top + 4 - Math.round(panelScrollShown);
        boolean first = true;
        for (Group g : m.groups()) {
            boolean any = false;
            for (Object s : g.settings()) if (rowH(s, PW - 26) > 0) { any = true; break; }
            if (!any) continue;
            if (!first) fill(ctx, px + 13, y - 2, px + PW - 13, y - 1, LovenotePalette.alpha(LovenotePalette.track(), 120));
            first = false;
            y += 4;
            txt(ctx, g.name(), px + 13, y, LovenotePalette.alpha(LovenotePalette.faint(), 190));
            y += 14;
            for (Object s : g.settings()) y += drawSetting(ctx, m, s, px + 13, y, PW - 26);
        }
        ctx.disableScissor();
        scrollbar(ctx, px + PW - 6, top, bottom, panelScrollShown, panelMax);

        ms.popMatrix();
        ga = saved;
    }

    // ---------------------------------------------------------------- input

    private boolean over(Hit h, double mx, double my) { return mx >= h.x && mx <= h.x + h.w && my >= h.y && my <= h.y + h.h; }

    private void applyAccent(int idx) {
        if (idx >= 0 && idx < LovenotePalette.PRESETS.length) LovenoteMenuConfig.accentIndex = idx;
        LovenoteMenuConfig.apply();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = click.x() / gs, my = click.y() / gs;
        int button = click.button();

        if (listening != null) { listening.setKey(button == 1 ? -1 : GLFW.GLFW_MOUSE_BUTTON_LEFT); listening = null; return true; }
        if (button == 1 && selected != null) { selected = null; panelModule = null; return true; }
        if (button != 0 && button != 1) return super.mouseClicked(click, doubled);

        searchFocused = false;

        for (int[] r : pageRects) {
            if (mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3]) {
                page = r[4]; tabMs = 0; scrollTarget = 0; scrollShown = 0; searchText = ""; addRipple((int) mx, (int) my);
                return true;
            }
        }

        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (!over(hit, mx, my)) continue;
            if (hit.kind == K_NONE) continue;
            switch (hit.kind) {
                case K_CARD -> { selected = hit.module; panelModule = hit.module; panelScrollTarget = 0; }
                case K_SWITCH -> hit.module.toggle();
                case K_BOOL -> {
                    if (hit.setting instanceof BoolSetting b) b.set(!b.get());
                    else if (hit.setting instanceof com.lovenote.setting.BoolSetting b) b.toggle();
                }
                case K_MULTI -> { if (hit.setting instanceof com.lovenote.setting.MultiSetting ms) ms.toggle(hit.idx); }
                case K_SLIDER -> { dragWin = 3; dragging = hit; setSlider(hit, mx); }
                case K_MODE -> {
                    if (hit.setting instanceof EnumSetting<?> es) {
                        Object[] vals = enumValues(es);
                        if (hit.idx >= 0 && hit.idx < vals.length) setEnum(es, vals[hit.idx]);
                    } else if (hit.setting instanceof com.lovenote.setting.ModeSetting ms) ms.setIndex(hit.idx);
                }
                case K_PTOGGLE -> hit.module.toggle();
                case K_PCLOSE -> { selected = null; panelModule = null; }
                case K_BIND -> { if (hit.module != null) listening = hit.module; }
                case K_ACCENT -> applyAccent(hit.idx);
                case K_SEARCH -> { searchFocused = true; return true; }
                case K_CLOSE -> close();
                default -> { }
            }
            return true;
        }

        if (button == 0 && selected != null && mx >= panX && mx <= panX + PW && my >= panY && my <= panY + 34) {
            dragWin = 2; grabX = (int) mx - panX; grabY = (int) my - panY; return true;
        }
        if (button == 0 && mx >= wx && mx <= wx + winW && my >= wy && my <= wy + 34) {
            dragWin = 1; grabX = (int) mx - wx; grabY = (int) my - wy; return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private void setSlider(Hit h, double mx) {
        double t = Mth.clamp((mx - h.x) / (double) h.w, 0.0, 1.0);
        if (h.setting instanceof IntSetting n) n.set((int) (n.sliderMin + t * (n.sliderMax - n.sliderMin)));
        else if (h.setting instanceof DoubleSetting n) n.set(n.sliderMin + t * (n.sliderMax - n.sliderMin));
        else if (h.setting instanceof com.lovenote.setting.NumberSetting n) n.set(n.min + t * (n.max - n.min));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        double mx = click.x() / gs, my = click.y() / gs;
        if (dragWin == 1) { mainX = (int) mx - grabX; mainY = (int) my - grabY; clampWindows(); return true; }
        if (dragWin == 2) { panX = (int) mx - grabX; panY = (int) my - grabY; clampWindows(); return true; }
        if (dragWin == 3) {
            if (dragging != null) setSlider(dragging, mx);
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) { dragWin = 0; dragging = null; return super.mouseReleased(click); }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double amount) {
        double s = mc.getWindow().getGuiScale();
        double x = mx / s / gs, y = my / s / gs;
        if (InputConstants.isKeyDown(windowHandle(), GLFW.GLFW_KEY_LEFT_CONTROL)) {
            LovenoteMenuConfig.menuScale = (int) Mth.clamp(LovenoteMenuConfig.menuScale + amount * 5, 40, 110);
            return true;
        }
        if (selected != null && x >= panX && x <= panX + PW && y >= panY && y <= panY + PH) {
            panelScrollTarget = Mth.clamp(panelScrollTarget - (float) amount * 24f, 0f, (float) panelMax);
            return true;
        }
        scrollTarget = Mth.clamp(scrollTarget - (float) amount * 28f, 0f, maxScroll);
        sbAlpha = 1f;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.key();
        if (listening != null) { listening.setKey(key == GLFW.GLFW_KEY_ESCAPE ? -1 : key); listening = null; return true; }
        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchText.isEmpty()) { searchText = searchText.substring(0, searchText.length() - 1); onSearch(); }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER) { searchFocused = false; return true; }
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (searchFocused) { searchFocused = false; return true; }
            close(); return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (searchFocused) {
            int cp = input.codepoint();
            if (cp >= 32 && searchText.length() < 32) { searchText += (char) cp; onSearch(); return true; }
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void mouseMoved(double mx, double my) {
    }
}

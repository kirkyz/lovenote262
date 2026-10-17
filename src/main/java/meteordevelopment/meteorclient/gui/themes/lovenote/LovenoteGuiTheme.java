package meteordevelopment.meteorclient.gui.themes.lovenote;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.screens.LovenoteModulesScreen;
import net.minecraft.client.gui.screens.Screen;

import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.themes.lovenote.widgets.WLovenoteModule;
import meteordevelopment.meteorclient.gui.themes.lovenote.widgets.WLovenoteQuad;
import meteordevelopment.meteorclient.gui.themes.lovenote.widgets.WLovenoteTopBar;
import meteordevelopment.meteorclient.gui.themes.lovenote.widgets.WLovenoteWindow;
import meteordevelopment.meteorclient.gui.themes.lovenote.widgets.pressable.WLovenoteButton;
import meteordevelopment.meteorclient.gui.themes.lovenote.widgets.pressable.WLovenoteCheckbox;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WQuad;
import meteordevelopment.meteorclient.gui.widgets.WTopBar;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class LovenoteGuiTheme extends MeteorGuiTheme {
    private final SettingGroup sgLovenote = settings.createGroup("Lovenote");

    public final Setting<SettingColor> secondaryColor = sgLovenote.add(new ColorSetting.Builder()
        .name("secondary-color")
        .description("Second gradient stop.")
        .defaultValue(new SettingColor(178, 141, 255))
        .build()
    );

    public final Setting<LovenotePresets> preset = sgLovenote.add(new EnumSetting.Builder<LovenotePresets>()
        .name("preset")
        .description("Accent preset (each is a gradient pair).")
        .defaultValue(LovenotePresets.Lovenote)
        .onChanged(p -> {
            accentColor.set(p.primary);
            secondaryColor.set(p.secondary);
        })
        .build()
    );

    public final Setting<Boolean> gradients = sgLovenote.add(new BoolSetting.Builder()
        .name("gradients")
        .description("Use gradients on headers and accents.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> animatedGradient = sgLovenote.add(new BoolSetting.Builder()
        .name("animated-gradient")
        .description("Slowly scroll the accent gradient.")
        .defaultValue(true)
        .build()
    );

    public LovenoteGuiTheme() {
        super("Lovenote");

        accentColor.set(LovenotePresets.Lovenote.primary);
        checkboxColor.set(LovenotePresets.Lovenote.primary);
        favoriteColor.set(LovenotePresets.Lovenote.primary);
        titleTextColor.set(new SettingColor(255, 255, 255));
        textColor.set(new SettingColor(235, 235, 242));
        textSecondaryColor.set(new SettingColor(148, 148, 160));
        placeholderColor.set(new SettingColor(255, 255, 255, 30));

        backgroundColor.set(
            new SettingColor(16, 16, 21, 220),
            new SettingColor(23, 23, 30, 232),
            new SettingColor(31, 31, 40, 244)
        );
        outlineColor.set(
            new SettingColor(255, 255, 255, 18),
            new SettingColor(255, 255, 255, 34),
            new SettingColor(255, 255, 255, 52)
        );
        scrollbarColor.set(
            new SettingColor(255, 255, 255, 16),
            new SettingColor(255, 255, 255, 30),
            new SettingColor(255, 255, 255, 44)
        );
        sliderHandle.set(
            LovenotePresets.Lovenote.primary,
            new SettingColor(255, 150, 215),
            new SettingColor(255, 170, 225)
        );
        sliderLeft.set(LovenotePresets.Lovenote.primary);
        sliderRight.set(new SettingColor(255, 255, 255, 24));

        moduleBackground.set(new SettingColor(255, 255, 255, 12));
        separatorText.set(new SettingColor(255, 255, 255));
        separatorCenter.set(new SettingColor(255, 255, 255, 50));
        separatorEdges.set(new SettingColor(255, 255, 255, 14));
    }

    // ---------------------------------------------------------------- radii

    public double radius() {
        return scale(8);
    }

    public double cardRadius() {
        return scale(9);
    }

    public double windowRadius() {
        return scale(12);
    }

    public double pillRadius() {
        return scale(9);
    }

    // ---------------------------------------------------------------- surfaces

    public Color panelColor() {
        return backgroundColor.get();
    }

    public Color panelTop() {
        SettingColor c = backgroundColor.get();
        return new SettingColor(c.r, c.g, c.b, Math.min(255, c.a + 12));
    }

    public Color panelBottom() {
        SettingColor c = backgroundColor.get();
        return new SettingColor(Math.max(0, c.r - 6), Math.max(0, c.g - 6), Math.max(0, c.b - 6), c.a);
    }

    public Color headerColor() {
        return new SettingColor(255, 255, 255, 12);
    }

    public Color moduleRowBackground(boolean active, boolean hovered) {
        SettingColor a = accentColor.get();
        if (active) return new SettingColor(a.r, a.g, a.b, hovered ? 60 : 40);
        return new SettingColor(255, 255, 255, hovered ? 26 : 16);
    }

    public Color accentText() {
        SettingColor c = accentColor.get();
        return new SettingColor(Math.min(255, c.r + 70), Math.min(255, c.g + 70), Math.min(255, c.b + 70), 255);
    }

    public Color checkboxBackground(boolean pressed, boolean hovered) {
        return new SettingColor(255, 255, 255, hovered ? 32 : 18);
    }

    public Color buttonColor(boolean pressed, boolean hovered) {
        if (pressed) return accentTop();
        return new SettingColor(255, 255, 255, hovered ? 28 : 15);
    }

    // ---------------------------------------------------------------- edges / depth

    public Color hairline() {
        return new SettingColor(255, 255, 255, 20);
    }

    public Color hairlineStrong() {
        return new SettingColor(255, 255, 255, 38);
    }

    public Color innerTop() {
        return new SettingColor(255, 255, 255, 30);
    }

    public Color shadowColor() {
        return new SettingColor(0, 0, 0, 120);
    }

    public Color trackColor() {
        return new SettingColor(255, 255, 255, 22);
    }

    public Color microText() {
        return new SettingColor(150, 150, 164, 200);
    }

    // ---------------------------------------------------------------- cards

    public Color cardTop(boolean hovered) {
        return new SettingColor(255, 255, 255, hovered ? 24 : 14);
    }

    public Color cardTopActive(boolean hovered) {
        SettingColor a = accentColor.get();
        return new SettingColor(a.r, a.g, a.b, hovered ? 58 : 36);
    }

    public Color cardBottom() {
        return new SettingColor(10, 10, 14, 145);
    }

    public Color glowColor() {
        SettingColor a = accentColor.get();
        return new SettingColor(a.r, a.g, a.b, 255);
    }

    // ---------------------------------------------------------------- accent

    public float phase() {
        if (!animatedGradient.get()) return 0f;
        return (System.currentTimeMillis() % 6000L) / 6000f;
    }

    private Color sample(float t) {
        t = ((t % 1f) + 1f) % 1f;
        SettingColor a = accentColor.get();
        SettingColor b = secondaryColor.get();

        return new SettingColor(
            (int) (a.r + (b.r - a.r) * t),
            (int) (a.g + (b.g - a.g) * t),
            (int) (a.b + (b.b - a.b) * t),
            a.a
        );
    }

    public Color accentTop() {
        if (!gradients.get()) return accentColor.get();
        return sample(phase());
    }

    public Color accentBottom() {
        if (!gradients.get()) return accentColor.get();
        return sample(phase() + 0.5f);
    }

    public Color accentAt(double offset) {
        if (!gradients.get()) return accentColor.get();
        return sample((float) (phase() + offset));
    }

    // ---------------------------------------------------------------- widgets

    @Override
    protected WButton button(String text, GuiTexture texture) {
        return w(new WLovenoteButton(text, texture));
    }

    @Override
    public WQuad quad(Color color) {
        return w(new WLovenoteQuad(color));
    }

    @Override
    public WCheckbox checkbox(boolean checked) {
        return w(new WLovenoteCheckbox(checked));
    }

    @Override
    public WTopBar topBar() {
        return w(new WLovenoteTopBar());
    }

    public WWindow window(String title) {
        return w(new WLovenoteWindow(null, title));
    }

    public WWidget module(Module module, String title) {
        return w(new WLovenoteModule(module, title));
    }

    @Override
    public WWindow window(WWidget icon, String title) {
        return w(new WLovenoteWindow(icon, title));
    }

    public Color cardBackground(boolean hovered, double anim) {
        SettingColor a = accentColor.get();
        int alpha = (int) (14 + 26 * anim + (hovered ? 12 : 0));
        return new SettingColor(a.r, a.g, a.b, alpha);
    }

    public Color toggleTrack(double anim) {
        SettingColor a = accentColor.get();
        SettingColor b = secondaryColor.get();
        return new SettingColor(
            (int) (a.r + (b.r - a.r) * anim),
            (int) (a.g + (b.g - a.g) * anim),
            (int) (a.b + (b.b - a.b) * anim),
            (int) (60 + 140 * anim)
        );
    }

    public Color toggleOff() {
        return new SettingColor(255, 255, 255, 26);
    }

    public Color toggleKnob() {
        return new SettingColor(245, 245, 250, 255);
    }

    @Override
    public TabScreen modulesScreen() {
        return new LovenoteModulesScreen(this);
    }

    @Override
    public boolean isLovenoteModulesScreen(Screen screen) {
        return screen instanceof LovenoteModulesScreen;
    }
}

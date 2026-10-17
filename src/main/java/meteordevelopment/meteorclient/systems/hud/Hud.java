package meteordevelopment.meteorclient.systems.hud;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;

public class Hud {
    private static final Hud INSTANCE = new Hud();
    public static Hud get() { return INSTANCE; }

    private boolean enabled = true;
    private final List<HudElement> elements = new ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public void toggle() { enabled = !enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void add(HudElement element) { elements.add(element); }
    public void remove(HudElement element) { elements.remove(element); }
    public List<HudElement> getElements() { return elements; }
    public WWidget getWidget(GuiTheme theme) { return null; }
    public void init() {}
}

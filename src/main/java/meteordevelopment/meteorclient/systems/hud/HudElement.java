package meteordevelopment.meteorclient.systems.hud;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;

public class HudElement {
    public boolean active = true;
    public double x, y;

    public void render(HudRenderer renderer) {}
    public WWidget getWidget(GuiTheme theme) { return null; }
    public boolean isActive() { return active; }
    public void toggle() { active = !active; }
}

package meteordevelopment.meteorclient.gui.widgets.pressable;

import meteordevelopment.meteorclient.gui.widgets.WWidget;

public class WPressable extends WWidget {
    public Runnable action;
    public boolean pressed;

    public WPressable() {}
    public WPressable(Runnable action) { this.action = action; }
}

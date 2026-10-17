package meteordevelopment.meteorclient.gui.widgets.input;

import meteordevelopment.meteorclient.gui.widgets.WWidget;

public class WIntEdit extends WWidget {
    public int value, min, max;
    public Runnable action;

    public WIntEdit(int value, int min, int max, Runnable action) {
        this.value = value; this.min = min; this.max = max; this.action = action;
    }
}

package meteordevelopment.meteorclient.gui.widgets.input;

import meteordevelopment.meteorclient.gui.widgets.WWidget;

public class WDoubleEdit extends WWidget {
    public double value, min, max;
    public Runnable action;

    public WDoubleEdit(double value, double min, double max, Runnable action) {
        this.value = value; this.min = min; this.max = max; this.action = action;
    }
}

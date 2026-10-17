package meteordevelopment.meteorclient.gui.widgets.input;

import meteordevelopment.meteorclient.gui.widgets.WWidget;

public class WSlider extends WWidget {
    public double value, min, max;

    public WSlider(double value, double min, double max) { this.value = value; this.min = min; this.max = max; }
}

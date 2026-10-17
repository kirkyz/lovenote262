package meteordevelopment.meteorclient.gui.widgets.input;

import meteordevelopment.meteorclient.gui.widgets.WWidget;

public class WTextBox extends WWidget {
    public String text;
    public Runnable action;
    public boolean centered;

    public WTextBox(String text) { this.text = text; }
    public WTextBox(String text, Runnable action) { this.text = text; this.action = action; }

    public String get() { return text; }
    public void set(String text) { this.text = text; }
    public WTextBox setFilter(Object filter) { return this; }
    public WTextBox setRenderer(Object renderer) { return this; }
}

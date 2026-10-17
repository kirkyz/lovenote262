package meteordevelopment.meteorclient.gui.widgets;

public class WLabel extends WWidget {
    public String text;
    public boolean title;

    public WLabel(String text) { this(text, false); }
    public WLabel(String text, boolean title) { this.text = text; this.title = title; }

    public WLabel color(int color) { return this; }
}

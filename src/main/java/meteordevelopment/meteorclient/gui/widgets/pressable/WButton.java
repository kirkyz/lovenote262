package meteordevelopment.meteorclient.gui.widgets.pressable;

public class WButton extends WPressable {
    public String text;

    public WButton(String text) { this.text = text; }
    public WButton(String text, Runnable action) { super(action); this.text = text; }
}

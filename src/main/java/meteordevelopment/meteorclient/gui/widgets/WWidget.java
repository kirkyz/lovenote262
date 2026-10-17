package meteordevelopment.meteorclient.gui.widgets;

public abstract class WWidget {
    public double x, y, width, height;
    public boolean visible = true;
    public WWidget parent;

    public void render() {}
    public void layout() {}
    public double pad() { return 0; }
    public void move(double x, double y) { this.x = x; this.y = y; }
    public void setSize(double width, double height) { this.width = width; this.height = height; }
    public boolean isVisible() { return visible; }
    public void remove() {}
    public void clear() {}
    public void tick() {}
    public WWidget getWidget() { return this; }

    public WWidget expandX() { return this; }
    public WWidget expandY() { return this; }
    public WWidget expandCellX() { return this; }
    public WWidget expandCellY() { return this; }
    public WWidget pad(int padding) { return this; }
    public WWidget centerX() { return this; }
    public WWidget centerY() { return this; }

    public boolean onMouseClicked(double mx, double my, int button) { return false; }
    public boolean onMouseReleased(double mx, double my, int button) { return false; }
    public boolean onMouseScrolled(double mx, double my, double amount) { return false; }
    public void onMouseMoved(double mx, double my) {}
    public boolean onKeyPressed(int key, int modifiers) { return false; }
    public boolean onKeyRepeated(int key, int modifiers) { return false; }
    public boolean onCharTyped(char c, int modifiers) { return false; }
}

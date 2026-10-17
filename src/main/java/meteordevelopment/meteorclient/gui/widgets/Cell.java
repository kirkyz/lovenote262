package meteordevelopment.meteorclient.gui.widgets;

public class Cell<T extends WWidget> {
    public final T widget;
    public boolean expandX, expandY, expandCellX, expandCellY;
    public int padding;
    public double alignX = 0.5, alignY = 0.5;

    public Cell(T widget) { this.widget = widget; }

    public Cell<T> expandX() { expandX = true; return this; }
    public Cell<T> expandY() { expandY = true; return this; }
    public Cell<T> expandCellX() { expandCellX = true; return this; }
    public Cell<T> expandCellY() { expandCellY = true; return this; }
    public Cell<T> pad() { padding = 4; return this; }
    public Cell<T> pad(int padding) { this.padding = padding; return this; }
    public Cell<T> padHorizontal() { return this; }
    public Cell<T> padVertical() { return this; }
    public Cell<T> alignX(double a) { alignX = a; return this; }
    public Cell<T> alignY(double a) { alignY = a; return this; }
    public Cell<T> centerX() { alignX = 0.5; return this; }
    public Cell<T> centerY() { alignY = 0.5; return this; }
    public Cell<T> left() { alignX = 0; return this; }
    public Cell<T> right() { alignX = 1; return this; }
    public Cell<T> top() { alignY = 0; return this; }
    public Cell<T> bottom() { alignY = 1; return this; }
    public Cell<T> minWidth(double w) { return this; }
    public Cell<T> maxWidth(double w) { return this; }
}

package meteordevelopment.meteorclient.gui.widgets.containers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import meteordevelopment.meteorclient.gui.widgets.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;

public class WContainer extends WWidget {
    protected final List<WWidget> widgets = new ArrayList<>();

    public <T extends WWidget> Cell<T> add(T widget) { widget.parent = this; widgets.add(widget); return new Cell<>(widget); }
    public void remove(WWidget widget) { widgets.remove(widget); }
    public void clear() { widgets.clear(); }
    public Iterator<WWidget> iterator() { return widgets.iterator(); }
    public List<WWidget> getWidgets() { return widgets; }
}

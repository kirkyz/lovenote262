package meteordevelopment.meteorclient.gui.widgets.input;

import java.util.function.Consumer;
import meteordevelopment.meteorclient.gui.widgets.WWidget;

public class WDropdown<T> extends WWidget {
    public T[] values;
    public T value;
    public Consumer<T> action;

    public WDropdown(T[] values, T value, Consumer<T> action) {
        this.values = values;
        this.value = value;
        this.action = action;
    }
}

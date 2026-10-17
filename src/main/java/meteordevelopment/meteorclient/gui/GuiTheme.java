package meteordevelopment.meteorclient.gui;

import java.util.function.Consumer;
import java.util.function.Supplier;
import meteordevelopment.meteorclient.gui.widgets.Cell;
import meteordevelopment.meteorclient.gui.widgets.WItem;
import meteordevelopment.meteorclient.gui.widgets.WKeybind;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WScrollable;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WBlockPosEdit;
import meteordevelopment.meteorclient.gui.widgets.input.WDoubleEdit;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.settings.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public class GuiTheme {
    public final String name;
    public final Settings settings = new Settings();
    public boolean scrollOnlyWhenMouseOver = true;
    public boolean alwaysShowScrollbar;
    public boolean categoryIcons = true;

    public GuiTheme(String name) { this.name = name; }

    public void beforeRender() {}
    public void afterRender() {}

    public <T extends WWidget> Cell<T> add(T widget) { return new Cell<>(widget); }

    public WVerticalList verticalList() { return new WVerticalList(); }
    public WHorizontalList horizontalList() { return new WHorizontalList(); }
    public WTable table() { return new WTable(); }
    public WSection section(String title) { return new WSection(title); }
    public WView view() { return new WView(); }
    public WScrollable scrollable() { return new WScrollable(); }
    public WWindow window(String title) { return new WWindow(title); }

    public WLabel label(String text) { return new WLabel(text); }
    public WLabel label(String text, boolean title) { return new WLabel(text, title); }
    public WButton button(String text) { return new WButton(text); }
    public WButton button(String text, Runnable action) { return new WButton(text, action); }
    public WMinus minus() { return new WMinus(); }
    public WPlus plus() { return new WPlus(); }
    public WConfirmedMinus confirmedMinus(Runnable action) { return new WConfirmedMinus(action); }
    public WCheckbox checkbox(boolean checked) { return new WCheckbox(checked); }
    public WTextBox textBox(String text) { return new WTextBox(text); }
    public WTextBox textBox(String text, Runnable action) { return new WTextBox(text, action); }
    public WKeybind keybind(int key) { return new WKeybind(key); }
    public WItem item(ItemStack item) { return new WItem(item); }

    public WIntEdit intEdit(int value, int min, int max, Runnable action) { return new WIntEdit(value, min, max, action); }
    public WDoubleEdit doubleEdit(double value, double min, double max, Runnable action) { return new WDoubleEdit(value, min, max, action); }
    public WBlockPosEdit blockPosEdit(BlockPos value, Runnable action) { return new WBlockPosEdit(value, action); }
    public WSlider slider(double value, double min, double max) { return new WSlider(value, min, max); }

    public <T> WDropdown<T> dropdown(T[] values, T value, Consumer<T> action) { return new WDropdown<>(values, value, action); }
    public <T> WDropdown<T> dropdown(Supplier<T[]> values, T value, Consumer<T> action) { return new WDropdown<>(values.get(), value, action); }
}

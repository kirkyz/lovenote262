package meteordevelopment.meteorclient.gui.tabs;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;

public abstract class TabScreen extends WidgetScreen {
    public final Tab tab;

    public TabScreen(Tab tab, GuiTheme theme) { super(theme, tab == null ? "Screen" : tab.name); this.tab = tab; }
}

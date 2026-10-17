package meteordevelopment.meteorclient.gui.tabs;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.gui.GuiTheme;

public class Tabs {
    private static final Tabs INSTANCE = new Tabs();
    public static Tabs get() { return INSTANCE; }

    private final List<Tab> tabs = new ArrayList<>();

    public void add(Tab tab) { tabs.add(tab); }
    public void remove(Tab tab) { tabs.remove(tab); }
    public List<Tab> getAll() { return tabs; }

    public <T extends Tab> T get(Class<T> klass) {
        for (Tab tab : tabs) if (klass.isInstance(tab)) return klass.cast(tab);
        return null;
    }

    public TabScreen getScreen(GuiTheme theme, Class<? extends Tab> klass) {
        Tab tab = get(klass);
        return tab == null ? null : tab.createScreen(theme);
    }

    public void init() {}
}

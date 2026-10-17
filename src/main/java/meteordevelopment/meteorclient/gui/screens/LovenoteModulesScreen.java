package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.themes.lovenote.widgets.WLovenoteCard;
import meteordevelopment.meteorclient.gui.themes.lovenote.widgets.WLovenoteTab;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class LovenoteModulesScreen extends TabScreen {
    private static final int COLUMNS = 2;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static Category selected;

    private final Map<Category, WLovenoteTab> categoryTabs = new LinkedHashMap<>();
    private WVerticalList grid;
    private WLabel activeLabel;
    private WTextBox search;
    private String query = "";

    public LovenoteModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
    }

    @Override
    public void initWidgets() {
        categoryTabs.clear();

        if (selected == null) {
            for (Category c : Modules.loopCategories()) { selected = c; break; }
        }

        WWindow window = add(theme.window("lovenote")).center().widget();
        WVerticalList root = window.add(theme.verticalList()).expandX().widget();

        WHorizontalList header = root.add(theme.horizontalList()).expandX().widget();
        header.add(theme.label("lovenote", true));
        header.add(theme.label("")).expandX();
        activeLabel = header.add(theme.label("")).widget();
        header.add(theme.label(mc.getFps() + " fps"));
        header.add(theme.label(LocalTime.now().format(TIME)));

        search = root.add(theme.textBox("", "search modules...")).expandX().widget();
        search.setFocused(true);
        search.action = () -> {
            query = search.get().trim().toLowerCase();
            refresh();
        };

        WHorizontalList body = root.add(theme.horizontalList()).expandX().widget();

        WVerticalList sidebar = body.add(theme.verticalList()).widget();
        for (Category category : Modules.loopCategories()) {
            WLovenoteTab tab = new WLovenoteTab(category.name, category == selected, () -> select(category));
            categoryTabs.put(category, tab);
            sidebar.add(tab).expandX();
        }

        WVerticalList content = body.add(theme.verticalList()).expandX().widget();
        grid = content.add(theme.verticalList()).expandX().widget();

        WHorizontalList footer = root.add(theme.horizontalList()).expandX().widget();
        for (Tab tab : Tabs.get()) {
            if (tab.name.equals("Modules")) continue;
            footer.add(new WLovenoteTab(tab.name, false, () -> tab.openScreen(theme)));
        }

        refresh();
    }

    private void select(Category category) {
        selected = category;
        if (!query.isEmpty()) {
            query = "";
            search.set("");
        }
        for (Map.Entry<Category, WLovenoteTab> e : categoryTabs.entrySet()) {
            e.getValue().setSelected(e.getKey() == category);
        }
        refresh();
    }

    private void refresh() {
        if (grid == null) return;
        grid.clear();

        List<Module> modules = new ArrayList<>();
        if (!query.isEmpty()) {
            for (Module m : Modules.get().getAll()) {
                if (m.title.toLowerCase().contains(query) || m.name.toLowerCase().contains(query)) modules.add(m);
            }
        } else if (selected != null) {
            modules.addAll(Modules.get().getGroup(selected));
        }

        if (modules.isEmpty()) {
            grid.add(theme.label("No modules found."));
        } else {
            WHorizontalList row = null;
            for (int i = 0; i < modules.size(); i++) {
                if (i % COLUMNS == 0) row = grid.add(theme.horizontalList()).expandX().widget();
                row.add(new WLovenoteCard(modules.get(i), this::updateCount)).expandX();
            }
            while (modules.size() % COLUMNS != 0 && row != null && row.cells.size() < COLUMNS) {
                row.add(theme.label("")).expandX();
            }
        }

        updateCount();
    }

    private void updateCount() {
        if (activeLabel != null) activeLabel.set(Modules.get().getActive().size() + " active");
    }
}

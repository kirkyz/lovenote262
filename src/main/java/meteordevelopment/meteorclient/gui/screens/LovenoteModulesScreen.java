package meteordevelopment.meteorclient.gui.screens;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.themes.lovenote.widgets.WLovenoteCard;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class LovenoteModulesScreen extends TabScreen {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private Category selected;
    private WContainer grid;
    private WVerticalList list;

    public LovenoteModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
    }

    @Override
    public void initWidgets() {
        if (selected == null) {
            for (Category category : Modules.loopCategories()) {
                selected = category;
                break;
            }
        }

        // header: wordmark, wide search, stats on the right
        header.add(theme.label("lovenote", true));
        header.add(theme.textBox("search modules...")).expandX();
        header.add(theme.label(mc.getUser().getName()));
        header.add(theme.label(mc.getFps() + " fps"));
        header.add(theme.label(LocalTime.now().format(TIME)));

        // sidebar
        WVerticalList sidebar = body.add(theme.verticalList()).widget();
        for (Category category : Modules.loopCategories()) {
            sidebar.add(theme.button(category.name)).expandX();
        }

        // content: category heading + card grid
        WVerticalList content = body.add(theme.verticalList()).expandX().expandWidgetY().widget();
        if (selected != null) content.add(theme.label(selected.name, true));

        list = content.add(theme.verticalList()).expandX().expandWidgetY().widget();

        // footer
        footer.add(theme.label("lovenote - drag to move")).expandX();
        footer.add(theme.label(Modules.get().getAll().size() + " modules"));

        refresh();
    }

    private void refresh() {
        if (list == null) return;
        list.clear();

        if (selected == null) return;

        List<Module> modules = new ArrayList<>(Modules.get().getGroup(selected));

        WHorizontalList row = null;
        for (int i = 0; i < modules.size(); i++) {
            if (i % 2 == 0) {
                row = list.add(theme.horizontalList()).expandX().widget();
            }
            row.add(new WLovenoteCard(modules.get(i))).expandX();
        }
    }
}

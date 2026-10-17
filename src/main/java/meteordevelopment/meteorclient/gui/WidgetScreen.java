package meteordevelopment.meteorclient.gui;

import meteordevelopment.meteorclient.gui.widgets.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class WidgetScreen extends Screen {
    public final GuiTheme theme;
    public WContainer root;

    public WidgetScreen(GuiTheme theme, String title) {
        super(Component.literal(title));
        this.theme = theme;
    }

    protected abstract void initWidgets();

    protected void init() {
        super.init();
        root = new WContainer();
        initWidgets();
    }

    public <T extends WWidget> Cell<T> add(T widget) { return root.add(widget); }

    public void reload() {
        root = new WContainer();
        initWidgets();
    }

    public boolean isPauseScreen() { return false; }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
    }
}

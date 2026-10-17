package meteordevelopment.meteorclient.gui.tabs;

import meteordevelopment.meteorclient.gui.GuiTheme;
import net.minecraft.client.gui.screens.Screen;

public abstract class Tab {
    public final String name;

    public Tab(String name) { this.name = name; }

    public abstract TabScreen createScreen(GuiTheme theme);
    public abstract boolean isScreen(Screen screen);
}

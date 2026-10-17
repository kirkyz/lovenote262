package meteordevelopment.meteorclient.gui.tabs.builtin;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.ModulesScreen;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import net.minecraft.client.gui.screens.Screen;

public class ModulesTab extends Tab {
    public ModulesTab() { super("Modules"); }
    public TabScreen createScreen(GuiTheme theme) { return new ModulesScreen(theme); }
    public boolean isScreen(Screen screen) { return screen instanceof ModulesScreen; }
}

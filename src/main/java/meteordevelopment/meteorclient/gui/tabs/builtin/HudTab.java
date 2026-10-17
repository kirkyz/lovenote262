package meteordevelopment.meteorclient.gui.tabs.builtin;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen;
import net.minecraft.client.gui.screens.Screen;

public class HudTab extends Tab {
    public HudTab() { super("HUD"); }
    public TabScreen createScreen(GuiTheme theme) { return new HudEditorScreen(theme); }
    public boolean isScreen(Screen screen) { return screen instanceof HudEditorScreen; }
}

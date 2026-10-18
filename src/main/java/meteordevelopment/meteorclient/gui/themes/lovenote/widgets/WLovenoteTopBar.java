package meteordevelopment.meteorclient.gui.themes.lovenote.widgets;

import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteWidget;
import meteordevelopment.meteorclient.gui.widgets.WTopBar;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class WLovenoteTopBar extends WTopBar implements LovenoteWidget {
    @Override
    protected Color getButtonColor(boolean pressed, boolean hovered) {
        if (pressed) return lovenote().accentTop();
        return new SettingColor(255, 255, 255, hovered ? 22 : 8);
    }

    @Override
    protected Color getNameColor() {
        return lovenote().textColor();
    }
}

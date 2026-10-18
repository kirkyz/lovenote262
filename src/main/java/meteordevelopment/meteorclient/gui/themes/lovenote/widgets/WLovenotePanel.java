package meteordevelopment.meteorclient.gui.themes.lovenote.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteGuiTheme;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;

public class WLovenotePanel extends WVerticalList implements LovenoteWidget {
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        LovenoteGuiTheme theme = lovenote();
        double r = theme.windowRadius();

        shadow(renderer, x, y, width, height, r, theme.shadowColor());
        panel(renderer, x, y, width, height, theme.panelTop(), theme.panelBottom(), theme.hairline());
        edgeGradient(renderer, x + r, y, width - r * 2, theme.accentAt(0), theme.accentAt(0.6));
    }
}

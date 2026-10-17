package meteordevelopment.meteorclient.gui.themes.lovenote.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;

public class WLovenotePanel extends WVerticalList implements LovenoteWidget {
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double r = lovenote().radius() * 2;
        roundedQuad(renderer, x, y, width, height, r, lovenote().panelColor());
    }
}

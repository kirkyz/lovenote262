package meteordevelopment.meteorclient.gui.themes.lovenote.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteGuiTheme;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WLovenoteWindow extends WWindow implements LovenoteWidget {
    public WLovenoteWindow(WWidget icon, String title) {
        super(icon, title);
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new WLovenoteHeader(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (expanded || animProgress > 0) {
            LovenoteGuiTheme theme = lovenote();
            double r = theme.windowRadius();

            shadow(renderer, x, y, width, height, r, theme.shadowColor());
            panel(renderer, x, y, width, height, theme.panelTop(), theme.panelBottom(), theme.hairline());
        }
    }

    private class WLovenoteHeader extends WHeader {
        public WLovenoteHeader(WWidget icon) {
            super(icon);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            LovenoteGuiTheme theme = lovenote();
            double r = theme.windowRadius();

            roundedGradient(renderer, x, y, width, height, r, theme.accentTop(), theme.accentBottom());
            edgeGradient(renderer, x + r, y, width - r * 2, theme.accentAt(0.75), theme.accentAt(0.25));
        }
    }
}

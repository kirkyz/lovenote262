package meteordevelopment.meteorclient.gui.themes.lovenote.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteGuiTheme;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;

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
            double r = lovenote().radius();
            roundedQuad(renderer, x, y, width, height, r, lovenote().panelColor());
        }
    }

    private class WLovenoteHeader extends WHeader {
        public WLovenoteHeader(WWidget icon) {
            super(icon);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            LovenoteGuiTheme theme = lovenote();
            double r = theme.radius();

            gradientRoundedTop(renderer, x, y, width, height, r, theme.accentTop(), theme.accentBottom());
        }

        private void gradientRoundedTop(GuiRenderer renderer, double x, double y, double w, double h, double r, meteordevelopment.meteorclient.utils.render.color.Color top, meteordevelopment.meteorclient.utils.render.color.Color bottom) {
            gradientQuad(renderer, x, y, w, h, top, bottom);
            roundedQuadTop(renderer, x, y, w, h, r, top);
        }
    }
}

package meteordevelopment.meteorclient.gui.themes.lovenote.widgets.pressable;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteGuiTheme;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import net.minecraft.util.Mth;

public class WLovenoteCheckbox extends WCheckbox implements LovenoteWidget {
    private double animProgress;

    public WLovenoteCheckbox(boolean checked) {
        super(checked);
        animProgress = checked ? 1 : 0;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        LovenoteGuiTheme theme = lovenote();

        animProgress += (checked ? 1 : -1) * delta * 14;
        animProgress = Mth.clamp(animProgress, 0, 1);

        double r = theme.radius();
        roundedQuad(renderer, x, y, width, height, r, theme.checkboxBackground(pressed, mouseOver));

        if (animProgress > 0) {
            double pad = theme.scale(2);
            double cs = (width - pad * 2) * animProgress;
            double cx = x + (width - cs) / 2;
            double cy = y + (height - cs) / 2;

            roundedQuad(renderer, cx, cy, cs, cs, r * animProgress, theme.accentTop());
        }
    }
}

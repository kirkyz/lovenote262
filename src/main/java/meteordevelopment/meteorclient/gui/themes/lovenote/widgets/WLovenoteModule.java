package meteordevelopment.meteorclient.gui.themes.lovenote.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteGuiTheme;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteWidget;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.util.Mth;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;

public class WLovenoteModule extends WPressable implements LovenoteWidget {
    private final Module module;
    private final String title;

    private double titleWidth;
    private double animBg;
    private double animAccent;

    public WLovenoteModule(Module module, String title) {
        this.module = module;
        this.title = title;
        this.tooltip = module.description;

        animBg = module.isActive() ? 1 : 0;
        animAccent = module.isActive() ? 1 : 0;
    }

    @Override
    public double pad() {
        return theme.scale(4);
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();

        if (titleWidth == 0) titleWidth = theme.textWidth(title);

        width = pad + titleWidth + pad;
        height = pad + theme.textHeight() + pad;
    }

    @Override
    protected void onPressed(int button) {
        if (button == MOUSE_BUTTON_LEFT) module.toggle();
        else if (button == MOUSE_BUTTON_RIGHT) mc.gui.setScreen(theme.moduleScreen(module));
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        LovenoteGuiTheme theme = lovenote();
        double pad = pad();

        animBg += delta * 4 * ((module.isActive() || mouseOver) ? 1 : -1);
        animBg = Mth.clamp(animBg, 0, 1);

        animAccent += delta * 6 * (module.isActive() ? 1 : -1);
        animAccent = Mth.clamp(animAccent, 0, 1);

        double r = theme.radius();
        double inset = theme.scale(1);

        if (animBg > 0) {
            double w = (width - inset * 2) * animBg;
            roundedQuad(renderer, x + inset, y + inset, w, height - inset * 2, r, theme.moduleRowBackground(module.isActive(), mouseOver));
        }

        if (animAccent > 0) {
            double bh = (height - inset * 2) * animAccent;
            double bw = theme.scale(2);
            roundedQuad(renderer, x + inset, y + (height - bh) / 2, bw, bh, theme.scale(1), theme.accentTop());
        }

        double tx = this.x + pad;
        double w = width - pad * 2;

        if (theme.moduleAlignment.get() == AlignmentX.Center) tx += w / 2 - titleWidth / 2;
        else if (theme.moduleAlignment.get() == AlignmentX.Right) tx += w - titleWidth;

        renderer.text(title, tx, y + pad, module.isActive() ? theme.accentText() : theme.textColor(), false);
    }
}

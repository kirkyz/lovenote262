package meteordevelopment.meteorclient.gui.themes.lovenote.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteGuiTheme;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.util.Mth;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;

public class WLovenoteCard extends WPressable implements LovenoteWidget {
    private final Module module;
    private double anim;

    public WLovenoteCard(Module module) {
        this.module = module;
        this.tooltip = module.description;
        anim = module.isActive() ? 1 : 0;
    }

    @Override
    public double pad() {
        return theme.scale(5);
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();
        width = pad * 2 + theme.scale(120);
        height = pad * 2 + theme.textHeight() * 2 + theme.scale(2);
    }

    @Override
    protected void onPressed(int button) {
        if (button == MOUSE_BUTTON_LEFT) module.toggle();
        else if (button == MOUSE_BUTTON_RIGHT) mc.gui.setScreen(theme.moduleScreen(module));
    }

    private String fit(String text, double maxWidth) {
        if (theme.textWidth(text) <= maxWidth) return text;
        String s = text;
        while (s.length() > 1 && theme.textWidth(s + "...") > maxWidth) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        LovenoteGuiTheme theme = lovenote();
        double pad = pad();
        double r = theme.radius();

        anim += delta * 6 * (module.isActive() ? 1 : -1);
        anim = Mth.clamp(anim, 0, 1);

        roundedQuad(renderer, x, y, width, height, r, theme.cardBackground(mouseOver, anim));

        double th = theme.textHeight();
        double toggleW = theme.scale(20);

        renderer.text(module.title, x + pad, y + pad, theme.textColor(), false);
        renderer.text(fit(module.description, width - pad * 2), x + pad, y + pad + th + theme.scale(2), theme.textSecondaryColor(), false);

        double tw = theme.scale(18);
        double thh = theme.scale(10);
        double tx = x + width - pad - tw;
        double ty = y + pad + (th - thh) / 2;

        roundedQuad(renderer, tx, ty, tw, thh, thh / 2, theme.toggleTrack(anim));
        double kr = thh - theme.scale(2);
        double kx = tx + theme.scale(1) + (tw - kr - theme.scale(2)) * anim;
        roundedQuad(renderer, kx, ty + theme.scale(1), kr, kr, kr / 2, theme.toggleKnob());
    }
}

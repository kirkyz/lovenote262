package meteordevelopment.meteorclient.gui.themes.lovenote.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteGuiTheme;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteStyle;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class WLovenoteCard extends WPressable implements LovenoteWidget {
    private final Module module;
    private final Runnable onToggle;
    private double anim;

    public WLovenoteCard(Module module, Runnable onToggle) {
        this.module = module;
        this.onToggle = onToggle;
        this.tooltip = module.description;
        this.anim = module.isActive() ? 1 : 0;
    }

    @Override
    protected void onCalculateSize() {
        double pad = theme.scale(6);
        width = pad * 2 + theme.scale(140);
        height = pad * 2 + theme.textHeight() * 2 + theme.scale(3);
    }

    @Override
    protected void onPressed(int button) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) {
            module.toggle();
            if (onToggle != null) onToggle.run();
        } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
            LovenoteStyle.open(theme.moduleScreen(module));
        }
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
        double pad = theme.scale(6);
        double r = theme.cardRadius();

        anim += delta * 6 * (module.isActive() ? 1 : -1);
        anim = Math.max(0, Math.min(1, anim));

        boolean active = module.isActive();

        if (mouseOver) shadow(renderer, x, y, width, height, r, theme.shadowColor());
        if (active && anim > 0.05) glow(renderer, x, y, width, height, r, theme.accentAt(0.1));

        Color top = active ? theme.cardTopActive(mouseOver) : theme.cardTop(mouseOver);
        card(renderer, x, y, width, height, top, theme.cardBottom(), theme.hairline());

        if (anim > 0.01) {
            double bh = (height - theme.scale(16)) * anim;
            roundedGradient(renderer, x + theme.scale(3), y + (height - bh) / 2, theme.scale(2), bh, theme.scale(1),
                theme.accentAt(0), theme.accentAt(0.3));
        }

        double th = theme.textHeight();
        renderer.text(module.title, x + pad, y + pad,
            active ? theme.accentText() : theme.textColor(), false);
        renderer.text(fit(module.description, width - pad * 2), x + pad, y + pad + th + theme.scale(3),
            theme.textSecondaryColor(), false);

        double tw = theme.scale(22), thh = theme.scale(12);
        double tx = x + width - pad - tw, ty = y + pad + (th - thh) / 2;
        toggle(renderer, tx, ty, tw, thh, anim);
    }
}

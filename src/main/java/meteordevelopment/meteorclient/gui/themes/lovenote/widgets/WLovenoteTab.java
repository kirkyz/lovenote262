package meteordevelopment.meteorclient.gui.themes.lovenote.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteGuiTheme;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.utils.render.color.Color;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class WLovenoteTab extends WPressable implements LovenoteWidget {
    private final String label;
    private boolean selected;
    private double anim;

    public WLovenoteTab(String label, boolean selected, Runnable onClick) {
        this.label = label;
        this.selected = selected;
        this.anim = selected ? 1 : 0;
        this.action = onClick;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void onCalculateSize() {
        double pad = theme.scale(5);
        width = pad * 2 + Math.max(theme.scale(72), theme.textWidth(label));
        height = pad * 2 + theme.textHeight();
    }

    @Override
    protected void onPressed(int button) {
        if (button == GLFW_MOUSE_BUTTON_LEFT && action != null) action.run();
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        LovenoteGuiTheme theme = lovenote();

        anim += delta * 8 * (selected ? 1 : -1);
        anim = Math.max(0, Math.min(1, anim));

        double pad = theme.scale(5);
        double r = theme.pillRadius();

        Color bg = mouseOver ? theme.cardTop(true) : theme.cardTop(false);
        roundedQuad(renderer, x, y, width, height, r, LovenoteStyleMix(bg, theme.accentAt(0.15), anim));
        if (anim > 0.01) outline(renderer, x, y, width, height, r, theme.hairline());

        if (anim > 0.01) {
            double bh = (height - theme.scale(8)) * anim;
            roundedGradient(renderer, x + theme.scale(3), y + (height - bh) / 2, theme.scale(2), bh, theme.scale(1),
                theme.accentAt(0), theme.accentAt(0.3));
        }

        renderer.text(label, x + pad + theme.scale(5), y + pad,
            selected ? theme.textColor() : theme.textSecondaryColor(), false);
    }

    private static Color LovenoteStyleMix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
            (int) (a.r + (b.r - a.r) * t),
            (int) (a.g + (b.g - a.g) * t),
            (int) (a.b + (b.b - a.b) * t),
            (int) (a.a + (b.a - a.a) * t));
    }
}

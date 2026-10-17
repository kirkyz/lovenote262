package meteordevelopment.meteorclient.gui.themes.lovenote;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.utils.render.color.Color;

public interface LovenoteWidget extends MeteorWidget {
    default LovenoteGuiTheme lovenote() {
        return (LovenoteGuiTheme) getTheme();
    }

    default void renderBackground(GuiRenderer renderer, WWidget widget, Color color) {
        roundedQuad(renderer, widget.x, widget.y, widget.width, widget.height, lovenote().radius(), color);
    }

    default void gradientQuad(GuiRenderer renderer, double x, double y, double w, double h, Color top, Color bottom) {
        LovenoteStyle.gradientQuad(renderer, x, y, w, h, top, bottom);
    }

    default void roundedQuad(GuiRenderer renderer, double x, double y, double w, double h, double r, Color color) {
        LovenoteStyle.roundedQuad(renderer, x, y, w, h, r, color);
    }

    /** Rounded on the top two corners only. */
    default void roundedQuadTop(GuiRenderer renderer, double x, double y, double w, double h, double r, Color color) {
        LovenoteStyle.roundedQuadTop(renderer, x, y, w, h, r, color);
    }

    // ------------------------------------------------------------ new helpers

    default void roundedGradient(GuiRenderer renderer, double x, double y, double w, double h,
                                 double r, Color top, Color bottom) {
        LovenoteStyle.roundedGradient(renderer, x, y, w, h, r, top, bottom);
    }

    default void outline(GuiRenderer renderer, double x, double y, double w, double h, double r, Color color) {
        LovenoteStyle.outline(renderer, x, y, w, h, r, LovenoteStyle.HAIRLINE, color);
    }

    default void innerHighlight(GuiRenderer renderer, double x, double y, double w, double r, Color color) {
        LovenoteStyle.innerHighlight(renderer, x, y, w, r, color);
    }

    default void edgeGradient(GuiRenderer renderer, double x, double y, double w, Color left, Color right) {
        LovenoteStyle.edgeGradient(renderer, x, y, w, LovenoteStyle.HAIRLINE, left, right);
    }

    default void glow(GuiRenderer renderer, double x, double y, double w, double h, double r, Color color) {
        LovenoteStyle.glow(renderer, x, y, w, h, r, color, 6);
    }

    default void shadow(GuiRenderer renderer, double x, double y, double w, double h, double r, Color color) {
        LovenoteStyle.shadow(renderer, x, y, w, h, r, color);
    }

    /** Gradient fill + hairline + inner highlight. */
    default void card(GuiRenderer renderer, double x, double y, double w, double h, Color fillTop, Color fillBottom, Color edge) {
        LovenoteStyle.card(renderer, x, y, w, h, lovenote().radius(), fillTop, fillBottom, edge);
    }

    default void micro(GuiRenderer renderer, String text, double x, double y, Color color) {
        LovenoteStyle.micro(renderer, lovenote(), text, x, y, color);
    }

    default void tracked(GuiRenderer renderer, String text, double x, double y, Color color, double spacing) {
        LovenoteStyle.tracked(renderer, lovenote(), text, x, y, color, spacing, false);
    }

    default void toggle(GuiRenderer renderer, double x, double y, double w, double h, double anim) {
        LovenoteStyle.toggle(renderer, x, y, w, h, anim,
            new Color(255, 255, 255, 26), lovenote().accentTop(), lovenote().toggleKnob());
    }

    default void divider(GuiRenderer renderer, double x, double y, double w, Color color) {
        LovenoteStyle.divider(renderer, x, y, w, color);
    }
}

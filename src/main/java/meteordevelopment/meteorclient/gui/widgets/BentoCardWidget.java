package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.systems.modules.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class BentoCardWidget extends AbstractWidget {
    private static final int BG = 0xCC101015;
    private static final int BG_HOVER = 0xE6181820;
    private static final int BORDER = 0x33FFFFFF;
    private static final int TITLE = 0xFFFFFFFF;

    private final Category category;

    public BentoCardWidget(int x, int y, Category category) {
        super(x, y, 200, 100, Component.literal(category.name));
        this.category = category;
    }

    public Category category() {
        return category;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        graphics.fill(x, y, x + w, y + h, isHovered() ? BG_HOVER : BG);
        graphics.outline(x, y, w, h, BORDER);
        graphics.text(Minecraft.getInstance().font, getMessage(), x + 6, y + 6, TITLE, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}

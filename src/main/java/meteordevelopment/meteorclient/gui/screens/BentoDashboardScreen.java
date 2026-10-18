package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.widgets.BentoCardWidget;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class BentoDashboardScreen extends Screen {
    public BentoDashboardScreen() {
        super(Component.literal("Bento Dashboard"));
    }

    @Override
    protected void init() {
        int y = 10;
        for (Category category : Modules.loopCategories()) {
            addRenderableWidget(new BentoCardWidget(10, y, category));
            y += 110;
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}

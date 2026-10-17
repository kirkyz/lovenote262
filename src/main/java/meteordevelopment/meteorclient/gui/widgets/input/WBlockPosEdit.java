package meteordevelopment.meteorclient.gui.widgets.input;

import meteordevelopment.meteorclient.gui.widgets.WWidget;
import net.minecraft.core.BlockPos;

public class WBlockPosEdit extends WWidget {
    public BlockPos value;
    public Runnable action;

    public WBlockPosEdit(BlockPos value, Runnable action) { this.value = value; this.action = action; }
}

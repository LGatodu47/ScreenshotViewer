package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

final class ConfirmDeletionScreen extends net.minecraft.client.gui.screen.ConfirmScreen {
    public ConfirmDeletionScreen(BooleanConsumer callback, Text title, Text message) {
        super(callback, title, message);
    }

    @Override
    public void renderBackground(DrawContext context) {
        context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
    }
}

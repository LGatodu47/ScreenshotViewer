package io.github.lgatodu47.screenshot_viewer.screens.manage_screenshots;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

final class ConfirmDeletionScreen extends ConfirmScreen {
    public ConfirmDeletionScreen(BooleanConsumer callback, Component title, Component message) {
        super(callback, title, message);
    }

    @Override
    public void renderBackground(GuiGraphics context) {
        context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
    }
}

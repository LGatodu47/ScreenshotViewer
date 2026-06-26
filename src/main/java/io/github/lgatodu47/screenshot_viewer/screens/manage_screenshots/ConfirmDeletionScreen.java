package io.github.lgatodu47.screenshot_viewer.screens.manage_screenshots;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

final class ConfirmDeletionScreen extends ConfirmScreen {
    public ConfirmDeletionScreen(BooleanConsumer callback, Component title, Component message) {
        super(callback, title, message);
    }

    @Override
    public void render(@NonNull GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
    }
}

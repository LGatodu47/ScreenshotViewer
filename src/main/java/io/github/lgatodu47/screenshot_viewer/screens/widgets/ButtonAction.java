package io.github.lgatodu47.screenshot_viewer.screens.widgets;

import net.minecraft.client.gui.GuiButton;

@FunctionalInterface
public interface ButtonAction {
    void onPress(GuiButton button);
}

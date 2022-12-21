package io.github.lgatodu47.screenshot_viewer.screens.widgets;

import net.minecraft.client.gui.GuiButton;

@FunctionalInterface
public interface ButtonTooltip {
    void renderTooltip(GuiButton button, int x, int y);
}

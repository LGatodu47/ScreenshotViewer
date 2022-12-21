package io.github.lgatodu47.screenshot_viewer.screens.widgets;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

import javax.annotation.Nullable;

public class ScreenshotViewerButton extends GuiButton implements ButtonAction, ButtonTooltip {
    private final ButtonAction action;
    @Nullable
    private final ButtonTooltip tooltip;

    public ScreenshotViewerButton(int x, int y, int width, int height, String text, ButtonAction action) {
        this(x, y, width, height, text, action, null);
    }

    public ScreenshotViewerButton(int x, int y, int width, int height, String text, ButtonAction action, @Nullable ButtonTooltip tooltip) {
        super(-1, x, y, width, height, text);
        this.action = action;
        this.tooltip = tooltip;
    }

    @Override
    public void onPress(GuiButton button) {
        this.action.onPress(button);
    }

    @Override
    public void renderTooltip(GuiButton button, int x, int y) {
        if(tooltip == null) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 1);
        tooltip.renderTooltip(button, x, y);
        GlStateManager.popMatrix();
    }
}

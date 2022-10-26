package io.github.lgatodu47.screenshot_viewer.screen;

import io.github.lgatodu47.catconfigmc.screen.ModConfigScreen;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerRenderedOptions;
import net.minecraft.client.gui.screen.Screen;

public class ScreenshotViewerConfigScreen extends ModConfigScreen {
    public ScreenshotViewerConfigScreen(Screen parent) {
        super(ScreenshotViewer.translatable("screen", "config"), parent, ScreenshotViewer.getInstance().getConfig(), ScreenshotViewerRenderedOptions::options);
    }
}

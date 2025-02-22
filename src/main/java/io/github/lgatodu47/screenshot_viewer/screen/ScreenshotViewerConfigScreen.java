package io.github.lgatodu47.screenshot_viewer.screen;

import io.github.lgatodu47.catconfigmc.screen.ModConfigScreen;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerRenderedOptions;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;

public class ScreenshotViewerConfigScreen extends ModConfigScreen {
    // used by WidgetPositionOption, temporary solution
    private static ScreenshotViewerConfigScreen currentInstance;

    public ScreenshotViewerConfigScreen(Screen parent) {
        super(ScreenshotViewerTexts.translatable("screen", "config"), parent, ScreenshotViewer.getInstance().getConfig(), ScreenshotViewerRenderedOptions.access());
        this.listeners = ScreenshotViewer.getInstance().getThumbnailManager();
        currentInstance = this;
    }

    public static ScreenshotViewerConfigScreen getCurrentInstance() {
        return currentInstance;
    }

    @Override
    public void close() {
        super.close();
        currentInstance = null;
    }
}

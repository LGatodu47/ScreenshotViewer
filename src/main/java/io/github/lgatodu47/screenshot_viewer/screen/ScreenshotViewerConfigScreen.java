package io.github.lgatodu47.screenshot_viewer.screen;

import io.github.lgatodu47.catconfigmc.screen.ModConfigScreen;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerRenderedOptions;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;

public class ScreenshotViewerConfigScreen extends ModConfigScreen {
    private static final Identifier BACKGROUND_TEXTURE = new Identifier("minecraft", "textures/block/cyan_terracotta.png");

    public ScreenshotViewerConfigScreen(Screen parent) {
        super(ScreenshotViewer.translatable("screen", "config"), parent, ScreenshotViewer.getInstance().getConfig(), ScreenshotViewerRenderedOptions.access());
        withBackgroundTexture(BACKGROUND_TEXTURE);
    }
}

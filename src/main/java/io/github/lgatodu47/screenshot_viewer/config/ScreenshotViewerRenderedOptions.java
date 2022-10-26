package io.github.lgatodu47.screenshot_viewer.config;

import io.github.lgatodu47.catconfigmc.RenderedConfigOption;
import io.github.lgatodu47.catconfigmc.RenderedConfigOptionBuilder;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;

import java.util.List;

public class ScreenshotViewerRenderedOptions {
    private static final RenderedConfigOptionBuilder BUILDER = new RenderedConfigOptionBuilder();

    static {
        BUILDER.ofBoolean(ScreenshotViewerOptions.SHOW_BUTTON_IN_GAME_PAUSE_MENU).setCommonTranslationKey(ScreenshotViewer.translation("config", "show_button_in_game_pause_menu")).build();
        BUILDER.ofBoolean(ScreenshotViewerOptions.SHOW_BUTTON_ON_TITLE_SCREEN).setCommonTranslationKey(ScreenshotViewer.translation("config", "show_button_on_title_screen")).build();
        BUILDER.ofBoolean(ScreenshotViewerOptions.ENABLE_SCREENSHOT_ENLARGEMENT_ANIMATION).setCommonTranslationKey(ScreenshotViewer.translation("config", "enable_screenshot_enlargement_animation")).build();
        BUILDER.ofInt(ScreenshotViewerOptions.INITIAL_SCREENSHOT_AMOUNT_PER_ROW).setCommonTranslationKey(ScreenshotViewer.translation("config", "initial_screenshot_amount_per_row")).build();
        BUILDER.ofInt(ScreenshotViewerOptions.SCREEN_SCROLL_SPEED).setCommonTranslationKey(ScreenshotViewer.translation("config", "screen_scroll_speed")).build();
    }

    public static List<RenderedConfigOption<?>> options() {
        return BUILDER.optionsToRender();
    }
}

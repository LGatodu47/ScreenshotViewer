package io.github.lgatodu47.screenshot_viewer.config;

import io.github.lgatodu47.catconfigmc.RenderedConfigOptionAccess;
import io.github.lgatodu47.catconfigmc.RenderedConfigOptionBuilder;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;

public class ScreenshotViewerRenderedOptions {
    private static final RenderedConfigOptionBuilder BUILDER = new RenderedConfigOptionBuilder();

    static {
        BUILDER.ofBoolean(ScreenshotViewerOptions.SHOW_BUTTON_IN_GAME_PAUSE_MENU).setCommonTranslationKey(ScreenshotViewer.translation("config", "show_button_in_game_pause_menu")).build();
        BUILDER.ofBoolean(ScreenshotViewerOptions.SHOW_BUTTON_ON_TITLE_SCREEN).setCommonTranslationKey(ScreenshotViewer.translation("config", "show_button_on_title_screen")).build();
        BUILDER.ofBoolean(ScreenshotViewerOptions.REDIRECT_SCREENSHOT_CHAT_LINKS).setCommonTranslationKey(ScreenshotViewer.translation("config", "redirect_screenshot_chat_links")).build();
        BUILDER.ofInt(ScreenshotViewerOptions.PAUSE_MENU_BUTTON_OFFSET).setCommonTranslationKey(ScreenshotViewer.translation("config", "pause_menu_button_offset")).build();
        BUILDER.withCategoryName("ingame", ScreenshotViewer.translatable("config_category", "ingame"));

        BUILDER.option(ScreenshotViewerOptions.SCREENSHOTS_FOLDER).setWidgetFactory(FileOption::createScreenshotsDirectoryWidget).setCommonTranslationKey(ScreenshotViewer.translation("config", "screenshots_folder")).build();
        BUILDER.ofEnum(ScreenshotViewerOptions.DEFAULT_LIST_ORDER, ScreenshotListOrder.class).setCommonTranslationKey(ScreenshotViewer.translation("config", "default_list_order")).build();
        BUILDER.ofBoolean(ScreenshotViewerOptions.PROMPT_WHEN_DELETING_SCREENSHOT).setCommonTranslationKey(ScreenshotViewer.translation("config", "prompt_when_deleting_screenshot")).build();
        BUILDER.ofBoolean(ScreenshotViewerOptions.ENABLE_SCREENSHOT_ENLARGEMENT_ANIMATION).setCommonTranslationKey(ScreenshotViewer.translation("config", "enable_screenshot_enlargement_animation")).build();
        BUILDER.ofInt(ScreenshotViewerOptions.INITIAL_SCREENSHOT_AMOUNT_PER_ROW).setCommonTranslationKey(ScreenshotViewer.translation("config", "initial_screenshot_amount_per_row")).build();
        BUILDER.ofInt(ScreenshotViewerOptions.SCREEN_SCROLL_SPEED).setCommonTranslationKey(ScreenshotViewer.translation("config", "screen_scroll_speed")).build();
        BUILDER.ofInt(ScreenshotViewerOptions.SCREENSHOT_ELEMENT_BACKGROUND_OPACITY).setCommonTranslationKey(ScreenshotViewer.translation("config", "screenshot_element_background_opacity")).build();
        BUILDER.ofEnum(ScreenshotViewerOptions.SCREENSHOT_ELEMENT_TEXT_VISIBILITY, VisibilityState.class).setCommonTranslationKey(ScreenshotViewer.translation("config", "screenshot_element_text_visibility")).build();
        BUILDER.option(ScreenshotViewerOptions.SCREENSHOT_ELEMENT_TEXT_COLOR).setWidgetFactory(config -> ColorOption.createWidget(config, ScreenshotViewerOptions.SCREENSHOT_ELEMENT_TEXT_COLOR)).setCommonTranslationKey(ScreenshotViewer.translation("config", "screenshot_element_text_color")).build();
        BUILDER.ofBoolean(ScreenshotViewerOptions.RENDER_SCREENSHOT_ELEMENT_FONT_SHADOW).setCommonTranslationKey(ScreenshotViewer.translation("config", "render_screenshot_element_font_shadow")).build();
        BUILDER.withCategoryName("ingui", ScreenshotViewer.translatable("config_category", "ingui"));
    }

    public static RenderedConfigOptionAccess access() {
        return BUILDER;
    }
}

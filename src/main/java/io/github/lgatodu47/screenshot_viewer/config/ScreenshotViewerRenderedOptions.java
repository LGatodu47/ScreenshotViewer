package io.github.lgatodu47.screenshot_viewer.config;

import io.github.lgatodu47.catconfigmc.RenderedConfigOptionAccess;
import io.github.lgatodu47.catconfigmc.RenderedConfigOptionBuilder;
import io.github.lgatodu47.screenshot_viewer.screen.ConfigureButtonPlacementScreen;
import io.github.lgatodu47.screenshot_viewer.screen.IconButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;

import static io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions.*;
import static io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerTexts.translatable;
import static io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerTexts.translation;

public class ScreenshotViewerRenderedOptions {
    private static final RenderedConfigOptionBuilder BUILDER = new RenderedConfigOptionBuilder();

    static {
        BUILDER.ofBoolean(SHOW_BUTTON_IN_GAME_PAUSE_MENU).setCommonTranslationKey(translation("config", "show_button_in_game_pause_menu")).build();
        BUILDER.option(PAUSE_MENU_BUTTON_POSITION).setWidgetFactory(config ->
                WidgetPositionOption.createWidget(config,
                        PAUSE_MENU_BUTTON_POSITION,
                        () -> new GameMenuScreen(true),
                        ConfigureButtonPlacementScreen.WidgetRemover.ofPredicate(IconButtonWidget.class::isInstance),
                        () -> MinecraftClient.getInstance().player != null && config.getOrFallback(SHOW_BUTTON_IN_GAME_PAUSE_MENU, true))
        ).setCommonTranslationKey(translation("config", "pause_menu_button_position")).build();
        BUILDER.ofBoolean(SHOW_BUTTON_ON_TITLE_SCREEN).setCommonTranslationKey(translation("config", "show_button_on_title_screen")).build();
        BUILDER.option(TITLE_SCREEN_BUTTON_POSITION).setWidgetFactory(config ->
                WidgetPositionOption.createWidget(config,
                        TITLE_SCREEN_BUTTON_POSITION,
                        () -> new TitleScreen(false),
                        ConfigureButtonPlacementScreen.WidgetRemover.ofPredicate(IconButtonWidget.class::isInstance),
                        () -> config.getOrFallback(SHOW_BUTTON_ON_TITLE_SCREEN, true))
        ).setCommonTranslationKey(translation("config", "title_screen_button_position")).build();
        BUILDER.ofBoolean(REDIRECT_SCREENSHOT_CHAT_LINKS).setCommonTranslationKey(translation("config", "redirect_screenshot_chat_links")).build();
        BUILDER.withCategoryName("ingame", translatable("config_category", "ingame"));

        BUILDER.option(SCREENSHOTS_FOLDER).setWidgetFactory(config -> FileOption.createDirectoryWidget(config, SCREENSHOTS_FOLDER)).setCommonTranslationKey(translation("config", "screenshots_folder")).build();
        BUILDER.ofEnum(DEFAULT_LIST_ORDER, ScreenshotListOrder.class).setCommonTranslationKey(translation("config", "default_list_order")).build();
        BUILDER.ofBoolean(PROMPT_WHEN_DELETING_SCREENSHOT).setCommonTranslationKey(translation("config", "prompt_when_deleting_screenshot")).build();
        BUILDER.ofBoolean(ENABLE_SCREENSHOT_ENLARGEMENT_ANIMATION).setCommonTranslationKey(translation("config", "enable_screenshot_enlargement_animation")).build();
        BUILDER.ofBoolean(RENDER_WIDE_PROPERTIES_BUTTON).setCommonTranslationKey(translation("config", "render_wide_properties_button")).build();
        BUILDER.ofBoolean(DISPLAY_HINT_TOOLTIP).setCommonTranslationKey(translation("config", "display_hint_tooltip")).build();
        BUILDER.ofBoolean(INVERT_ZOOM_DIRECTION).setCommonTranslationKey(translation("config", "invert_zoom_direction")).build();
        BUILDER.ofInt(INITIAL_SCREENSHOT_AMOUNT_PER_ROW).setCommonTranslationKey(translation("config", "initial_screenshot_amount_per_row")).build();
        BUILDER.ofInt(SCREEN_SCROLL_SPEED).setCommonTranslationKey(translation("config", "screen_scroll_speed")).build();
        BUILDER.ofInt(SCREENSHOT_ELEMENT_BACKGROUND_OPACITY).setCommonTranslationKey(translation("config", "screenshot_element_background_opacity")).build();
        BUILDER.ofEnum(SCREENSHOT_ELEMENT_TEXT_VISIBILITY, VisibilityState.class).setCommonTranslationKey(translation("config", "screenshot_element_text_visibility")).build();
        BUILDER.option(SCREENSHOT_ELEMENT_TEXT_COLOR).setWidgetFactory(config -> ColorOption.createWidget(config, SCREENSHOT_ELEMENT_TEXT_COLOR)).setCommonTranslationKey(translation("config", "screenshot_element_text_color")).build();
        BUILDER.ofBoolean(RENDER_SCREENSHOT_ELEMENT_FONT_SHADOW).setCommonTranslationKey(translation("config", "render_screenshot_element_font_shadow")).build();
        BUILDER.withCategoryName("ingui", translatable("config_category", "ingui"));

        BUILDER.option(THUMBNAIL_FOLDER).setWidgetFactory(config -> FileOption.createDirectoryWidget(config, THUMBNAIL_FOLDER)).setCommonTranslationKey(translation("config", "thumbnail_folder")).build();
        BUILDER.ofEnum(COMPRESSION_RATIO, CompressionRatio.class).setCommonTranslationKey(translation("config", "compression_ratio")).build();
        BUILDER.withCategoryName("screenshot_thumbnails", translatable("config_category", "screenshot_thumbnails"));
    }

    public static RenderedConfigOptionAccess access() {
        return BUILDER;
    }
}

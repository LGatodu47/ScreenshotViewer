package io.github.lgatodu47.screenshot_viewer.config;

import io.github.lgatodu47.catconfig.ConfigOption;
import io.github.lgatodu47.catconfig.ConfigOptionAccess;
import io.github.lgatodu47.catconfig.ConfigOptionBuilder;
import io.github.lgatodu47.catconfigmc.MinecraftConfigSides;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import io.github.lgatodu47.screenshot_viewer.config.WidgetPositionOption.WidgetPosition;
import net.minecraft.text.TextColor;

import java.io.File;

public class ScreenshotViewerOptions {
    private static final ConfigOptionBuilder BUILDER = ConfigOptionBuilder.create();
    public static final ConfigOptionAccess OPTIONS = BUILDER;

    static {
        BUILDER.onSides(MinecraftConfigSides.CLIENT);
        BUILDER.inCategory("ingame");
    }
    public static final ConfigOption<Boolean> SHOW_BUTTON_IN_GAME_PAUSE_MENU = BUILDER.createBool("show_button_in_game_pause_menu", true);
    public static final ConfigOption<WidgetPosition> PAUSE_MENU_BUTTON_POSITION = BUILDER.put(new WidgetPositionOption("pause_menu_button_position", null, "ingame"));
    public static final ConfigOption<Boolean> SHOW_BUTTON_ON_TITLE_SCREEN = BUILDER.createBool("show_button_on_title_screen", true);
    public static final ConfigOption<WidgetPosition> TITLE_SCREEN_BUTTON_POSITION = BUILDER.put(new WidgetPositionOption("title_screen_button_position", null, "ingame"));
    public static final ConfigOption<Boolean> REDIRECT_SCREENSHOT_CHAT_LINKS = BUILDER.createBool("redirect_screenshot_chat_links", false);

    static {
        BUILDER.inCategory("ingui");
    }
    public static final ConfigOption<File> SCREENSHOTS_FOLDER = BUILDER.put(new FileOption("screenshots_folder", ScreenshotViewerUtils::getVanillaScreenshotsFolder, "ingui"));
    public static final ConfigOption<ScreenshotListOrder> DEFAULT_LIST_ORDER = BUILDER.createEnum("default_list_order", ScreenshotListOrder.class, ScreenshotListOrder.ASCENDING);
    public static final ConfigOption<Boolean> PROMPT_WHEN_DELETING_SCREENSHOT = BUILDER.createBool("prompt_when_deleting_screenshot", true);
    public static final ConfigOption<Boolean> ENABLE_SCREENSHOT_ENLARGEMENT_ANIMATION = BUILDER.createBool("enable_screenshot_enlargement_animation", true);
    public static final ConfigOption<Boolean> DISPLAY_HINT_TOOLTIP = BUILDER.createBool("display_hint_tooltip", true);
    public static final ConfigOption<Boolean> RENDER_WIDE_PROPERTIES_BUTTON = BUILDER.createBool("render_wide_properties_button", true);
    public static final ConfigOption<Boolean> INVERT_ZOOM_DIRECTION = BUILDER.createBool("invert_zoom_direction", false);
    public static final ConfigOption<Integer> INITIAL_SCREENSHOT_AMOUNT_PER_ROW = BUILDER.createInt("initial_screenshot_amount_per_row", 4, 2, 8);
    public static final ConfigOption<Integer> SCREEN_SCROLL_SPEED = BUILDER.createInt("screen_scroll_speed", 10, 1, 50);
    public static final ConfigOption<Integer> SCREENSHOT_ELEMENT_BACKGROUND_OPACITY = BUILDER.createInt("screenshot_element_background_opacity", 100, 0, 100);
    public static final ConfigOption<VisibilityState> SCREENSHOT_ELEMENT_TEXT_VISIBILITY = BUILDER.createEnum("screenshot_element_text_visibility", VisibilityState.class, VisibilityState.VISIBLE);
    public static final ConfigOption<TextColor> SCREENSHOT_ELEMENT_TEXT_COLOR = BUILDER.put(new ColorOption("screenshot_element_text_color", TextColor.fromRgb(0xFFFFFF), "ingui"));
    public static final ConfigOption<Boolean> RENDER_SCREENSHOT_ELEMENT_FONT_SHADOW = BUILDER.createBool("render_screenshot_element_font_shadow", true);

    static {
        BUILDER.inCategory("screenshot_thumbnails");
    }
    public static final ConfigOption<File> THUMBNAIL_FOLDER = BUILDER.put(new FileOption("thumbnail_folder", ScreenshotViewerUtils::getDefaultThumbnailFolder, "screenshot_thumbnails"));
    public static final ConfigOption<CompressionRatio> COMPRESSION_RATIO = BUILDER.createEnum("compression_ratio", CompressionRatio.class, CompressionRatio.NONE);
}

package io.github.lgatodu47.screenshot_viewer.config;

import io.github.lgatodu47.catconfig.ConfigOption;
import io.github.lgatodu47.catconfig.ConfigOptionAccess;
import io.github.lgatodu47.catconfig.ConfigOptionBuilder;
import io.github.lgatodu47.catconfigmc.MinecraftConfigSides;

public class ScreenshotViewerOptions {
    private static final ConfigOptionBuilder BUILDER = ConfigOptionBuilder.create();
    public static final ConfigOptionAccess OPTIONS = BUILDER;

    static {
        BUILDER.onSides(MinecraftConfigSides.CLIENT);
    }
    public static final ConfigOption<Boolean> SHOW_BUTTON_IN_GAME_PAUSE_MENU = BUILDER.createBool("show_button_in_game_pause_menu", true);
    public static final ConfigOption<Boolean> SHOW_BUTTON_ON_TITLE_SCREEN = BUILDER.createBool("show_button_on_title_screen", true);
    public static final ConfigOption<Boolean> ENABLE_SCREENSHOT_ENLARGEMENT_ANIMATION = BUILDER.createBool("enable_screenshot_enlargement_animation", true);
    public static final ConfigOption<Integer> INITIAL_SCREENSHOT_AMOUNT_PER_ROW = BUILDER.createInt("initial_screenshot_amount_per_row", 4, 2, 8);
    public static final ConfigOption<Integer> SCREEN_SCROLL_SPEED = BUILDER.createInt("screen_scroll_speed", 10, 1, 50);
}

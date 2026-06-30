package io.github.lgatodu47.screenshot_viewer.config;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.OptionalInt;

public final class ScreenshotViewerConfig {
    // in game
    public final BooleanValue showButtonInGamePauseMenu;
    public final IntValue pauseMenuButtonVerticalOffset;
    public final BooleanValue showButtonOnTitleScreen;
    public final IntValue titleScreenButtonHorizontalOffset;
    public final BooleanValue redirectScreenshotChatLinks;

    // in gui
    public final ConfigValue<String> screenshotsFolder;
    public final ConfigValue<ScreenshotListOrder> defaultListOrder;
    public final BooleanValue promptWhenDeletingScreenshot;
    public final BooleanValue enableScreenshotEnlargementAnimation;
    public final BooleanValue displayHintTooltip;
    public final BooleanValue renderWidePropertiesButton;
    public final BooleanValue invertZoomDirection;
    public final IntValue initialScreenshotAmountPerRow;
    public final IntValue screenScrollSpeed;
    public final ConfigValue<String> screenshotElementBackgroundColor;
    public final EnumValue<VisibilityState> screenshotElementTextVisibility;
    public final ConfigValue<String> screenshotElementTextColor;
    public final BooleanValue renderScreenshotElementFontShadow;

    // screenshot thumbnails
    public final ConfigValue<String> thumbnailFolder;
    public final EnumValue<CompressionRatio> compressionRatio;

    private ScreenshotViewerConfig(ModConfigSpec.Builder builder) {
        builder.translation("config_category.screenshot_viewer.ingame").push("ingame");
        showButtonInGamePauseMenu = builder.comment("Shows a button to access screenshots directly in the pause menu.")
                .translation("config.screenshot_viewer.show_button_in_game_pause_menu")
                .define("showButtonInGamePauseMenu", true);
        pauseMenuButtonVerticalOffset = builder.comment("The vertical offset in pixels of the screenshot manager button if shown in the pause menu.")
                .translation("config.screenshot_viewer.pause_menu_button_vertical_offset")
                .defineInRange("pauseMenuButtonVerticalOffset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        showButtonOnTitleScreen = builder.comment("Shows a button to access screenshots directly on the title screen.")
                .translation("config.screenshot_viewer.show_button_on_title_screen")
                .define("showButtonOnTitleScreen", true);
        titleScreenButtonHorizontalOffset = builder.comment("The horizontal offset in pixels of the screenshot manager button if shown in the title screen.")
                .translation("config.screenshot_viewer.title_screen_button_horizontal_offset")
                .defineInRange("titleScreenButtonHorizontalOffset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        redirectScreenshotChatLinks = builder.comment("If enabled, clicking on the link printed in the chat when taking a screenshot will open it directly in the screenshot manager.")
                .translation("config.screenshot_viewer.redirect_screenshot_chat_links")
                .define("redirectScreenshotChatLinks", false);
        builder.pop();

        builder.translation("config_category.screenshot_viewer.ingui").push("ingui");
        screenshotsFolder = builder.comment("The path to the folder containing the screenshots images that will be displayed in the screen.")
                .translation("config.screenshot_viewer.screenshots_folder")
                .define("screenshotsFolder", ScreenshotViewerUtils::getVanillaScreenshotsFolderPath, this::validDirectory);
        defaultListOrder = builder.comment("The ordering of the screenshots when the screen is opened.")
                .translation("config.screenshot_viewer.default_list_order")
                .defineEnum("defaultListOrder", ScreenshotListOrder.ASCENDING);
        promptWhenDeletingScreenshot = builder.comment("Warns you with a confirmation screen when trying to delete a screenshot.")
                .translation("config.screenshot_viewer.prompt_when_deleting_screenshot")
                .define("promptWhenDeletingScreenshot", true);
        enableScreenshotEnlargementAnimation = builder.comment("Whether a growing animation should play when a screenshot on the screen is clicked to be enlarged.")
                .translation("config.screenshot_viewer.enable_screenshot_enlargement_animation")
                .define("enableScreenshotEnlargementAnimation", true);
        displayHintTooltip = builder.comment("Controls whether a tooltip giving useful info should appear when hovering a screenshot for a prolonged period.")
                .translation("config.screenshot_viewer.display_hint_tooltip")
                .define("displayHintTooltip", true);
        renderWidePropertiesButton = builder.comment("Whether the properties buttons should be rendered wide or square.")
                .translation("config.screenshot_viewer.render_wide_properties_button")
                .define("renderWidePropertiesButton", true);
        invertZoomDirection = builder.comment("If enabled, it inverts the scroll direction during zoom in the Screenshot Viewer menu")
                .translation("config.screenshot_viewer.invert_zoom_direction")
                .define("invertZoomDirection", false);
        initialScreenshotAmountPerRow = builder.comment("Number of screenshots to be displayed per row when the screen is opened (from 2 to 8).")
                .translation("config.screenshot_viewer.initial_screenshot_amount_per_row")
                .defineInRange("initialScreenshotAmountPerRow", 4, 2, 8);
        screenScrollSpeed = builder.comment("Scroll speed of the screen (from 1 to 50).")
                .translation("config.screenshot_viewer.screen_scroll_speed")
                .defineInRange("screenScrollSpeed", 10, 1, 50);
        screenshotElementBackgroundColor = builder.comment("Color of the background that appears when the screenshot elements are hovered.")
                .translation("config.screenshot_viewer.screenshot_element_background_color")
                .define("screenshotElementBackgroundColor", "#FFFFFFFF", this::validColorString);
        screenshotElementTextVisibility = builder.comment("The visibility state of the screenshots names.")
                .translation("config.screenshot_viewer.screenshot_element_text_visibility")
                .defineEnum("screenshotElementTextVisibility", VisibilityState.VISIBLE);
        screenshotElementTextColor = builder.comment("Color of the texts describing the screenshots elements.")
                .translation("config.screenshot_viewer.screenshot_element_text_color")
                .define("screenshotElementTextColor", "#FFFFFFFF", this::validColorString);
        renderScreenshotElementFontShadow = builder.comment("If a font shadow should be rendered under the screenshot elements text.")
                .translation("config.screenshot_viewer.render_screenshot_element_font_shadow")
                .define("renderScreenshotElementFontShadow", true);
        builder.pop();

        builder.translation("config_category.screenshot_viewer.screenshot_thumbnails").push("screenshot_thumbnails");
        thumbnailFolder = builder.comment("The path to the folder containing the screenshots thumbnails that will be displayed in the screen.")
                .translation("config.screenshot_viewer.thumbnail_folder")
                .define("screenshotsFolder", ScreenshotViewerUtils::getDefaultThumbnailFolderPath, this::validDirectory);
        compressionRatio = builder.comment("If set to none, will not generate any thumbnails. Controls the downscale ratio between a screenshot image and its thumbnail.")
                .translation("config.screenshot_viewer.compression_ratio")
                .defineEnum("compression_ratio", CompressionRatio.NONE);
        builder.pop();
    }

    private boolean validColorString(Object obj) {
        if(!(obj instanceof String s)) {
            return false;
        }
        if(!s.startsWith("#")) {
            return false;
        }
        s = s.substring(1);
        try {
            //noinspection ResultOfMethodCallIgnored
            Integer.parseUnsignedInt(s.substring(0, Math.min(s.length(), 8)), 16);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static OptionalInt colorFromHex(String hexString) {
        if(hexString.startsWith("#")) {
            hexString = hexString.substring(1);
        }
        try {
            int value = Integer.parseUnsignedInt(hexString.substring(0, Math.min(hexString.length(), 8)), 16);
            return OptionalInt.of(value);
        } catch (Throwable ignored) {
        }
        return OptionalInt.empty();
    }

    private boolean validDirectory(Object obj) {
        if(!(obj instanceof String fileName)) {
            return false;
        }
        File file = new File(fileName);
        return file.exists() && file.isDirectory() && file.canRead();
    }

    public static ScreenshotViewerConfig registerConfig(ModContainer container) {
        Pair<ScreenshotViewerConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ScreenshotViewerConfig::new);
        container.registerConfig(ModConfig.Type.CLIENT, specPair.getRight(), ScreenshotViewer.MODID.concat("-client.toml"));
        return specPair.getLeft();
    }
}

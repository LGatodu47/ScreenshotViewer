package io.github.lgatodu47.screenshot_viewer.config;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import net.minecraft.network.chat.TextColor;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;

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
    public final IntValue screenshotElementBackgroundOpacity;
    public final EnumValue<VisibilityState> screenshotElementTextVisibility;
    public final ConfigValue<String> screenshotElementTextColor;
    public final BooleanValue renderScreenshotElementFontShadow;

    // screenshot thumbnails
    public final ConfigValue<String> thumbnailFolder;
    public final EnumValue<CompressionRatio> compressionRatio;

    private ScreenshotViewerConfig(ModConfigSpec.Builder builder) {
        builder.comment("Screenshot Viewer Hooks");
        showButtonInGamePauseMenu = builder.comment("Shows a button to access screenshots directly in the pause menu.")
                .translation("config.screenshot_viewer.show_button_in_game_pause_menu")
                .define("ingame.showButtonInGamePauseMenu", true);
        pauseMenuButtonVerticalOffset = builder.comment("The vertical offset in pixels of the screenshot manager button if shown in the pause menu.")
                .translation("config.screenshot_viewer.pause_menu_button_vertical_offset")
                .defineInRange("ingame.pauseMenuButtonVerticalOffset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        showButtonOnTitleScreen = builder.comment("Shows a button to access screenshots directly on the title screen.")
                .translation("config.screenshot_viewer.show_button_on_title_screen")
                .define("ingame.showButtonOnTitleScreen", true);
        titleScreenButtonHorizontalOffset = builder.comment("The horizontal offset in pixels of the screenshot manager button if shown in the title screen.")
                .translation("config.screenshot_viewer.title_screen_button_horizontal_offset")
                .defineInRange("ingame.titleScreenButtonHorizontalOffset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        redirectScreenshotChatLinks = builder.comment("If enabled, clicking on the link printed in the chat when taking a screenshot will open it directly in the screenshot manager.")
                .translation("config.screenshot_viewer.redirect_screenshot_chat_links")
                .define("ingame.redirectScreenshotChatLinks", false);

        builder.comment("Screenshot Manager Customization");
        screenshotsFolder = builder.comment("The path to the folder containing the screenshots images that will be displayed in the screen.")
                .translation("config.screenshot_viewer.screenshots_folder")
                .define("ingui.screenshotsFolder", () -> ScreenshotViewerUtils.getVanillaScreenshotsFolder().getAbsolutePath(), this::validDirectory);
        defaultListOrder = builder.comment("The ordering of the screenshots when the screen is opened.")
                .translation("config.screenshot_viewer.default_list_order")
                .defineEnum("ingui.defaultListOrder", ScreenshotListOrder.ASCENDING);
        promptWhenDeletingScreenshot = builder.comment("Warns you with a confirmation screen when trying to delete a screenshot.")
                .translation("config.screenshot_viewer.prompt_when_deleting_screenshot")
                .define("ingui.promptWhenDeletingScreenshot", true);
        enableScreenshotEnlargementAnimation = builder.comment("Whether a growing animation should play when a screenshot on the screen is clicked to be enlarged.")
                .translation("config.screenshot_viewer.enable_screenshot_enlargement_animation")
                .define("ingui.enableScreenshotEnlargementAnimation", true);
        displayHintTooltip = builder.comment("Controls whether a tooltip giving useful info should appear when hovering a screenshot for a prolonged period.")
                .translation("config.screenshot_viewer.display_hint_tooltip")
                .define("ingui.displayHintTooltip", true);
        renderWidePropertiesButton = builder.comment("Whether the properties buttons should be rendered wide or square.")
                .translation("config.screenshot_viewer.render_wide_properties_button")
                .define("ingui.renderWidePropertiesButton", true);
        invertZoomDirection = builder.comment("If enabled, it inverts the scroll direction during zoom in the Screenshot Viewer menu")
                .translation("config.screenshot_viewer.invert_zoom_direction")
                .define("ingui.invertZoomDirection", false);
        initialScreenshotAmountPerRow = builder.comment("Number of screenshots to be displayed per row when the screen is opened (from 2 to 8).")
                .translation("config.screenshot_viewer.initial_screenshot_amount_per_row")
                .defineInRange("ingui.initialScreenshotAmountPerRow", 4, 2, 8);
        screenScrollSpeed = builder.comment("Scroll speed of the screen (from 1 to 50).")
                .translation("config.screenshot_viewer.screen_scroll_speed")
                .defineInRange("ingui.screenScrollSpeed", 10, 1, 50);
        screenshotElementBackgroundOpacity = builder.comment("Opacity of the background that appears when the screenshot elements are hovered (from 0 to 100).")
                .translation("config.screenshot_viewer.screenshot_element_background_opacity")
                .defineInRange("ingui.screenshotElementBackgroundOpacity", 100, 0, 100);
        screenshotElementTextVisibility = builder.comment("The visibility state of the screenshots names.")
                .translation("config.screenshot_viewer.screenshot_element_text_visibility")
                .defineEnum("ingui.screenshotElementTextVisibility", VisibilityState.VISIBLE);
        screenshotElementTextColor = builder.comment("Color of the texts describing the screenshots elements.")
                .translation("config.screenshot_viewer.screenshot_element_text_color")
                .define("ingui.screenshotElementTextColor", "#FFFFFF", this::validColorString);
        renderScreenshotElementFontShadow = builder.comment("If a font shadow should be rendered under the screenshot elements text.")
                .translation("config.screenshot_viewer.render_screenshot_element_font_shadow")
                .define("ingui.renderScreenshotElementFontShadow", true);

        builder.comment("Screenshot Thumbnails");
        thumbnailFolder = builder.comment("The path to the folder containing the screenshots thumbnails that will be displayed in the screen.")
                .translation("config.screenshot_viewer.thumbnail_folder")
                .define("screenshot_thumbnails.screenshotsFolder", () -> ScreenshotViewerUtils.getDefaultThumbnailFolder().getAbsolutePath(), this::validDirectory);
        compressionRatio = builder.comment("If set to none, will not generate any thumbnails. Controls the downscale ratio between a screenshot image and its thumbnail.")
                .translation("config.screenshot_viewer.compression_ratio")
                .defineEnum("screenshot_thumbnails.compression_ratio", CompressionRatio.NONE);
    }

    private boolean validColorString(Object obj) {
        if(!(obj instanceof String s)) {
            return false;
        }
        return s.startsWith("#") && (s.substring(1).isEmpty() || TextColor.parseColor(s).isSuccess());
    }

    private boolean validDirectory(Object obj) {
        if(!(obj instanceof String fileName)) {
            return false;
        }
        File file = new File(fileName);
        return file.exists() && file.isAbsolute() && file.isDirectory() && file.canRead();
    }

    public static ScreenshotViewerConfig registerConfig(ModContainer container) {
        Pair<ScreenshotViewerConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ScreenshotViewerConfig::new);
        container.registerConfig(ModConfig.Type.CLIENT, specPair.getRight(), ScreenshotViewer.MODID.concat("-client.toml"));
        return specPair.getLeft();
    }
}

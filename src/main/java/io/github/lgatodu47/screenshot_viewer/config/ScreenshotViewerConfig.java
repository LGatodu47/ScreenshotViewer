package io.github.lgatodu47.screenshot_viewer.config;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;

public final class ScreenshotViewerConfig {
    public final BooleanValue showButtonInGamePauseMenu;
    public final BooleanValue showButtonOnTitleScreen;
    public final BooleanValue enableScreenshotEnlargementAnimation;
    public final BooleanValue promptWhenDeletingScreenshot;
    public final IntValue initialScreenshotAmountPerRow;
    public final IntValue screenScrollSpeed;
    public final IntValue screenshotElementBackgroundOpacity;
    public final BooleanValue renderScreenshotElementFontShadow;
    public final ConfigValue<String> screenshotElementTextColor;
    public final ConfigValue<ScreenshotListOrder> defaultListOrder;
    public final IntValue pauseMenuButtonOffset;
    public final ConfigValue<String> screenshotsFolder;
    public final BooleanValue redirectScreenshotChatLinks;
    public final EnumValue<VisibilityState> screenshotElementTextVisibility;
    public final BooleanValue invertZoomDirection;
    public final BooleanValue displayHintTooltip;
    public final BooleanValue renderWidePropertiesButton;

    private ScreenshotViewerConfig(ForgeConfigSpec.Builder builder) {
        showButtonInGamePauseMenu = builder.comment("Shows a button to access screenshots directly in the pause menu.")
                .translation("config.screenshot_viewer.show_button_in_game_pause_menu")
                .define("showButtonInGamePauseMenu", true);
        showButtonOnTitleScreen = builder.comment("Shows a button to access screenshots directly on the title screen.")
                .translation("config.screenshot_viewer.show_button_on_title_screen")
                .define("showButtonOnTitleScreen", true);
        enableScreenshotEnlargementAnimation = builder.comment("Whether a growing animation should play when a screenshot on the screen is clicked to be enlarged.")
                .translation("config.screenshot_viewer.enable_screenshot_enlargement_animation")
                .define("enableScreenshotEnlargementAnimation", true);
        promptWhenDeletingScreenshot = builder.comment("Warns you with a confirmation screen when trying to delete a screenshot.")
                .translation("config.screenshot_viewer.prompt_when_deleting_screenshot")
                .define("promptWhenDeletingScreenshot", true);
        initialScreenshotAmountPerRow = builder.comment("Number of screenshots to be displayed per row when the screen is opened (from 2 to 8).")
                .translation("config.screenshot_viewer.initial_screenshot_amount_per_row")
                .defineInRange("initialScreenshotAmountPerRow", 4, 2, 8);
        screenScrollSpeed = builder.comment("Scroll speed of the screen (from 1 to 50).")
                .translation("config.screenshot_viewer.screen_scroll_speed")
                .defineInRange("screenScrollSpeed", 10, 1, 50);
        screenshotElementBackgroundOpacity = builder.comment("Opacity of the background that appears when the screenshot elements are hovered (from 0 to 100).")
                .translation("config.screenshot_viewer.screenshot_element_background_opacity")
                .defineInRange("screenshotElementBackgroundOpacity", 100, 0, 100);
        renderScreenshotElementFontShadow = builder.comment("If a font shadow should be rendered under the screenshot elements text.")
                .translation("config.screenshot_viewer.render_screenshot_element_font_shadow")
                .define("renderScreenshotElementFontShadow", true);
        screenshotElementTextColor = builder.comment("Color of the texts describing the screenshots elements.")
                .translation("config.screenshot_viewer.screenshot_element_text_color")
                .define("screenshotElementTextColor", "#FFFFFF", this::validColorString);
        defaultListOrder = builder.comment("The ordering of the screenshots when the screen is opened.")
                .translation("config.screenshot_viewer.default_list_order")
                .defineEnum("defaultListOrder", ScreenshotListOrder.ASCENDING);
        pauseMenuButtonOffset = builder.comment("The offset in pixels of the screenshot manager button if shown in the pause menu (from 0 to how much you want).")
                .translation("config.screenshot_viewer.pause_menu_button_offset")
                .defineInRange("pauseMenuButtonOffset", 4, 0, Integer.MAX_VALUE);
        screenshotsFolder = builder.comment("The path to the folder containing the screenshots images that will be displayed in the screen.")
                .translation("config.screenshot_viewer.screenshots_folder")
                .define("screenshotsFolder", () -> ScreenshotViewerUtils.getVanillaScreenshotsFolder().getAbsolutePath(), this::validDirectory);
        redirectScreenshotChatLinks = builder.comment("If enabled, clicking on the link printed in the chat when taking a screenshot will open it directly in the screenshot manager.")
                .translation("config.screenshot_viewer.redirect_screenshot_chat_links")
                .define("redirectScreenshotChatLinks", false);
        screenshotElementTextVisibility = builder.comment("The visibility state of the screenshots names.")
                .translation("config.screenshot_viewer.screenshot_element_text_visibility")
                .defineEnum("screenshotElementTextVisibility", VisibilityState.VISIBLE);
        invertZoomDirection = builder.comment("If enabled, it inverts the scroll direction during zoom in the Screenshot Viewer menu")
                .translation("config.screenshot_viewer.invert_zoom_direction")
                .define("invertZoomDirection", false);
        displayHintTooltip = builder.comment("Controls whether a tooltip giving useful info should appear when hovering a screenshot for a prolonged period.")
                .translation("config.screenshot_viewer.display_hint_tooltip")
                .define("displayHintTooltip", true);
        renderWidePropertiesButton = builder.comment("Whether the properties buttons should be rendered wide or square.")
                .translation("config.screenshot_viewer.render_wide_properties_button")
                .define("renderWidePropertiesButton", true);
    }

    private boolean validColorString(Object obj) {
        if(!(obj instanceof String s)) {
            return false;
        }
        return s.startsWith("#") && (s.substring(1).isEmpty() || TextColor.parseColor(s) != null);
    }

    private boolean validDirectory(Object obj) {
        if(!(obj instanceof String fileName)) {
            return false;
        }
        File file = new File(fileName);
        return file.exists() && file.isAbsolute() && file.isDirectory() && file.canRead();
    }

    public static ScreenshotViewerConfig registerConfig(ModLoadingContext ctx) {
        Pair<ScreenshotViewerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ScreenshotViewerConfig::new);
        ctx.registerConfig(ModConfig.Type.CLIENT, specPair.getRight(), ScreenshotViewer.MODID.concat("-client.toml"));
        return specPair.getLeft();
    }
}

package io.github.lgatodu47.screenshot_viewer.config;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import net.minecraft.util.text.Color;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

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
    }

    private boolean validColorString(Object obj) {
        if(!(obj instanceof String)) {
            return false;
        }
        String s = (String) obj;
        return s.startsWith("#") && (s.substring(1).isEmpty() || Color.parseColor(s) != null);
    }

    public static ScreenshotViewerConfig registerConfig(ModLoadingContext ctx) {
        Pair<ScreenshotViewerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ScreenshotViewerConfig::new);
        ctx.registerConfig(ModConfig.Type.CLIENT, specPair.getRight(), ScreenshotViewer.MODID.concat("-client.toml"));
        return specPair.getLeft();
    }
}

package io.github.lgatodu47.screenshot_viewer.config;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class ScreenshotViewerConfig {
    static final String CATEGORY = "Config";

    private final Configuration configuration;
    public final BooleanSupplier useNewButtonTextures;
    public final BooleanSupplier showButtonInGamePauseMenu;
    public final BooleanSupplier showButtonOnTitleScreen;
    public final BooleanSupplier enableScreenshotEnlargementAnimation;
    public final BooleanSupplier promptWhenDeletingScreenshot;
    public final IntSupplier initialScreenshotAmountPerRow;
    public final IntSupplier screenScrollSpeed;
    public final IntSupplier screenshotElementBackgroundOpacity;
    public final BooleanSupplier renderScreenshotElementFontShadow;
    public final Supplier<String> screenshotElementTextColor;

    private ScreenshotViewerConfig(Configuration configuration) {
        this.configuration = configuration;
        List<String> orderList = new ArrayList<>();
        useNewButtonTextures = defineBool(orderList,
                "useNewButtonTextures",
                false,
                "config.screenshot_viewer.use_new_button_textures",
                "If set to true, the buttons in the mod's screens will be using the 1.15+ hovering textures.");
        showButtonInGamePauseMenu = defineBool(orderList,
                "showButtonInGamePauseMenu",
                true,
                "config.screenshot_viewer.show_button_in_game_pause_menu",
                "Shows a button to access screenshots directly in the pause menu.");
        showButtonOnTitleScreen = defineBool(orderList,
                "showButtonOnTitleScreen",
                true,
                "config.screenshot_viewer.show_button_on_title_screen",
                "Shows a button to access screenshots directly on the title screen.");
        enableScreenshotEnlargementAnimation = defineBool(orderList,
                "enableScreenshotEnlargementAnimation",
                true,
                "config.screenshot_viewer.enable_screenshot_enlargement_animation",
                "Whether a growing animation should play when a screenshot on the screen is clicked to be enlarged.");
        promptWhenDeletingScreenshot = defineBool(orderList,
                "promptWhenDeletingScreenshot",
                true,
                "config.screenshot_viewer.prompt_when_deleting_screenshot",
                "Warns you with a confirmation screen when trying to delete a screenshot.");
        initialScreenshotAmountPerRow = defineInt(orderList,
                "initialScreenshotAmountPerRow",
                4,
                2,
                8,
                "config.screenshot_viewer.initial_screenshot_amount_per_row",
                "Number of screenshots to be displayed per row when the screen is opened.");
        screenScrollSpeed = defineInt(orderList,
                "screenScrollSpeed",
                10,
                1,
                50,
                "config.screenshot_viewer.screen_scroll_speed",
                "Scroll speed of the screen.");
        screenshotElementBackgroundOpacity = defineInt(orderList,
                "screenshotElementBackgroundOpacity",
                100,
                0,
                100,
                "config.screenshot_viewer.screenshot_element_background_opacity",
                "Opacity of the background that appears when the screenshot elements are hovered.");
        renderScreenshotElementFontShadow = defineBool(orderList,
                "renderScreenshotElementFontShadow",
                true,
                "config.screenshot_viewer.render_screenshot_element_font_shadow",
                "If a font shadow should be rendered under the screenshot elements text.");

        orderList.add("screenshotElementTextColor");
        Property screenshotElementTextColorProperty = configuration.get(CATEGORY, "screenshotElementTextColor", "#FFFFFF");
        screenshotElementTextColorProperty.setLanguageKey("config.screenshot_viewer.screenshot_element_text_color");
        screenshotElementTextColorProperty.setValidationPattern(Pattern.compile("^#([a-fA-F0-9]{6}|[a-fA-F0-9]{3})$"));
        screenshotElementTextColorProperty.setComment("Color of the texts describing the screenshots elements. [default: #FFFFFF]");
        screenshotElementTextColor = screenshotElementTextColorProperty::getString;

        configuration.getCategory(CATEGORY).setPropertyOrder(orderList);
        save();
    }

    Configuration getConfiguration() {
        return configuration;
    }

    public void save() {
        this.configuration.save();
    }

    private BooleanSupplier defineBool(List<String> orderList, String property, boolean defaultValue, String translationKey, String comment) {
        orderList.add(property);
        Property prop = configuration.get(CATEGORY, property, defaultValue);
        prop.setLanguageKey(translationKey);
        prop.setComment(comment + " [default: " + defaultValue + "]");
        return prop::getBoolean;
    }

    private IntSupplier defineInt(List<String> orderList, String property, int defaultValue, int min, int max, String translationKey, String comment) {
        orderList.add(property);
        Property prop = configuration.get(CATEGORY, property, defaultValue);
        prop.setMinValue(min);
        prop.setMaxValue(max);
        prop.setLanguageKey(translationKey);
        prop.setComment(comment + " [range: " + min + " ~ " + max + ", default: " + defaultValue + "]");
        return prop::getInt;
    }

    public static ScreenshotViewerConfig registerConfig(File configDir) {
        Configuration configuration = new Configuration(new File(configDir, ScreenshotViewer.MODID.concat("-client.cfg")));
        return new ScreenshotViewerConfig(configuration);
    }
}

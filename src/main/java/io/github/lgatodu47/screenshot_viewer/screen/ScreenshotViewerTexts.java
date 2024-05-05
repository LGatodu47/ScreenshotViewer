package io.github.lgatodu47.screenshot_viewer.screen;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class ScreenshotViewerTexts {
    public static final Text MANAGE_SCREENSHOTS = translatable("screen", "manage_screenshots");
    public static final Text ZOOM_MODE = translatable("screen", "screenshot_manager.zoom");
    public static final Text FAST_DELETE_MODE = translatable("screen", "screenshot_manager.fast_delete");
    public static final Text NO_SCREENSHOTS = translatable("screen", "screenshot_manager.no_screenshots");

    public static final Text REFRESH = translatable("screen", "button.refresh");
    public static final Text CONFIG = translatable("screen", "button.config");
    public static final Text ORDER = translatable("screen", "button.order");
    public static final Text ASCENDING_ORDER = translatable("screen", "button.order.ascending");
    public static final Text DESCENDING_ORDER = translatable("screen", "button.order.descending");
    public static final Text OPEN_FOLDER = translatable("screen", "button.screenshot_folder");
    public static final Text FAST_DELETE = translatable("screen", "button.fast_delete");
    public static final Text DELETE = translatable("screen", "button.delete_screenshot");
    public static final Text OPEN_FILE = translatable("screen", "button.open_file");
    public static final Text RENAME_FILE = translatable("screen", "button.rename_file");
    public static final Text CLOSE_PROPERTIES = translatable("screen", "button.close_properties_menu");
    public static final Text COPY = translatable("screen", "button.copy_screenshot");

    public static final Text DELETE_WARNING_MESSAGE = translatable("screen", "delete_prompt.message");
    public static final Text DELETE_MULTIPLE_WARNING_MESSAGE = translatable("screen", "delete_n_screenshots.message");
    public static final Text RENAME_PROMPT = translatable("screen", "rename_screenshot");
    public static final Text SCREENSHOT_NAME_INPUT = translatable("screen", "field.screenshot_name");
    public static final Text REDIRECT_TO_SCREENSHOT_MANAGER = translatable("screen", "redirect_to_screenshot_manager");

    public static final Text TOAST_COPY_SUCCESS = translatable("toast", "copy_success");

    public static MutableText translatable(String prefix, String suffix, Object... args) {
        return Text.translatable(translation(prefix, suffix), args);
    }

    public static String translation(String prefix, String suffix) {
        return prefix + '.' + ScreenshotViewer.MODID + '.' + suffix;
    }
}

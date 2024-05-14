package io.github.lgatodu47.screenshot_viewer.screens;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ScreenshotViewerTexts {
    public static final Component MANAGE_SCREENSHOTS = translatable("screen", "manage_screenshots");
    public static final Component ZOOM_MODE = translatable("screen", "screenshot_manager.zoom");
    public static final Component FAST_DELETE_MODE = translatable("screen", "screenshot_manager.fast_delete");
    public static final Component NO_SCREENSHOTS = translatable("screen", "screenshot_manager.no_screenshots");
    public static final Component NO_CONFIG = translatable("screen", "no_config");

    public static final Component REFRESH = translatable("screen", "button.refresh");
    public static final Component CONFIG = translatable("screen", "button.config");
    public static final Component ORDER = translatable("screen", "button.order");
    public static final Component ASCENDING_ORDER = translatable("screen", "button.order.ascending");
    public static final Component DESCENDING_ORDER = translatable("screen", "button.order.descending");
    public static final Component OPEN_FOLDER = translatable("screen", "button.screenshot_folder");
    public static final Component FAST_DELETE = translatable("screen", "button.fast_delete");
    public static final Component DELETE = translatable("screen", "button.delete_screenshot");
    public static final Component OPEN_FILE = translatable("screen", "button.open_file");
    public static final Component RENAME_FILE = translatable("screen", "button.rename_file");
    public static final Component CLOSE_PROPERTIES = translatable("screen", "button.close_properties_menu");
    public static final Component COPY = translatable("screen", "button.copy_screenshot");

    public static final Component DELETE_WARNING_MESSAGE = translatable("screen", "delete_prompt.message");
    public static final Component DELETE_MULTIPLE_WARNING_MESSAGE = translatable("screen", "delete_n_screenshots.message");
    public static final Component RENAME_PROMPT = translatable("screen", "rename_screenshot");
    public static final Component SCREENSHOT_NAME_INPUT = translatable("screen", "field.screenshot_name");
    public static final Component REDIRECT_TO_SCREENSHOT_MANAGER = translatable("tooltip", "redirect_to_screenshot_manager");

    public static final Component TOAST_COPY_SUCCESS = translatable("toast", "copy_success");

    public static MutableComponent translatable(String prefix, String suffix, Object... args) {
        return Component.translatable(translation(prefix, suffix), args);
    }

    public static String translation(String prefix, String suffix) {
        return prefix + '.' + ScreenshotViewer.MODID + '.' + suffix;
    }
}

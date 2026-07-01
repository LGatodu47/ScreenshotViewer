package io.github.lgatodu47.screenshot_viewer.screen;

import net.minecraft.network.chat.ClickEvent;
import org.jspecify.annotations.NonNull;

import java.io.File;

public record ScreenshotClickEvent(File screenshotFile) implements ClickEvent {
    @Override
    public @NonNull Action action() {
        return Action.OPEN_URL;
    }
}

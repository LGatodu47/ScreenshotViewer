package io.github.lgatodu47.screenshot_viewer.screen;

import net.minecraft.text.ClickEvent;

import java.io.File;

public record ScreenshotClickEvent(File screenshotFile) implements ClickEvent {
    @Override
    public Action getAction() {
        return Action.OPEN_URL;
    }
}

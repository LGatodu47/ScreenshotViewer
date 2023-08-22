package io.github.lgatodu47.screenshot_viewer.screen;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import net.minecraft.text.ClickEvent;

import java.io.File;
import java.util.Objects;

public class ScreenshotClickEvent extends ClickEvent {
    private final File screenshotFile;

    public ScreenshotClickEvent(File screenshotFile) {
        super(Action.OPEN_URL, ScreenshotViewer.MODID + ":screenshot_click_event");
        this.screenshotFile = screenshotFile;
    }

    public File getScreenshotFile() {
        return screenshotFile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ScreenshotClickEvent that = (ScreenshotClickEvent) o;
        return Objects.equals(screenshotFile, that.screenshotFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), screenshotFile);
    }

    @Override
    public String toString() {
        return "ScreenshotClickEvent{screenshotFile=" + screenshotFile + '}';
    }
}

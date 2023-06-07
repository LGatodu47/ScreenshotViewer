package io.github.lgatodu47.screenshot_viewer.config;

public enum ScreenshotListOrder {
    ASCENDING,
    DESCENDING;

    public boolean isInverted() {
        return this == DESCENDING;
    }
}

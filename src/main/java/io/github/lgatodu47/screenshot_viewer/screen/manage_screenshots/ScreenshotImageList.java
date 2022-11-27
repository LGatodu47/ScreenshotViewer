package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

interface ScreenshotImageList {
    ScreenshotImageHolder getScreenshot(int index);

    int size();
}

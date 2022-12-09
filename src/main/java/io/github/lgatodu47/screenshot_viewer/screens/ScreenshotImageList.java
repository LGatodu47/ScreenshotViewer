package io.github.lgatodu47.screenshot_viewer.screens;

interface ScreenshotImageList {
    ScreenshotImageHolder getScreenshot(int index);

    int size();
}

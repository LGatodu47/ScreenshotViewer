package io.github.lgatodu47.screenshot_viewer.screens;

import java.io.File;
import java.util.Optional;

interface ScreenshotImageList {
    ScreenshotImageHolder getScreenshot(int index);

    Optional<ScreenshotImageHolder> findByFileName(File file);

    int size();
}

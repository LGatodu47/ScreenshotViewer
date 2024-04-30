package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import java.io.File;
import java.util.Optional;

public interface ScreenshotImageList {
    ScreenshotImageHolder getScreenshot(int index);

    Optional<ScreenshotImageHolder> findByFileName(File file);

    int size();
}

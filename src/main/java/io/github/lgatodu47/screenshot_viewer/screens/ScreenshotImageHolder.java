package io.github.lgatodu47.screenshot_viewer.screens;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;

interface ScreenshotImageHolder {
    int indexInList();

    int imageId();

    @Nullable
    BufferedImage image();
}

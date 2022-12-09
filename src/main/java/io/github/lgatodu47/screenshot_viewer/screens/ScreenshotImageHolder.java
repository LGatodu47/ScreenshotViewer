package io.github.lgatodu47.screenshot_viewer.screens;

import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.Nullable;

interface ScreenshotImageHolder {
    int indexInList();

    int imageId();

    @Nullable
    NativeImage image();
}

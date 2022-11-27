package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import net.minecraft.client.texture.NativeImage;
import org.jetbrains.annotations.Nullable;

interface ScreenshotImageHolder {
    int indexInList();

    int imageId();

    @Nullable
    NativeImage image();
}

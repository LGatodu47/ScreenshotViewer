package io.github.lgatodu47.screenshot_viewer.screens;

import net.minecraft.client.renderer.texture.NativeImage;

import javax.annotation.Nullable;

interface ScreenshotImageHolder {
    int indexInList();

    int imageId();

    @Nullable
    NativeImage image();
}

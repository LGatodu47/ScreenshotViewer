package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import net.minecraft.client.texture.NativeImage;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public interface ScreenshotImageHolder {
    File getScreenshotFile();

    void openFile();

    void copyScreenshot();

    void requestFileDeletion();

    void renameFile();

    int indexInList();

    int imageId();

    @Nullable
    NativeImage image();
}

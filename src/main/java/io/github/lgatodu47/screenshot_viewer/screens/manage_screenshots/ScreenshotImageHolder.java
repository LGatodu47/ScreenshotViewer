package io.github.lgatodu47.screenshot_viewer.screens.manage_screenshots;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.File;

interface ScreenshotImageHolder {
    File getScreenshotFile();

    void openFile();

    void copyScreenshot();

    void requestFileDeletion();

    void renameFile();

    int indexInList();

    @Nullable
    Identifier textureId();

    @Nullable
    NativeImage image();
}

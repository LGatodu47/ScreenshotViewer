package io.github.lgatodu47.screenshot_viewer.mixin;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotClickEvent;
import io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots.ManageScreenshotsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow
    protected static void defaultHandleClickEvent(ClickEvent event, Minecraft minecraft, @Nullable Screen activeScreen) {
    }

    @Inject(method = "defaultHandleGameClickEvent", at = @At("HEAD"), cancellable = true)
    private static void screenshot_viewer$inject_handleClickEvent(ClickEvent event, Minecraft minecraft, @Nullable Screen activeScreen, CallbackInfo ci) {
        if (event instanceof ScreenshotClickEvent(File screenshotFile)) {
            if(!minecraft.hasShiftDown() && ScreenshotViewer.getInstance().getConfig().getOrFallback(ScreenshotViewerOptions.REDIRECT_SCREENSHOT_CHAT_LINKS, false)) {
                minecraft.gui.setScreen(new ManageScreenshotsScreen(minecraft.gui.screen(), screenshotFile));
                ci.cancel();
                return;
            }
            defaultHandleClickEvent(new ClickEvent.OpenFile(screenshotFile), minecraft, activeScreen);
            ci.cancel();
        }
    }
}

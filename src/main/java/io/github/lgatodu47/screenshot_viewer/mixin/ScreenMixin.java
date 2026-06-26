package io.github.lgatodu47.screenshot_viewer.mixin;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.screens.manage_screenshots.ManageScreenshotsScreen;
import io.github.lgatodu47.screenshot_viewer.screens.ScreenshotClickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow
    protected static void defaultHandleClickEvent(ClickEvent clickEvent, Minecraft client, @org.jspecify.annotations.Nullable Screen screenAfterRun) {
    }

    @Inject(method = "defaultHandleGameClickEvent", at = @At("HEAD"), cancellable = true)
    private static void screenshot_viewer$inject_handleClickEvent(ClickEvent clickEvent, Minecraft client, @Nullable Screen screenAfterRun, CallbackInfo ci) {
        if (clickEvent instanceof ScreenshotClickEvent ce) {
            if(!client.hasShiftDown() && ScreenshotViewer.getInstance().getConfig().redirectScreenshotChatLinks.getAsBoolean()) {
                client.setScreen(new ManageScreenshotsScreen(client.screen, ce.screenshotFile()));
                ci.cancel();
                return;
            }
            defaultHandleClickEvent(new ClickEvent.OpenFile(ce.screenshotFile()), client, screenAfterRun);
            ci.cancel();
        }
    }
}

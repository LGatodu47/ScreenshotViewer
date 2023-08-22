package io.github.lgatodu47.screenshot_viewer.mixin;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions;
import io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots.ManageScreenshotsScreen;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotClickEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow @Nullable protected MinecraftClient client;

    @Shadow public abstract boolean handleTextClick(@Nullable Style style);

    @Inject(method = "handleTextClick", at = @At("HEAD"), cancellable = true)
    private void screenshot_viewer$inject_handleTextClick(Style style, CallbackInfoReturnable<Boolean> cir) {
        if(style == null || Screen.hasShiftDown()) {
            return;
        }
        if(style.getClickEvent() instanceof ScreenshotClickEvent event) {
            if(ScreenshotViewer.getInstance().getConfig().getOrFallback(ScreenshotViewerOptions.REDIRECT_SCREENSHOT_CHAT_LINKS, false)) {
                //noinspection DataFlowIssue
                this.client.setScreen(new ManageScreenshotsScreen((Screen) (Object) this, event.getScreenshotFile()));
                cir.setReturnValue(true);
                return;
            }
            cir.setReturnValue(this.handleTextClick(style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, event.getScreenshotFile().getAbsolutePath()))));
        }
    }
}

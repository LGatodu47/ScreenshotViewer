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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow @Nullable protected Minecraft minecraft;

    @Shadow public abstract boolean handleComponentClicked(@Nullable Style style);

    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void screenshot_viewer$inject_handleComponentClicked(Style style, CallbackInfoReturnable<Boolean> cir) {
        if(style == null || Screen.hasShiftDown()) {
            return;
        }
        if(style.getClickEvent() instanceof ScreenshotClickEvent event) {
            if(ScreenshotViewer.getInstance().getConfig().redirectScreenshotChatLinks.get()) {
                //noinspection DataFlowIssue
                this.minecraft.setScreen(new ManageScreenshotsScreen((Screen) (Object) this, event.getScreenshotFile()));
                cir.setReturnValue(true);
                return;
            }
            cir.setReturnValue(this.handleComponentClicked(style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, event.getScreenshotFile().getAbsolutePath()))));
        }
    }
}

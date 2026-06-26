package io.github.lgatodu47.screenshot_viewer.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions;
import io.github.lgatodu47.screenshot_viewer.screen.IconButtonWidget;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerTexts;
import io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots.ManageScreenshotsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class PauseScreenMixin {
    @Inject(method = "createPauseMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSingleplayerServer()Lnet/minecraft/client/server/IntegratedServer;", shift = At.Shift.BEFORE))
    private void screenshot_viewer$inject_createPauseMenu(CallbackInfo ci, @Local(name = "iconButtonRow") LinearLayout iconButtonRow) {
        if(ScreenshotViewer.getInstance().getConfig().getOrFallback(ScreenshotViewerOptions.SHOW_BUTTON_IN_GAME_PAUSE_MENU, true)
        && ScreenshotViewer.getInstance().getConfig().get(ScreenshotViewerOptions.PAUSE_MENU_BUTTON_POSITION).isEmpty()) {
            iconButtonRow.addChild(Util.make(new IconButtonWidget(0, 0, 20, 20, ScreenshotViewerTexts.MANAGE_SCREENSHOTS, ScreenshotViewer.SCREENSHOT_VIEWER_ICON, button -> {
                Minecraft.getInstance().gui.setScreen(new ManageScreenshotsScreen((Screen) (Object) this));
            }), btn -> btn.setTooltip(Tooltip.create(ScreenshotViewerTexts.MANAGE_SCREENSHOTS))));
        }
    }
}

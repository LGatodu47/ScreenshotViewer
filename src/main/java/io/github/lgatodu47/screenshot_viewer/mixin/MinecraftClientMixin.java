package io.github.lgatodu47.screenshot_viewer.mixin;

import io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots.ManageScreenshotsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow @Nullable public Screen currentScreen;

    @Shadow @Final private Window window;

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void screenshot_viewer$inject_getFramerateLimit(CallbackInfoReturnable<Integer> cir) {
        if(currentScreen != null && currentScreen instanceof ManageScreenshotsScreen) {
            cir.setReturnValue(window.getFramerateLimit());
        }
    }
}

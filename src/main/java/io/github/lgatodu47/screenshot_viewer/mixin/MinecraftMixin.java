package io.github.lgatodu47.screenshot_viewer.mixin;

import com.mojang.blaze3d.platform.Window;
import io.github.lgatodu47.screenshot_viewer.screens.manage_screenshots.ManageScreenshotsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow @Nullable public Screen screen;

    @Shadow @Final private Window window;

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void screenshot_viewer$inject_getFramerateLimit(CallbackInfoReturnable<Integer> cir) {
        if(screen != null && screen instanceof ManageScreenshotsScreen) {
            cir.setReturnValue(window.getFramerateLimit());
        }
    }
}

package io.github.lgatodu47.screenshot_viewer.mixin;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotClickEvent;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerTexts;
import net.fabricmc.loader.api.FabricLoader;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.util.function.Consumer;

@Mixin(Screenshot.class)
public class ScreenshotMixin {
    @Inject(method = "lambda$grab$3", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", ordinal = 0, shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void screenshot_viewer$inject_saveScreenshotInner$lambda(NativeImage nativeImage, File file, Consumer<Component> consumer, CallbackInfo ci, NativeImage var3, Component text) {
        if(!(text instanceof MutableComponent mutable) || text.getStyle().getClickEvent() == null) {
            if(FabricLoader.getInstance().isDevelopmentEnvironment()) {
                throw new RuntimeException("Minecraft codebase probably changed, code won't work");
            }
            return;
        }
        mutable.withStyle(style -> style
                .withClickEvent(new ScreenshotClickEvent(file))
                .withHoverEvent(new HoverEvent.ShowText(ScreenshotViewerUtils.ofSupplied(() ->
                        ScreenshotViewer.getInstance().getConfig().getOrFallback(ScreenshotViewerOptions.REDIRECT_SCREENSHOT_CHAT_LINKS, false)
                                ? ScreenshotViewerTexts.REDIRECT_TO_SCREENSHOT_MANAGER
                                : null))
                )
        );
    }
}

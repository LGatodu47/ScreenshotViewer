package io.github.lgatodu47.screenshot_viewer.mixin;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotClickEvent;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerTexts;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.util.function.Consumer;

@Mixin(ScreenshotRecorder.class)
public class ScreenshotRecorderMixin {
    /*@ModifyVariable(
            method = "saveScreenshot(Ljava/io/File;Ljava/lang/String;Lnet/minecraft/client/gl/Framebuffer;ILjava/util/function/Consumer;)V",
            at = @org.spongepowered.asm.mixin.injection.At("HEAD"),
            argsOnly = true,
            index = 4
    )
    private static Consumer<Text> screenshot_viewer$wrapMessageReceiver(Consumer<Text> messageReceiver, File gameDirectory, String fileName, Framebuffer framebuffer, int downscaleFactor) {
        return message -> {
            if (ScreenshotViewer.getInstance().getConfig().getOrFallback(ScreenshotViewerOptions.REDIRECT_SCREENSHOT_CHAT_LINKS, false) && message instanceof MutableText mutable) {
                mutable.styled(style ->
                        style.withClickEvent(new ScreenshotClickEvent())
                                .withHoverEvent(new HoverEvent.ShowText(ScreenshotViewerTexts.REDIRECT_TO_SCREENSHOT_MANAGER)));
            }
            messageReceiver.accept(message);
        };
    }*/

    @Inject(method = "method_22691", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", ordinal = 0, shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void screenshot_viewer$inject_saveScreenshotInner$lambda(NativeImage nativeImage, File file, Consumer<Text> consumer, CallbackInfo ci, NativeImage var3, Text text) {
        if(!(text instanceof MutableText mutable) || text.getStyle().getClickEvent() == null) {
            if(FabricLoader.getInstance().isDevelopmentEnvironment()) {
                throw new RuntimeException("Minecraft codebase probably changed, code won't work");
            }
            return;
        }
        mutable.styled(style -> style
                .withClickEvent(new ScreenshotClickEvent(file))
                .withHoverEvent(new HoverEvent.ShowText(ScreenshotViewerUtils.ofSupplied(() ->
                        ScreenshotViewer.getInstance().getConfig().getOrFallback(ScreenshotViewerOptions.REDIRECT_SCREENSHOT_CHAT_LINKS, false)
                                ? ScreenshotViewerTexts.REDIRECT_TO_SCREENSHOT_MANAGER
                                : null))
                )
        );
    }
}

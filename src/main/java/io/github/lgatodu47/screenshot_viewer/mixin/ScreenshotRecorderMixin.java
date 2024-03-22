package io.github.lgatodu47.screenshot_viewer.mixin;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotClickEvent;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.util.function.Consumer;

@Mixin(ScreenshotRecorder.class)
public class ScreenshotRecorderMixin {
    @Inject(method = "method_1661", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", ordinal = 0, shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void screenshot_viewer$inject_saveScreenshotInner$lambda(NativeImage nativeImage, File file, Consumer<Text> messageReceiver, CallbackInfo ci, Text text) {
        if(!(text instanceof MutableText mutable) || text.getStyle().getClickEvent() == null) {
            if(FabricLoader.getInstance().isDevelopmentEnvironment()) {
                throw new RuntimeException("Minecraft codebase probably changed, code won't work");
            }
            return;
        }
        mutable.styled(style -> style
                .withClickEvent(new ScreenshotClickEvent(new File(text.getStyle().getClickEvent().getValue())))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, ScreenshotViewer.translatable("tooltip", "redirect_to_screenshot_manager")) {
                    @Nullable
                    @Override
                    public <T> T getValue(Action<T> action) {
                        return ScreenshotViewer.getInstance().getConfig().getOrFallback(ScreenshotViewerOptions.REDIRECT_SCREENSHOT_CHAT_LINKS, false) ? super.getValue(action) : null;
                    }
                })
        );
    }
}

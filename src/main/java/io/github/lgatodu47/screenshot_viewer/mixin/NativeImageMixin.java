package io.github.lgatodu47.screenshot_viewer.mixin;

import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;

@Mixin(NativeImage.class)
public class NativeImageMixin {
    @Redirect(method = "read(Lnet/minecraft/client/texture/NativeImage$Format;Ljava/nio/ByteBuffer;)Lnet/minecraft/client/texture/NativeImage;", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/PngMetadata;validate(Ljava/nio/ByteBuffer;)V"))
    private static void screenshot_viewer$redirect_validate(ByteBuffer buf) {
        // basically mojang decided that only png files could be read.
        // so redirecting this method allows other file types to pass the validation.
        // I admit this can be dangerous but whatever if there's an error then the fantastic library STB_image will let us know.
        // the worst that could happen would be a memory access violation, which would simply crash the game and print a memory dump.
        // but if older minecraft did not check for png then it's probably safe I guess 💀
    }
}

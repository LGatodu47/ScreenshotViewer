package io.github.lgatodu47.screenshot_viewer.mixin;

import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(Main.class)
public class MainMixin {
    /**
     * Sets the 'java.awt.headless' property to false on non MAC-Os systems, therefore allowing to copy images to clipboard.<br>
     * Minecraft uses LWJGL with GLFW to handle the clipboard content and GLFW only allows copying text. As Minecraft
     * disables Java AWT (because might cause some issues) by setting this property to 'true' we need to activate it ourselves
     * as early as possible. Note that on MAC-Os we do not need the Java AWT Toolkit to copy an image to the clipboard.
     *
     * @param args The provided program arguments to start Minecraft.
     * @param ci The callback info from the mixin (unused).
     */
    @Inject(method = "main", at = @At("HEAD"), remap = false)
    private static void screenshot_viewer$inject_main(String[] args, CallbackInfo ci) {
        if(!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac") && !System.getProperties().containsKey("screenshot_viewer.debug.disable_headless_hook")) {
            System.out.println("Screenshot Viewer sets 'java.awt.headless' to false!");
            System.setProperty("java.awt.headless", "false");
        }
    }
}

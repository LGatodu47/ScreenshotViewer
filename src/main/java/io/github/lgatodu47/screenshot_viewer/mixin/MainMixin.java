package io.github.lgatodu47.screenshot_viewer.mixin;

import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(Main.class)
public class MainMixin {
    @Inject(method = "main", at = @At("HEAD"), remap = false)
    private static void screenshot_viewer$inject_main(String[] args, CallbackInfo ci) {
        if(!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) {
            System.out.println("Screenshot Viewer sets 'java.awt.headless' to false!");
            System.setProperty("java.awt.headless", "false");
        }
    }
}

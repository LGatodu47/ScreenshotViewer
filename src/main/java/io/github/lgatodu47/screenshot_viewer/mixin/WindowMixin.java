package io.github.lgatodu47.screenshot_viewer.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.PointerBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static org.lwjgl.glfw.GLFW.glfwGetMonitors;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.system.MemoryUtil.NULL;

@Mixin(Window.class)
public class WindowMixin {
    /**
     * Simple Redirect that allows us to create the window on the monitor we choose (through the property 'screenshot_viewer.debug.monitor').
     * This is just a small feature that can be useful when you have 2 screens.
     */
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwGetPrimaryMonitor()J", remap = false))
    private long redirect_glfwGetPrimaryMonitor() {
        if(System.getProperties().containsKey("screenshot_viewer.debug.monitor")) {
            try {
                int monitorIndex = Integer.parseInt(System.getProperty("screenshot_viewer.debug.monitor"));
                PointerBuffer monitors = glfwGetMonitors();
                if(monitors != null) {
                    long monitor = monitors.get(monitorIndex);
                    if(monitor != NULL) {
                        return monitor;
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return glfwGetPrimaryMonitor();
    }
}

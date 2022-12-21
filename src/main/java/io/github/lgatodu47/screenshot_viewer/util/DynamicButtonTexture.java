package io.github.lgatodu47.screenshot_viewer.util;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class DynamicButtonTexture extends ResourceLocation {
    private final String btnTexture;

    public DynamicButtonTexture(String btnTexture) {
        super(ScreenshotViewer.MODID, "dynamic_button_texture");
        this.btnTexture = btnTexture;
    }

    @Nonnull
    @Override
    public String getResourcePath() {
        String path = "textures/gui/";
        if(!ScreenshotViewer.getInstance().getConfig().useNewButtonTextures.getAsBoolean()) {
            path = path.concat("retro/");
        }
        return path.concat(btnTexture).concat(".png");
    }

    @Nonnull
    @Override
    public String toString() {
        return getResourceDomain() + ":" + getResourcePath();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof ResourceLocation)) {
            return false;
        } else {
            ResourceLocation loc = (ResourceLocation) other;
            return getResourceDomain().equals(loc.getResourceDomain()) && getResourcePath().equals(loc.getResourcePath());
        }
    }

    @Override
    public int hashCode() {
        return 31 * getResourceDomain().hashCode() + getResourcePath().hashCode();
    }

    @Override
    public int compareTo(ResourceLocation other) {
        int i = getResourceDomain().compareTo(other.getResourceDomain());

        if (i == 0) {
            i = getResourcePath().compareTo(other.getResourcePath());
        }

        return i;
    }
}

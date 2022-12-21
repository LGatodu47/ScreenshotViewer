package io.github.lgatodu47.screenshot_viewer.screens.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ScreenshotViewerImageButton extends ScreenshotViewerButton {
    protected final int u, v, hoveredVOffset, textureWidth, textureHeight;
    protected final ResourceLocation texture;

    public ScreenshotViewerImageButton(int x, int y, int width, int height,
                                       int u, int v, int hoveredVOffset, ResourceLocation texture, int textureWidth, int textureHeight,
                                       String text, ButtonAction action) {
        this(x, y, width, height, u, v, hoveredVOffset, texture, textureWidth, textureHeight, text, action, null);
    }

    public ScreenshotViewerImageButton(int x, int y, int width, int height,
                                       int u, int v, int hoveredVOffset, ResourceLocation texture, int textureWidth, int textureHeight,
                                       String text, ButtonAction action, @Nullable ButtonTooltip tooltip) {
        super(x, y, width, height, text, action, tooltip);
        this.u = u;
        this.v = v;
        this.hoveredVOffset = hoveredVOffset;
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    @Override
    public void drawButton(@Nonnull Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (this.visible) {
            this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
            mc.getTextureManager().bindTexture(this.texture);
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.disableDepth();
            int vOffset = this.v;
            if (!enabled) {
                vOffset += this.hoveredVOffset * 2;
            } else if (hovered) {
                vOffset += this.hoveredVOffset;
            }

            drawModalRectWithCustomSizedTexture(this.x, this.y, this.u, vOffset, this.width, this.height, this.textureWidth, this.textureHeight);
            GlStateManager.enableDepth();
        }
    }
}

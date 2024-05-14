package io.github.lgatodu47.screenshot_viewer.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

public class IconButtonWidget extends Button {
    private static final Vector2i DEFAULT_SIZE = new Vector2i(20, 20);
    @Nullable
    private final ResourceLocation iconTexture;

    public IconButtonWidget(int x, int y, int width, int height, Component message, @Nullable ResourceLocation iconTexture, OnPress pressAction) {
        super(x, y, width, height, message, pressAction, DEFAULT_NARRATION);
        this.iconTexture = iconTexture;
    }

    @Nullable
    public ResourceLocation getIconTexture() {
        return iconTexture;
    }

    public Vector2i getIconTextureSize() {
        return DEFAULT_SIZE;
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        context.setColor(1, 1, 1, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        context.blitNineSliced(WIDGETS_LOCATION, getX(), getY(), getWidth(), getHeight(), 20, 4, 200, 20, 0, getTextureY());
        ResourceLocation icon = getIconTexture();
        if(icon != null) {
            Vector2i iconTexSize = getIconTextureSize();
            context.blit(icon, getX(), getY(), 0, 0, getWidth(), getHeight(), iconTexSize.x(), iconTexSize.y());
        }
        context.setColor(1, 1, 1, 1);
    }

    protected int getTextureY() {
        int i = 1;
        if (!this.active) {
            i = 0;
        } else if (this.isHoveredOrFocused()) {
            i = 2;
        }

        return 46 + i * 20;
    }
}

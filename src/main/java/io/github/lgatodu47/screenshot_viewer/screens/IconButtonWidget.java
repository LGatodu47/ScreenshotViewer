package io.github.lgatodu47.screenshot_viewer.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class IconButtonWidget extends Button {
    @Nullable
    private final Identifier iconTexture;

    public IconButtonWidget(int x, int y, int width, int height, Component message, @Nullable Identifier iconTexture, OnPress pressAction) {
        super(x, y, width, height, message, pressAction, DEFAULT_NARRATION);
        this.iconTexture = iconTexture;
    }

    @Nullable
    public Identifier getIconTexture() {
        return iconTexture;
    }

    public WidgetSprites getBackgroundTexture() {
        return AbstractButton.SPRITES;
    }

    @Override
    public void renderContents(GuiGraphics context, int mouseX, int mouseY, float delta) {
        context.blitSprite(RenderPipelines.GUI_TEXTURED, getBackgroundTexture().get(this.active, isHoveredOrFocused()), getX(), getY(), getWidth(), getHeight(), getAlpha());
        Identifier icon = getIconTexture();
        if(icon != null) {
            context.blitSprite(RenderPipelines.GUI_TEXTURED, icon, getX(), getY(), getWidth(), getHeight(), getAlpha());
        }
    }
}

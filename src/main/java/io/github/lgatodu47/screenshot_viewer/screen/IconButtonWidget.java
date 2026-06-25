package io.github.lgatodu47.screenshot_viewer.screen;

import io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots.ManageScreenshotsScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.renderer.RenderPipelines;

public class IconButtonWidget extends Button {
    @Nullable
    private final Identifier iconTexture;

    public IconButtonWidget(int x, int y, int width, int height, net.minecraft.network.chat.Component message, @Nullable Identifier iconTexture, OnPress pressAction) {
        super(x, y, width, height, message, pressAction, DEFAULT_NARRATION);
        this.iconTexture = iconTexture;
    }

    @Nullable
    public Identifier getIconTexture() {
        return iconTexture;
    }

    public WidgetSprites getBackgroundTexture() {
        return ManageScreenshotsScreen.DEFAULT_BUTTON_TEXTURES;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        context.blitSprite(RenderPipelines.GUI_TEXTURED, getBackgroundTexture().get(this.active, isHoveredOrFocused()), getX(), getY(), getWidth(), getHeight(), getAlpha());
        Identifier icon = getIconTexture();
        if(icon != null) {
            context.blitSprite(RenderPipelines.GUI_TEXTURED, icon, getX(), getY(), getWidth(), getHeight(), getAlpha());
        }
    }
}

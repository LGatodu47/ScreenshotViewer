package io.github.lgatodu47.screenshot_viewer.screen;

import io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots.ManageScreenshotsScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.gl.RenderPipelines;

public class IconButtonWidget extends ButtonWidget {
    @Nullable
    private final Identifier iconTexture;

    public IconButtonWidget(int x, int y, int width, int height, net.minecraft.text.Text message, @Nullable Identifier iconTexture, PressAction pressAction) {
        super(x, y, width, height, message, pressAction, DEFAULT_NARRATION_SUPPLIER);
        this.iconTexture = iconTexture;
    }

    @Nullable
    public Identifier getIconTexture() {
        return iconTexture;
    }

    public ButtonTextures getBackgroundTexture() {
        return ManageScreenshotsScreen.DEFAULT_BUTTON_TEXTURES;
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, getBackgroundTexture().get(this.active, isSelected()), getX(), getY(), getWidth(), getHeight(), getAlpha());
        Identifier icon = getIconTexture();
        if(icon != null) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, icon, getX(), getY(), getWidth(), getHeight(), getAlpha());
        }
    }
}

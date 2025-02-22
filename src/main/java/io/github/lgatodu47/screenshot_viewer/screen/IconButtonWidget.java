package io.github.lgatodu47.screenshot_viewer.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots.ManageScreenshotsScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Nullable;

public class IconButtonWidget extends ButtonWidget {
    @Nullable
    private final Identifier iconTexture;

    public IconButtonWidget(int x, int y, int width, int height, Text message, @Nullable Identifier iconTexture, PressAction pressAction) {
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
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        context.drawGuiTexture(RenderLayer::getGuiTextured, getBackgroundTexture().get(this.active, isSelected()), getX(), getY(), getWidth(), getHeight(), ColorHelper.getWhite(this.alpha));
        Identifier icon = getIconTexture();
        if(icon != null) {
            context.drawGuiTexture(RenderLayer::getGuiTextured, icon, getX(), getY(), getWidth(), getHeight());
        }
    }
}

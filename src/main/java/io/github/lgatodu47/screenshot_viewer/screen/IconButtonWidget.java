package io.github.lgatodu47.screenshot_viewer.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

public class IconButtonWidget extends ButtonWidget {
    private static final Vector2i DEFAULT_SIZE = new Vector2i(20, 20);
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

    public Vector2i getIconTextureSize() {
        return DEFAULT_SIZE;
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        context.setShaderColor(1, 1, 1, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        context.drawNineSlicedTexture(WIDGETS_TEXTURE, getX(), getY(), getWidth(), getHeight(), 20, 4, 200, 20, 0, getTextureY());
        Identifier icon = getIconTexture();
        if(icon != null) {
            Vector2i iconTexSize = getIconTextureSize();
            context.drawTexture(icon, getX(), getY(), 0, 0, getWidth(), getHeight(), iconTexSize.x(), iconTexSize.y());
        }
        context.setShaderColor(1, 1, 1, 1);
    }

    protected int getTextureY() {
        int i = 1;
        if (!this.active) {
            i = 0;
        } else if (this.isSelected()) {
            i = 2;
        }

        return 46 + i * 20;
    }
}

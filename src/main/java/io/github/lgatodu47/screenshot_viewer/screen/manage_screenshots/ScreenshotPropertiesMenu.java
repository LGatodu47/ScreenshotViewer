package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions;
import io.github.lgatodu47.screenshot_viewer.screen.IconButtonWidget;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

class ScreenshotPropertiesMenu extends AbstractParentElement implements Drawable {
    private static final Identifier BACKGROUND_TEXTURE = new Identifier(ScreenshotViewer.MODID, "textures/gui/screenshot_properties_background.png");
    static final Identifier OPEN_ICON = new Identifier(ScreenshotViewer.MODID, "widget/icons/open_folder");
    static final Identifier COPY_ICON = new Identifier(ScreenshotViewer.MODID, "widget/icons/copy");
    static final Identifier DELETE_ICON = new Identifier(ScreenshotViewer.MODID, "widget/icons/delete");
    static final Identifier RENAME_ICON = new Identifier(ScreenshotViewer.MODID, "widget/icons/rename");
    private static final Identifier CLOSE_ICON = new Identifier(ScreenshotViewer.MODID, "widget/icons/close");
    private static final int BUTTON_SIZE = 19;

    private final Supplier<MinecraftClient> mcSupplier;
    private final List<ClickableWidget> buttons = new ArrayList<>();

    private int x, y, width, height;
    private ScreenshotImageHolder targetScreenshot;
    private boolean shouldRender;

    ScreenshotPropertiesMenu(Supplier<MinecraftClient> mcSupplier) {
        this.mcSupplier = mcSupplier;
        addButton(OPEN_ICON, ScreenshotViewerTexts.OPEN_FILE, ScreenshotImageHolder::openFile);
        addButton(COPY_ICON, ScreenshotViewerTexts.COPY, ScreenshotImageHolder::copyScreenshot);
        addButton(DELETE_ICON, ScreenshotViewerTexts.DELETE, ScreenshotImageHolder::requestFileDeletion);
        addButton(RENAME_ICON, ScreenshotViewerTexts.RENAME_FILE, ScreenshotImageHolder::renameFile);
        addButton(CLOSE_ICON, ScreenshotViewerTexts.CLOSE_PROPERTIES, null);
    }

    private void addButton(Identifier texture, Text description, @Nullable Consumer<ScreenshotImageHolder> action) {
        this.buttons.add(new Button(texture, description, btn -> {
            if(action != null && targetScreenshot != null) {
                action.accept(targetScreenshot);
            }
            hide();
        }));
    }

    void show(int x, int y, int parentWidth, int parentHeight, ScreenshotImageHolder targetScreenshot) {
        this.targetScreenshot = targetScreenshot;
        final int spacing = 2;

        TextRenderer font = mcSupplier.get().textRenderer;
        final int largestTextWidth = buttons.stream().map(ClickableWidget::getMessage).mapToInt(font::getWidth).max().orElse(0);
        this.width = spacing * 2 + Math.max(font.getWidth(targetScreenshot.getScreenshotFile().getName()), BUTTON_SIZE + largestTextWidth + spacing * 2);
        this.height = spacing * 3 + font.fontHeight + BUTTON_SIZE * buttons.size();

        // Offset the widget if it goes out of the screen
        if (x + width > parentWidth) {
            this.x = x - width;
        } else {
            this.x = x;
        }
        if (y + height > parentHeight) {
            this.y = y - height;
        } else {
            this.y = y;
        }

        for (int i = 0; i < this.buttons.size(); i++) {
            this.buttons.get(i).setDimensionsAndPosition(width - 2 * spacing, BUTTON_SIZE, this.x + spacing, this.y + spacing * 2 + font.fontHeight + BUTTON_SIZE * i);
        }

        shouldRender = true;
    }

    void hide() {
        shouldRender = false;
        x = y = width = height = 0;
    }

    boolean renders() {
        return shouldRender;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (shouldRender) {
            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(0, 0, 1);
            final int spacing = 2;

            //corners
            context.drawTexture(BACKGROUND_TEXTURE, x, y, 0, 0,
                    2, 2, 8, 8);
            context.drawTexture(BACKGROUND_TEXTURE, x+width-2, y, 6, 0,
                    2, 2, 8, 8);
            context.drawTexture(BACKGROUND_TEXTURE, x, y+height-2, 0, 6,
                    2, 2, 8, 8);
            context.drawTexture(BACKGROUND_TEXTURE, x+width-2, y+height-2, 6, 6,
                    2, 2, 8, 8);
            //sides
            context.drawTexture(BACKGROUND_TEXTURE, x+2, y, (float) (width * 3) /2, 0,
                    width-4, 2, width*4, 8);
            context.drawTexture(BACKGROUND_TEXTURE, x, y+2, 0, (float) (height * 3) /2,
                    2, height-4, 8, height*4);
            context.drawTexture(BACKGROUND_TEXTURE, x+2, y+height-2, (float) (width * 3) /2, 6,
                    width-4, 2, width*4, 8);
            context.drawTexture(BACKGROUND_TEXTURE, x+width-2, y+2, 6, (float) (height * 3) /2,
                    2, height-4, 8, height*4);
            //center
            context.drawTexture(BACKGROUND_TEXTURE, x+2, y+2,
                    (float) (width * 3) /2, (float) (height * 3) /2,
                    width-4, height-4,
                    width*4, height*4);

            context.drawTextWithShadow(mcSupplier.get().textRenderer, targetScreenshot.getScreenshotFile().getName(), x + spacing, y + spacing, 0xFFFFFFFF);
            for (ClickableWidget widget : buttons) {
                widget.render(context, mouseX, mouseY, delta);
                matrices.push();
                matrices.translate(0, 0, 1);
                context.drawTextWithShadow(mcSupplier.get().textRenderer, widget.getMessage(), widget.getX() + BUTTON_SIZE + spacing, (int) (widget.getY() + (widget.getHeight() - 9) / 2.f + spacing), 0xFFFFFFFF);
                matrices.pop();
            }
            matrices.pop();
        }
    }

    @Override
    public List<? extends Element> children() {
        return List.copyOf(this.buttons);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < x || mouseY < y || mouseX > x + width || mouseY > y + height) {
            hide();
            return false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputUtil.GLFW_KEY_ESCAPE) {
            hide();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static final class Button extends IconButtonWidget {
        private static final ButtonTextures BUTTON_TEXTURES = new ButtonTextures(
                new Identifier(ScreenshotViewer.MODID, "widget/properties_button_enabled"),
                new Identifier(ScreenshotViewer.MODID, "widget/properties_button"),
                new Identifier(ScreenshotViewer.MODID, "widget/properties_button_hovered")
        );
        private static final ButtonTextures TEXTURES_FOR_WIDE = new ButtonTextures(
                new Identifier(ScreenshotViewer.MODID, "textures/gui/sprites/widget/properties_button_enabled.png"),
                new Identifier(ScreenshotViewer.MODID, "textures/gui/sprites/widget/properties_button.png"),
                new Identifier(ScreenshotViewer.MODID, "textures/gui/sprites/widget/properties_button_hovered.png")
        );

        private boolean renderWide = ManageScreenshotsScreen.CONFIG.getOrFallback(ScreenshotViewerOptions.RENDER_WIDE_PROPERTIES_BUTTON, true);

        public Button(Identifier texture, Text title, PressAction pressAction) {
            super(0, 0, BUTTON_SIZE, BUTTON_SIZE, title, texture, pressAction);
        }

        @Override
        public void setDimensionsAndPosition(int width, int height, int x, int y) {
            this.renderWide = ManageScreenshotsScreen.CONFIG.getOrFallback(ScreenshotViewerOptions.RENDER_WIDE_PROPERTIES_BUTTON, true);
            // provided width is for wide while current width is for squared.
            super.setDimensionsAndPosition(renderWide ? width : getWidth(), height, x, y);
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            context.setShaderColor(1, 1, 1, this.alpha);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            Identifier backgroundTexture = getBackgroundTexture().get(this.active, isSelected());
            if (renderWide) {
                context.drawTexture(backgroundTexture, getX(), getY(), 0, 0, 1, getHeight(), BUTTON_SIZE, BUTTON_SIZE);
                context.drawTexture(backgroundTexture, getX() + 1, getY(), getWidth() - 2, getHeight(), 1, 0, BUTTON_SIZE - 2, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);
                context.drawTexture(backgroundTexture, getX() + getWidth() - 1, getY(), 18, 0, 1, getHeight(), BUTTON_SIZE, BUTTON_SIZE);
            } else {
                context.drawGuiTexture(backgroundTexture, getX(), getY(), getWidth(), getHeight());
            }
            Identifier icon = getIconTexture();
            if(icon != null) {
                context.drawGuiTexture(icon, getX(), getY(), BUTTON_SIZE, getHeight());
            }
            context.setShaderColor(1, 1, 1, 1);
        }

        @Override
        public boolean isSelected() {
            return isHovered();
        }

        @Override
        public ButtonTextures getBackgroundTexture() {
            return renderWide ? TEXTURES_FOR_WIDE : BUTTON_TEXTURES;
        }
    }
}

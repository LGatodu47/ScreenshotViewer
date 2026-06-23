package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

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
import net.minecraft.text.Text;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gl.RenderPipelines;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

class ScreenshotPropertiesMenu extends AbstractParentElement implements Drawable {
    private static final Identifier BACKGROUND_TEXTURE_ATLAS = Identifier.of(ScreenshotViewer.MODID, "screenshot_properties_background");
    static final Identifier OPEN_ICON = Identifier.of(ScreenshotViewer.MODID, "widget/icons/open_folder");
    static final Identifier COPY_ICON = Identifier.of(ScreenshotViewer.MODID, "widget/icons/copy");
    static final Identifier DELETE_ICON = Identifier.of(ScreenshotViewer.MODID, "widget/icons/delete");
    static final Identifier RENAME_ICON = Identifier.of(ScreenshotViewer.MODID, "widget/icons/rename");
    private static final Identifier CLOSE_ICON = Identifier.of(ScreenshotViewer.MODID, "widget/icons/close");
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
            final int spacing = 2;

            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE_ATLAS, x, y, width, height);

            context.drawTextWithShadow(mcSupplier.get().textRenderer, targetScreenshot.getScreenshotFile().getName(), x + spacing, y + spacing, 0xFFFFFFFF);
            for (ClickableWidget widget : buttons) {
                widget.render(context, mouseX, mouseY, delta);
                context.drawTextWithShadow(mcSupplier.get().textRenderer, widget.getMessage(), widget.getX() + BUTTON_SIZE + spacing, (int) (widget.getY() + (widget.getHeight() - 9) / 2.f + spacing), 0xFFFFFFFF);
            }
        }
    }

    @Override
    public List<? extends Element> children() {
        return List.copyOf(this.buttons);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.x() < x || click.y() < y || click.x() > x + width || click.y() > y + height) {
            hide();
            return false;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == InputUtil.GLFW_KEY_ESCAPE) {
            hide();
            return true;
        }
        return super.keyPressed(input);
    }

    private static final class Button extends IconButtonWidget {
        private static final ButtonTextures BUTTON_TEXTURES = new ButtonTextures(
                Identifier.of(ScreenshotViewer.MODID, "widget/properties_button_enabled"),
                Identifier.of(ScreenshotViewer.MODID, "widget/properties_button"),
                Identifier.of(ScreenshotViewer.MODID, "widget/properties_button_hovered")
        );
        private static final ButtonTextures TEXTURES_FOR_WIDE = new ButtonTextures(
                Identifier.of(ScreenshotViewer.MODID, "textures/gui/sprites/widget/properties_button_enabled.png"),
                Identifier.of(ScreenshotViewer.MODID, "textures/gui/sprites/widget/properties_button.png"),
                Identifier.of(ScreenshotViewer.MODID, "textures/gui/sprites/widget/properties_button_hovered.png")
        );

        private boolean renderWide = ManageScreenshotsScreen.CONFIG.getOrFallback(ScreenshotViewerOptions.RENDER_WIDE_PROPERTIES_BUTTON, true);

        public Button(Identifier texture, net.minecraft.text.Text title, PressAction pressAction) {
            super(0, 0, BUTTON_SIZE, BUTTON_SIZE, title, texture, pressAction);
        }

        @Override
        public void setDimensionsAndPosition(int width, int height, int x, int y) {
            this.renderWide = ManageScreenshotsScreen.CONFIG.getOrFallback(ScreenshotViewerOptions.RENDER_WIDE_PROPERTIES_BUTTON, true);
            // provided width is for wide while current width is for squared.
            super.setDimensionsAndPosition(renderWide ? width : getWidth(), height, x, y);
        }

        @Override
        protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            Identifier backgroundTexture = getBackgroundTexture().get(this.active, isSelected());
            if (renderWide) {
                context.drawTexture(RenderPipelines.GUI_TEXTURED, backgroundTexture, getX(), getY(), 0, 0, 1, getHeight(), BUTTON_SIZE, BUTTON_SIZE, ColorHelper.getWhite(this.alpha));
                context.drawTexture(RenderPipelines.GUI_TEXTURED, backgroundTexture, getX() + 1, getY(), 1, 0, getWidth() - 2, getHeight(), BUTTON_SIZE - 2, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, ColorHelper.getWhite(this.alpha));
                context.drawTexture(RenderPipelines.GUI_TEXTURED, backgroundTexture, getX() + getWidth() - 1, getY(), 18, 0, 1, getHeight(), BUTTON_SIZE, BUTTON_SIZE, ColorHelper.getWhite(this.alpha));
            } else {
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, backgroundTexture, getX(), getY(), BUTTON_SIZE, getHeight(), ColorHelper.getWhite(this.alpha));
            }

            Identifier icon = getIconTexture();
            if (icon != null) {
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, icon, getX(), getY(), BUTTON_SIZE, getHeight(), ColorHelper.getWhite(this.alpha));
            }
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

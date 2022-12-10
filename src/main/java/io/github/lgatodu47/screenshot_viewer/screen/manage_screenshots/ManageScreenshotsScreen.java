package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import io.github.lgatodu47.catconfig.CatConfig;
import io.github.lgatodu47.catconfigmc.screen.ConfigListener;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.io.File;
import java.util.Optional;
import java.util.function.Supplier;

public class ManageScreenshotsScreen extends Screen implements ConfigListener {
    // Package-private config instance accessible in all the package classes
    static final CatConfig CONFIG = ScreenshotViewer.getInstance().getConfig();
    static final Logger LOGGER = LogUtils.getLogger();

    private static final Identifier CONFIG_BUTTON_TEXTURE = new Identifier(ScreenshotViewer.MODID, "textures/gui/config_button.png");
    private static final Identifier REFRESH_BUTTON_TEXTURE = new Identifier(ScreenshotViewer.MODID, "textures/gui/refresh_button.png");
    private static final Identifier ASCENDING_ORDER_BUTTON_TEXTURE = new Identifier(ScreenshotViewer.MODID, "textures/gui/ascending_order_button.png");
    private static final Identifier DESCENDING_ORDER_BUTTON_TEXTURE = new Identifier(ScreenshotViewer.MODID, "textures/gui/descending_order_button.png");
    private static final Identifier OPEN_FOLDER_BUTTON_TEXTURE = new Identifier(ScreenshotViewer.MODID, "textures/gui/open_folder_button.png");

    private final Screen parent;
    private final EnlargedScreenshotScreen enlargedScreenshot;
    private final ScreenshotPropertiesMenu screenshotProperties;
    private ScreenshotList list;

    public ManageScreenshotsScreen(Screen parent) {
        super(ScreenshotViewer.translatable("screen", "manage_screenshots"));
        this.parent = parent;
        this.enlargedScreenshot = new EnlargedScreenshotScreen();
        this.screenshotProperties = new ScreenshotPropertiesMenu(this::client, () -> width, () -> height);
    }

    MinecraftClient client() {
        return client;
    }

    /// Basic Screen implementations ///

    @Override
    public void tick() {
        if(screenshotProperties != null) {
            screenshotProperties.tick();
        }
    }

    @Override
    protected void init() {
        if(client == null) {
            return;
        }

        final int spacing = 8;
        final int btnHeight = 20;

        this.enlargedScreenshot.init(client, width, height);

        //Main content
        int contentWidth = width - 24;
        int contentHeight = height - spacing * 5 - btnHeight;
        // We avoid creating the list every time we refresh the screen, so we don't have to load the screenshots again (which takes time)
        if(list == null) {
            list = new ScreenshotList(this, 12, spacing * 3, width - 24, height - spacing * 5 - btnHeight);
            list.init();
        }
        else {
            list.updateSize(contentWidth, contentHeight);
            list.updateChildren();
        }
        // Adds it to the 'children' list which makes 'mouseClicked' and other methods work with it.
        addSelectableChild(list);

        // Button stuff
        final int btnY = height - spacing - btnHeight;
        final int btnSize = 20;
        final int bigBtnWidth = 200;

        // Config Button
        addDrawableChild(new ExtendedTexturedButtonWidget(2, 2, btnSize, btnSize, 0, 0, btnSize, CONFIG_BUTTON_TEXTURE, 32, 64, button -> {
            client.setScreen(new ScreenshotViewerConfigScreen(this));
        }, ScreenshotViewer.translatable("screen", "button.config"), ScreenshotViewer.translatable("screen", "button.config")).offsetTooltip());
        // Order Button
        addDrawableChild(new ExtendedTexturedButtonWidget(spacing, btnY, btnSize, btnSize, 0, 0, btnSize, null, 32, 64, button -> {
            if(list != null) {
                list.invertOrder();
            }
        }, list == null ? null : ScreenshotViewer.translatable("screen", list.isInvertedOrder() ? "button.order.descending" : "button.order.ascending"), ScreenshotViewer.translatable("screen", "button.order")) {
            @Override
            public @Nullable Identifier getTexture() {
                return list == null ? null : list.isInvertedOrder() ? DESCENDING_ORDER_BUTTON_TEXTURE : ASCENDING_ORDER_BUTTON_TEXTURE;
            }
        });
        // Screenshot Folder Button
        addDrawableChild(new ExtendedTexturedButtonWidget(spacing * 2 + btnSize, btnY, btnSize, btnSize, 0, 0, btnSize, OPEN_FOLDER_BUTTON_TEXTURE, 32, 64, btn -> {
            Util.getOperatingSystem().open(new File(this.client.runDirectory, "screenshots"));
        }, ScreenshotViewer.translatable("screen", "button.screenshot_folder"), ScreenshotViewer.translatable("screen", "button.screenshot_folder")));
        // Done Button
        addDrawableChild(new ExtendedButtonWidget((width - bigBtnWidth) / 2, btnY, bigBtnWidth, btnHeight, ScreenTexts.DONE, button -> close()));
        // Refresh Button
        addDrawableChild(new ExtendedTexturedButtonWidget(width - spacing - btnSize, btnY, btnSize, btnSize, 0, 0, btnSize, REFRESH_BUTTON_TEXTURE, 32, 64, button -> {
            list.init();
        }, ScreenshotViewer.translatable("screen", "button.refresh"), ScreenshotViewer.translatable("screen", "button.refresh")));
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        // Adapts the size of the enlarged screenshot when resized
        this.enlargedScreenshot.resize(client, width, height);
        // Hides the screenshot properties menu
        this.screenshotProperties.hide();
    }

    private float screenshotScaleAnimation;

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        if(list != null) {
            list.render(matrices, mouseX, mouseY, delta, !(enlargedScreenshot.renders() || screenshotProperties.renders()));
        }
        drawCenteredText(matrices, textRenderer, title,width / 2, 8, 0xFFFFFF);
        Text text = ScreenshotViewer.translatable("screen", "screenshot_manager.zoom");
        drawTextWithShadow(matrices, textRenderer, text, width - textRenderer.getWidth(text) - 8, 8, isCtrlDown ? 0x18DE39 : 0xF0CA22);
        super.render(matrices, mouseX, mouseY, delta);
        screenshotProperties.render(matrices, mouseX, mouseY, delta);
        if(enlargedScreenshot.renders()) {
            float animationTime = 1;

            if(CONFIG.getOrFallback(ScreenshotViewerOptions.ENABLE_SCREENSHOT_ENLARGEMENT_ANIMATION, true)) {
                if(screenshotScaleAnimation < 1f) {
                    animationTime = (float) (1 - Math.pow(1 - (screenshotScaleAnimation += 0.03), 3));
                }
            }

            matrices.push();
            matrices.translate(0, 0, 1);
            enlargedScreenshot.renderBackground(matrices);
            matrices.translate((enlargedScreenshot.width / 2f) * (1 - animationTime), (enlargedScreenshot.height / 2f) * (1 - animationTime), 0);
            matrices.scale(animationTime, animationTime, animationTime);
            enlargedScreenshot.render(matrices, mouseX, mouseY, delta);
            matrices.pop();
        } else {
            if(screenshotScaleAnimation > 0) {
                screenshotScaleAnimation = 0;
            }

            if(!screenshotProperties.renders()) {
                for (Element element : this.children()) {
                    if (element instanceof CustomHoverState hover) {
                        hover.updateHoveredState(mouseX, mouseY);
                    }
                }
            }
        }
    }

    /// Methods shared between the classes of the package ///

    void enlargeScreenshot(ScreenshotImageHolder showing) {
        this.enlargedScreenshot.show(showing, list);
    }

    void showScreenshotProperties(double mouseX, double mouseY, ScreenshotWidget widget) {
        this.screenshotProperties.show((int) mouseX, (int) mouseY, () -> list.removeEntry(widget), widget.getScreenshotFile(), widget::updateScreenshotFile);
    }

    /// Input handling methods below ///

    private boolean isCtrlDown;

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(screenshotProperties.renders()) {
            return screenshotProperties.keyPressed(keyCode, scanCode, modifiers);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.keyPressed(keyCode, scanCode, modifiers);
        }
        isCtrlDown = keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if(screenshotProperties.renders()) {
            return screenshotProperties.keyReleased(keyCode, scanCode, modifiers);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.keyReleased(keyCode, scanCode, modifiers);
        }
        if(isCtrlDown) {
            isCtrlDown = false;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if(screenshotProperties.renders()) {
            return screenshotProperties.charTyped(chr, modifiers);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if(screenshotProperties.renders()) {
            return screenshotProperties.mouseScrolled(mouseX, mouseY, amount);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.mouseScrolled(mouseX, mouseY, amount);
        }
        if(list != null) {
            if(isCtrlDown) {
                list.updateScreenshotsPerRow(amount);
                return true;
            }

            return list.mouseScrolled(mouseX, mouseY, amount);
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(screenshotProperties.renders()) {
            return screenshotProperties.mouseClicked(mouseX, mouseY, button);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(screenshotProperties.renders()) {
            return screenshotProperties.mouseReleased(mouseX, mouseY, button);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if(screenshotProperties.renders()) {
            return screenshotProperties.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public Optional<Element> hoveredElement(double mouseX, double mouseY) {
        if(screenshotProperties.renders()) {
            return screenshotProperties.hoveredElement(mouseX, mouseY);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.hoveredElement(mouseX, mouseY);
        }
        return super.hoveredElement(mouseX, mouseY);
    }

    /// Other Methods ///

    @Override
    public void close() {
        if(client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public void removed() {
        list.close();
    }

    @Override // From ConfigListener
    public void configUpdated() {
        this.list.onConfigUpdate();
    }

    private static final class ExtendedButtonWidget extends ButtonWidget implements CustomHoverState {
        ExtendedButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
            super(x, y, width, height, message, onPress, Supplier::get);
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            if (!this.visible) {
                return;
            }
            this.renderButton(matrices, mouseX, mouseY, delta);
        }

        @Override
        public void updateHoveredState(int mouseX, int mouseY) {
            this.hovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
        }
    }

    private static class ExtendedTexturedButtonWidget extends TexturedButtonWidget implements CustomHoverState {
        @Nullable
        private final Identifier texture;
        private final int u;
        private final int v;
        private final int hoveredVOffset;
        private final int textureWidth;
        private final int textureHeight;
        @Nullable
        private final Text tooltip;
        private boolean offsetTooltip;

        ExtendedTexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, @Nullable Identifier texture, int textureWidth, int textureHeight, PressAction pressAction, @Nullable Text tooltip, Text text) {
            super(x, y, width, height, u, v, hoveredVOffset, ButtonWidget.WIDGETS_TEXTURE, textureWidth, textureHeight, pressAction, text);
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.u = u;
            this.v = v;
            this.hoveredVOffset = hoveredVOffset;
            this.texture = texture;
            this.tooltip = tooltip;
            setTooltip(Tooltip.of(tooltip));
        }

        ExtendedTexturedButtonWidget offsetTooltip() {
            this.offsetTooltip = true;
            return this;
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            if (!this.visible) {
                return;
            }
            this.renderButton(matrices, mouseX, mouseY, delta);
            applyTooltip();
        }

        private void applyTooltip() {
            if (this.tooltip != null) {
                if (isHovered()) {
                    Screen screen = MinecraftClient.getInstance().currentScreen;
                    if (screen != null) {
                        screen.setTooltip(Tooltip.of(tooltip), getTooltipPositioner(), isFocused());
                    }
                }
            }
        }

        @Override
        protected TooltipPositioner getTooltipPositioner() {
            return offsetTooltip ? (screen, x, y, w, h) -> super.getTooltipPositioner().getPosition(screen, x, y + height, w, h) : super.getTooltipPositioner();
        }

        @Nullable
        public Identifier getTexture() {
            return texture;
        }

        @Override
        public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            Identifier texture = getTexture();
            if(texture == null) {
                DrawableHelper.fill(matrices, getX(), getY(), getX() + width, getY() + height, 0xFFFFFF);
            } else {
                RenderSystem.setShader(GameRenderer::getPositionTexProgram);
                RenderSystem.setShaderTexture(0, texture);
                int vOffset = this.v;
                if (!this.isNarratable()) {
                    vOffset += this.hoveredVOffset * 2;
                } else if (this.isHovered()) {
                    vOffset += this.hoveredVOffset;
                }
                RenderSystem.enableDepthTest();
                DrawableHelper.drawTexture(matrices, this.getX(), this.getY(), this.u, vOffset, this.width, this.height, this.textureWidth, this.textureHeight);
            }
        }

        @Override
        public void updateHoveredState(int mouseX, int mouseY) {
            this.hovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
        }
    }

    private interface CustomHoverState {
        void updateHoveredState(int mouseX, int mouseY);
    }
}

package io.github.lgatodu47.screenshot_viewer.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfig;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfigListener;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.io.File;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ManageScreenshotsScreen extends Screen implements ScreenshotViewerConfigListener {
    // Package-private config instance accessible in all the package classes
    static final ScreenshotViewerConfig CONFIG = ScreenshotViewer.getInstance().getConfig();
    static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation CONFIG_BUTTON_TEXTURE = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/config_button.png");
    private static final ResourceLocation REFRESH_BUTTON_TEXTURE = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/refresh_button.png");
    private static final ResourceLocation ASCENDING_ORDER_BUTTON_TEXTURE = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/ascending_order_button.png");
    private static final ResourceLocation DESCENDING_ORDER_BUTTON_TEXTURE = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/descending_order_button.png");
    private static final ResourceLocation OPEN_FOLDER_BUTTON_TEXTURE = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/open_folder_button.png");

    private final Screen parent;
    private final EnlargedScreenshotScreen enlargedScreenshot;
    private final ScreenshotPropertiesMenu screenshotProperties;
    private ScreenshotList list;

    public ManageScreenshotsScreen(Screen parent) {
        super(ScreenshotViewer.translatable("screen", "manage_screenshots"));
        this.parent = parent;
        this.enlargedScreenshot = new EnlargedScreenshotScreen();
        this.screenshotProperties = new ScreenshotPropertiesMenu(this::client, () -> width, () -> height);
        ScreenshotViewer.getInstance().registerConfigListener(this);
    }

    Minecraft client() {
        return minecraft;
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
        if(minecraft == null) {
            return;
        }

        final int spacing = 8;
        final int btnHeight = 20;

        this.enlargedScreenshot.init(minecraft, width, height);

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
        addWidget(list);

        // Button stuff
        final int btnY = height - spacing - btnHeight;
        final int btnSize = 20;
        final int bigBtnWidth = 200;

        // Config Button
        Optional<BiFunction<Minecraft, Screen, Screen>> configScreenFactory = ScreenshotViewer.getInstance().getConfigScreenFactory();
        Button configButton = new ExtendedTexturedButtonWidget(2, 2, btnSize, btnSize, 0, 0, btnSize, CONFIG_BUTTON_TEXTURE, 32, 64,
                button -> configScreenFactory.ifPresent(f -> minecraft.setScreen(f.apply(minecraft, this))),
                ScreenshotViewer.translatable("screen", configScreenFactory.isPresent() ? "button.config" : "no_config"),
                ScreenshotViewer.translatable("screen", configScreenFactory.isPresent() ? "button.config" : "no_config")
        ).offsetTooltip();
        configButton.active = configScreenFactory.isPresent();
        addRenderableWidget(configButton);
        // Order Button
        addRenderableWidget(new ExtendedTexturedButtonWidget(spacing, btnY, btnSize, btnSize, 0, 0, btnSize, null, 32, 64, button -> {
            if(list != null) {
                list.invertOrder();
            }
        }, null, ScreenshotViewer.translatable("screen", "button.order")) {
            @Override
            protected @Nullable Component getTooltipText() {
                return list == null ? null : ScreenshotViewer.translatable("screen", list.isInvertedOrder() ? "button.order.descending" : "button.order.ascending");
            }

            @Override
            public @Nullable ResourceLocation getTexture() {
                return list == null ? null : list.isInvertedOrder() ? DESCENDING_ORDER_BUTTON_TEXTURE : ASCENDING_ORDER_BUTTON_TEXTURE;
            }
        });
        // Screenshot Folder Button
        addRenderableWidget(new ExtendedTexturedButtonWidget(spacing * 2 + btnSize, btnY, btnSize, btnSize, 0, 0, btnSize, OPEN_FOLDER_BUTTON_TEXTURE, 32, 64, btn -> {
            Util.getPlatform().openFile(new File(this.minecraft.gameDirectory, "screenshots"));
        }, ScreenshotViewer.translatable("screen", "button.screenshot_folder"), ScreenshotViewer.translatable("screen", "button.screenshot_folder")));
        // Done Button
        addRenderableWidget(new ExtendedButtonWidget((width - bigBtnWidth) / 2, btnY, bigBtnWidth, btnHeight, CommonComponents.GUI_DONE, button -> onClose()));
        // Refresh Button
        addRenderableWidget(new ExtendedTexturedButtonWidget(width - spacing - btnSize, btnY, btnSize, btnSize, 0, 0, btnSize, REFRESH_BUTTON_TEXTURE, 32, 64, button -> {
            list.init();
        }, ScreenshotViewer.translatable("screen", "button.refresh"), ScreenshotViewer.translatable("screen", "button.refresh")));
    }

    @Override
    public void resize(Minecraft client, int width, int height) {
        super.resize(client, width, height);
        // Adapts the size of the enlarged screenshot when resized
        this.enlargedScreenshot.resize(client, width, height);
        // Hides the screenshot properties menu
        this.screenshotProperties.hide();
    }

    private float screenshotScaleAnimation;

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        if(list != null) {
            list.render(matrices, mouseX, mouseY, delta, !(enlargedScreenshot.renders() || screenshotProperties.renders()));
        }
        drawCenteredString(matrices, font, title,width / 2, 8, 0xFFFFFF);
        Component text = ScreenshotViewer.translatable("screen", "screenshot_manager.zoom");
        drawString(matrices, font, text, width - font.width(text) - 8, 8, isCtrlDown ? 0x18DE39 : 0xF0CA22);
        super.render(matrices, mouseX, mouseY, delta);
        screenshotProperties.render(matrices, mouseX, mouseY, delta);
        if(enlargedScreenshot.renders()) {
            float animationTime = 1;

            if(CONFIG.enableScreenshotEnlargementAnimation.get()) {
                if(screenshotScaleAnimation < 1f) {
                    animationTime = (float) (1 - Math.pow(1 - (screenshotScaleAnimation += 0.03), 3));
                }
            }

            matrices.pushPose();
            matrices.translate(0, 0, 1);
            enlargedScreenshot.renderBackground(matrices);
            matrices.translate((enlargedScreenshot.width / 2f) * (1 - animationTime), (enlargedScreenshot.height / 2f) * (1 - animationTime), 0);
            matrices.scale(animationTime, animationTime, animationTime);
            enlargedScreenshot.render(matrices, mouseX, mouseY, delta);
            matrices.popPose();
        } else {
            if(screenshotScaleAnimation > 0) {
                screenshotScaleAnimation = 0;
            }

            if(!screenshotProperties.renders()) {
                for (GuiEventListener element : this.children()) {
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
    public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        if(screenshotProperties.renders()) {
            return screenshotProperties.getChildAt(mouseX, mouseY);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.getChildAt(mouseX, mouseY);
        }
        return super.getChildAt(mouseX, mouseY);
    }

    /// Other Methods ///

    @Override
    public void onClose() {
        if(minecraft != null) {
            this.minecraft.setScreen(parent);
        }
        ScreenshotViewer.getInstance().unregisterConfigListener(this);
    }

    @Override
    public void removed() {
        list.close();
    }

    @Override
    public void onConfigReloaded() {
        list.configUpdated();
    }

    private static final class ExtendedButtonWidget extends Button implements CustomHoverState {
        ExtendedButtonWidget(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, Supplier::get);
        }

        @Override
        public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
            if (!this.visible) {
                return;
            }
            super.renderWidget(matrices, mouseX, mouseY, delta);
        }

        @Override
        public void updateHoveredState(int mouseX, int mouseY) {
            this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
        }
    }

    private static class ExtendedTexturedButtonWidget extends ImageButton implements CustomHoverState {
        @Nullable
        private final ResourceLocation texture;
        private final int u;
        private final int v;
        private final int hoveredVOffset;
        private final int textureWidth;
        private final int textureHeight;
        @Nullable
        private final Component tooltip;
        private boolean offsetTooltip;

        ExtendedTexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, @Nullable ResourceLocation texture, int textureWidth, int textureHeight, OnPress pressAction, @Nullable Component tooltip, Component text) {
            super(x, y, width, height, u, v, hoveredVOffset, Button.WIDGETS_LOCATION, textureWidth, textureHeight, pressAction, text);
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.u = u;
            this.v = v;
            this.hoveredVOffset = hoveredVOffset;
            this.texture = texture;
            this.tooltip = tooltip;
            if(tooltip != null) {
                setTooltip(Tooltip.create(tooltip));
            }
        }

        ExtendedTexturedButtonWidget offsetTooltip() {
            offsetTooltip = true;
            return this;
        }

        @Override
        public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
            if (!this.visible) {
                return;
            }
            this.renderWidget(matrices, mouseX, mouseY, delta);
            updateTooltip();
        }

        private void updateTooltip() {
            Component tooltip = getTooltipText();
            if (tooltip != null) {
                if (this.isHovered || this.isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard()) {
                    Screen screen = Minecraft.getInstance().screen;
                    if (screen != null) {
                        screen.setTooltipForNextRenderPass(Tooltip.create(tooltip), this.createTooltipPositioner(), this.isFocused());
                    }
                }
            }
        }

        @Nullable
        protected Component getTooltipText() {
            return tooltip;
        }

        @Override
        protected ClientTooltipPositioner createTooltipPositioner() {
            return offsetTooltip ? (screen, x, y, w, h) -> super.createTooltipPositioner().positionTooltip(screen, x, y + height, w, h) : super.createTooltipPositioner();
        }

        @Nullable
        public ResourceLocation getTexture() {
            return texture;
        }

        @Override
        public void renderWidget(PoseStack matrices, int mouseX, int mouseY, float delta) {
            ResourceLocation texture = getTexture();
            if(texture == null) {
                GuiComponent.fill(matrices, getX(), getY(), getX() + width, getY() + height, 0xFFFFFF);
            } else {
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, texture);
                int vOffset = this.v;
                if (!this.isActive()) {
                    vOffset += this.hoveredVOffset * 2;
                } else if (this.isHoveredOrFocused()) {
                    vOffset += this.hoveredVOffset;
                }
                RenderSystem.enableDepthTest();
                GuiComponent.blit(matrices, this.getX(), this.getY(), this.u, vOffset, this.width, this.height, this.textureWidth, this.textureHeight);
            }
        }

        @Override
        public void updateHoveredState(int mouseX, int mouseY) {
            this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
        }
    }

    private interface CustomHoverState {
        void updateHoveredState(int mouseX, int mouseY);
    }
}

package io.github.lgatodu47.screenshot_viewer.screens;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfig;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfigListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Optional;
import java.util.function.BiFunction;

public class ManageScreenshotsScreen extends Screen implements ScreenshotViewerConfigListener {
    // Package-private config instance accessible in all the package classes
    static final ScreenshotViewerConfig CONFIG = ScreenshotViewer.getInstance().getConfig();
    static final Logger LOGGER = LogManager.getLogger();

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
        Button configButton = new ExtendedTexturedButtonWidget(2, 2, btnSize, btnSize, 0, 0, btnSize, CONFIG_BUTTON_TEXTURE, 32, 64, button -> {
            configScreenFactory.ifPresent(f -> minecraft.setScreen(f.apply(minecraft, this)));
        }, (button, matrices, x, y) -> {
            renderTooltip(matrices, minecraft.font.split(ScreenshotViewer.translatable("screen", configScreenFactory.isPresent() ? "button.config" : "no_config"), Math.max(width / 2 - 43, 170)), x, y + btnSize);
        }, ScreenshotViewer.translatable("screen", configScreenFactory.isPresent() ? "button.config" : "no_config"));
        configButton.active = configScreenFactory.isPresent();
        addButton(configButton);
        // Order Button
        addButton(new ExtendedTexturedButtonWidget(spacing, btnY, btnSize, btnSize, 0, 0, btnSize, null, 32, 64, button -> {
            if(list != null) {
                list.invertOrder();
            }
        }, (button, matrices, x, y) -> {
            if(list != null) {
                renderTooltip(matrices, minecraft.font.split(ScreenshotViewer.translatable("screen", list.isInvertedOrder() ? "button.order.descending" : "button.order.ascending"), Math.max(width / 2 - 43, 170)), x, y);
            }
        }, ScreenshotViewer.translatable("screen", "button.order")) {
            @Override
            public @Nullable ResourceLocation getTexture() {
                return list == null ? null : list.isInvertedOrder() ? DESCENDING_ORDER_BUTTON_TEXTURE : ASCENDING_ORDER_BUTTON_TEXTURE;
            }
        });
        // Screenshot Folder Button
        addButton(new ExtendedTexturedButtonWidget(spacing * 2 + btnSize, btnY, btnSize, btnSize, 0, 0, btnSize, OPEN_FOLDER_BUTTON_TEXTURE, 32, 64, btn -> {
            Util.getPlatform().openFile(new File(this.minecraft.gameDirectory, "screenshots"));
        }, (button, matrices, x, y) -> {
            renderTooltip(matrices, minecraft.font.split(ScreenshotViewer.translatable("screen", "button.screenshot_folder"), Math.max(width / 2 - 43, 170)), x, y);
        }, ScreenshotViewer.translatable("screen", "button.screenshot_folder")));
        // Done Button
        addButton(new ExtendedButtonWidget((width - bigBtnWidth) / 2, btnY, bigBtnWidth, btnHeight, DialogTexts.GUI_DONE, button -> onClose()));
        // Refresh Button
        addButton(new ExtendedTexturedButtonWidget(width - spacing - btnSize, btnY, btnSize, btnSize, 0, 0, btnSize, REFRESH_BUTTON_TEXTURE, 32, 64, button -> {
            list.init();
        }, (btn, matrices, x, y) -> {
            renderTooltip(matrices, minecraft.font.split(ScreenshotViewer.translatable("screen", "button.refresh"), Math.max(width / 2 - 43, 170)), x, y);
        }, ScreenshotViewer.translatable("screen", "button.refresh")));
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
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        if(list != null) {
            list.render(matrices, mouseX, mouseY, delta, !(enlargedScreenshot.renders() || screenshotProperties.renders()));
        }
        drawCenteredString(matrices, font, title,width / 2, 8, 0xFFFFFF);
        ITextComponent text = ScreenshotViewer.translatable("screen", "screenshot_manager.zoom");
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
                for (IGuiEventListener element : this.children()) {
                    if (element instanceof CustomHoverState) {
                        ((CustomHoverState) element).updateHoveredState(mouseX, mouseY);
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
    public Optional<IGuiEventListener> getChildAt(double mouseX, double mouseY) {
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
        ExtendedButtonWidget(int x, int y, int width, int height, ITextComponent message, IPressable onPress) {
            super(x, y, width, height, message, onPress);
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
            this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
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

        ExtendedTexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, @Nullable ResourceLocation texture, int textureWidth, int textureHeight, IPressable pressAction, ITooltip tooltipSupplier, ITextComponent text) {
            super(x, y, width, height, u, v, hoveredVOffset, Button.WIDGETS_LOCATION, textureWidth, textureHeight, pressAction, tooltipSupplier, text);
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.u = u;
            this.v = v;
            this.hoveredVOffset = hoveredVOffset;
            this.texture = texture;
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            if (!this.visible) {
                return;
            }
            this.renderButton(matrices, mouseX, mouseY, delta);
        }

        @Nullable
        public ResourceLocation getTexture() {
            return texture;
        }

        @Override
        public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            ResourceLocation texture = getTexture();
            if(texture == null) {
                fill(matrices, x, y, x + width, y + height, 0xFFFFFF);
            } else {
                Minecraft.getInstance().getTextureManager().bind(texture);
                int vOffset = this.v;
                if (!active) {
                    vOffset += this.hoveredVOffset * 2;
                } else if (this.isHovered()) {
                    vOffset += this.hoveredVOffset;
                }
                RenderSystem.enableDepthTest();
                blit(matrices, this.x, this.y, this.u, vOffset, this.width, this.height, this.textureWidth, this.textureHeight);
                if (this.isHovered) {
                    this.renderToolTip(matrices, mouseX, mouseY);
                }
            }
        }

        @Override
        public void updateHoveredState(int mouseX, int mouseY) {
            this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        }
    }

    private interface CustomHoverState {
        void updateHoveredState(int mouseX, int mouseY);
    }
}

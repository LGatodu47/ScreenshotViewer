package io.github.lgatodu47.screenshot_viewer.screens.manage_screenshots;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfig;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfigListener;
import io.github.lgatodu47.screenshot_viewer.screens.IconButtonWidget;
import io.github.lgatodu47.screenshot_viewer.screens.ScreenshotViewerTexts;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ManageScreenshotsScreen extends Screen implements ScreenshotViewerConfigListener {
    // Package-private config instance accessible in all the package classes
    static final ScreenshotViewerConfig CONFIG = ScreenshotViewer.getInstance().getConfig();
    static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation CONFIG_ICON = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/sprites/widget/icons/config.png");
    private static final ResourceLocation REFRESH_ICON = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/sprites/widget/icons/refresh.png");
    private static final ResourceLocation ASCENDING_ORDER_ICON = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/sprites/widget/icons/ascending_order.png");
    private static final ResourceLocation DESCENDING_ORDER_ICON = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/sprites/widget/icons/descending_order.png");
    private static final ResourceLocation OPEN_FOLDER_ICON = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/sprites/widget/icons/open_folder.png");
    private static final ResourceLocation FAST_DELETE_ICON = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/sprites/widget/icons/delete.png");
    private static final ResourceLocation FAST_DELETE_ENABLED_ICON = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/sprites/widget/icons/fast_delete_enabled.png");

    private final Screen parent;
    private final EnlargedScreenshotScreen enlargedScreenshot;
    private final ScreenshotPropertiesMenu screenshotProperties;
    private ScreenshotList list;
    private boolean fastDelete;
    @Nullable
    private Screen dialogScreen;
    @Nullable
    private File enlargedScreenshotFile;
    private boolean enlargeAnimation;

    public ManageScreenshotsScreen(Screen parent) {
        super(ScreenshotViewerTexts.MANAGE_SCREENSHOTS);
        this.parent = parent;
        this.enlargedScreenshot = new EnlargedScreenshotScreen(this::showScreenshotProperties);
        this.screenshotProperties = new ScreenshotPropertiesMenu(this::client);
        this.enlargeAnimation = CONFIG.enableScreenshotEnlargementAnimation.get();
        ScreenshotViewer.getInstance().registerConfigListener(this);
    }

    public ManageScreenshotsScreen(Screen parent, @Nullable File enlargedScreenshotFile) {
        this(parent);
        this.enlargedScreenshotFile = enlargedScreenshotFile;
    }

    Minecraft client() {
        return minecraft;
    }

    public boolean isFastDeleteToggled() {
        return fastDelete;
    }

    /// Basic Screen implementations ///

    @Override
    public void tick() {
        if(dialogScreen != null) {
            dialogScreen.tick();
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
            list.updateChildren(false);
        }
        // Adds it to the 'children' list which makes 'mouseClicked' and other methods work with it.
        addWidget(list);

        // Button stuff
        final int btnY = height - spacing - btnHeight;
        final int btnSize = 20;
        final int bigBtnWidth = 200;

        // Config Button
        Optional<BiFunction<Minecraft, Screen, Screen>> configScreenFactory = ScreenshotViewer.getInstance().getConfigScreenFactory();
        Button configButton = new ExtendedTexturedButtonWidget(2, 2, btnSize, btnSize, CONFIG_ICON,
                button -> configScreenFactory.ifPresent(f -> minecraft.setScreen(f.apply(minecraft, this))),
                configScreenFactory.isPresent() ? ScreenshotViewerTexts.CONFIG : ScreenshotViewerTexts.NO_CONFIG,
                configScreenFactory.isPresent() ? ScreenshotViewerTexts.CONFIG : ScreenshotViewerTexts.NO_CONFIG
        ).offsetTooltip();
        configButton.active = configScreenFactory.isPresent();
        addRenderableWidget(configButton);
        // Order Button
        addRenderableWidget(new ExtendedTexturedButtonWidget(spacing, btnY, btnSize, btnSize, null, button -> {
            if(list != null) {
                list.invertOrder();
            }
        }, null, ScreenshotViewerTexts.ORDER) {
            @Override
            protected @Nullable Component getTooltipText() {
                return list == null ? null : list.isInvertedOrder() ? ScreenshotViewerTexts.DESCENDING_ORDER : ScreenshotViewerTexts.ASCENDING_ORDER;
            }

            @Override
            public @Nullable ResourceLocation getIconTexture() {
                return list == null ? null : list.isInvertedOrder() ? DESCENDING_ORDER_ICON : ASCENDING_ORDER_ICON;
            }
        });
        // Screenshot Folder Button
        addRenderableWidget(new ExtendedTexturedButtonWidget(spacing * 2 + btnSize, btnY, btnSize, btnSize, OPEN_FOLDER_ICON, btn -> {
            Util.getPlatform().openFile(new File(CONFIG.screenshotsFolder.get()));
        }, ScreenshotViewerTexts.OPEN_FOLDER, ScreenshotViewerTexts.OPEN_FOLDER));
        // Done Button
        addRenderableWidget(new ExtendedButtonWidget((width - bigBtnWidth) / 2, btnY, bigBtnWidth, btnHeight, CommonComponents.GUI_DONE, button -> {
            List<ScreenshotWidget> toDelete = list.deletionList();
            if(fastDelete && !toDelete.isEmpty()) {
                if(CONFIG.promptWhenDeletingScreenshot.get()) {
                    setDialogScreen(new ConfirmDeletionScreen(value -> {
                                if(value) {
                                    toDelete.forEach(ScreenshotWidget::deleteScreenshot);
                                    this.fastDelete = false;
                                }
                                setDialogScreen(null);
                            }, Component.translatable("screen." + ScreenshotViewer.MODID + ".screenshot_manager.delete_n_screenshots", toDelete.size()),
                                    toDelete.size() == 1 ? ScreenshotViewerTexts.DELETE_WARNING_MESSAGE : ScreenshotViewerTexts.DELETE_MULTIPLE_WARNING_MESSAGE)
                    );
                } else {
                    toDelete.forEach(ScreenshotWidget::deleteScreenshot);
                    this.fastDelete = false;
                }
                return;
            }
            onClose();
        }) {
            @Override
            public @NotNull Component getMessage() {
                List<ScreenshotWidget> toDelete = list.deletionList();
                if(fastDelete && !toDelete.isEmpty()) {
                    return Component.translatable("screen." + ScreenshotViewer.MODID + ".screenshot_manager.delete_n_screenshots", toDelete.size()).withStyle(ChatFormatting.RED);
                }
                return super.getMessage();
            }
        });
        // Fast Delete Button
        addRenderableWidget(new ExtendedTexturedButtonWidget(width - spacing * 2 - btnSize * 2, btnY, btnSize, btnSize, null, button -> {
            this.fastDelete = !fastDelete;
            if(!fastDelete) {
                list.resetDeleteSelection();
            }
        }, ScreenshotViewerTexts.FAST_DELETE, ScreenshotViewerTexts.FAST_DELETE) {
            @Override
            public ResourceLocation getIconTexture() {
                return fastDelete ? FAST_DELETE_ENABLED_ICON : FAST_DELETE_ICON;
            }
        });
        // Refresh Button
        addRenderableWidget(new ExtendedTexturedButtonWidget(width - spacing - btnSize, btnY, btnSize, btnSize, REFRESH_ICON, button -> {
            list.init();
        }, ScreenshotViewerTexts.REFRESH, ScreenshotViewerTexts.REFRESH));

        if(enlargedScreenshotFile != null) {
            list.findByFileName(enlargedScreenshotFile).ifPresentOrElse(this::enlargeScreenshot, () -> LOGGER.warn("Tried to enlarge screenshot with a path '{}' that could not be located in the screenshots folder!", enlargedScreenshotFile.getAbsolutePath()));
            enlargedScreenshotFile = null;
        }
    }

    @Override
    public void resize(@NotNull Minecraft client, int width, int height) {
        super.resize(client, width, height);
        // Adapts the size of the enlarged screenshot when resized
        this.enlargedScreenshot.resize(client, width, height);
        if(dialogScreen != null) {
            this.dialogScreen.resize(client, width, height);
        }
        // Hides the screenshot properties menu
        this.screenshotProperties.hide();
    }

    private float screenshotScaleAnimation;

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        if(list != null) {
            list.render(graphics, mouseX, mouseY, delta, !(enlargedScreenshot.renders() || screenshotProperties.renders()) && dialogScreen == null);
        }
        graphics.drawCenteredString(font, title,width / 2, 8, 0xFFFFFF);
        renderActionText(graphics);
        ScreenshotViewerUtils.forEachDrawable(this, drawable -> drawable.render(graphics, mouseX, mouseY, delta));
        screenshotProperties.render(graphics, mouseX, mouseY, delta);
        PoseStack pose = graphics.pose();
        if(enlargedScreenshot.renders()) {
            float animationTime = 1;

            if(enlargeAnimation) {
                if(screenshotScaleAnimation < 1f) {
                    animationTime = (float) (1 - Math.pow(1 - (screenshotScaleAnimation += 0.03F), 3));
                }
            }

            pose.pushPose();
            pose.translate(0, 0, 1);
            enlargedScreenshot.renderBackground(graphics);
            pose.pushPose();
            pose.translate((enlargedScreenshot.width / 2f) * (1 - animationTime), (enlargedScreenshot.height / 2f) * (1 - animationTime), 0);
            pose.scale(animationTime, animationTime, animationTime);
            enlargedScreenshot.renderImage(graphics);
            pose.popPose();
            enlargedScreenshot.render(graphics, mouseX, mouseY, delta, !screenshotProperties.renders() && dialogScreen == null);
            pose.popPose();
        } else {
            if(screenshotScaleAnimation > 0) {
                screenshotScaleAnimation = 0;
            }

            if(!screenshotProperties.renders() && dialogScreen == null) {
                for (GuiEventListener element : this.children()) {
                    if (element instanceof CustomHoverState hover) {
                        hover.updateHoveredState(mouseX, mouseY);
                    }
                }
            }
        }
        if(dialogScreen != null) {
            pose.pushPose();
            pose.translate(0, 0, 5);
            dialogScreen.render(graphics, mouseX, mouseY, delta);
            pose.popPose();
        } else {
            pose.pushPose();
            pose.translate(0, 0, 2);
            screenshotProperties.render(graphics, mouseX, mouseY, delta);
            pose.popPose();
        }
    }

    private void renderActionText(GuiGraphics context) {
        Component text = fastDelete ? ScreenshotViewerTexts.FAST_DELETE_MODE : ScreenshotViewerTexts.ZOOM_MODE;
        context.drawString(font, text, width - font.width(text) - 8, 8, fastDelete ? 0xEB4034 : isCtrlDown ? 0x18DE39 : 0xF0CA22);
    }

    /// Methods shared between the classes of the package ///

    void enlargeScreenshot(@Nullable ScreenshotImageHolder showing) {
        if(showing == null) {
            this.enlargedScreenshot.onClose();
        }
        this.enlargedScreenshot.show(showing, list);
    }

    void showScreenshotProperties(double mouseX, double mouseY, ScreenshotImageHolder widget) {
        if(list == null) {
            return;
        }
        this.screenshotProperties.show((int) mouseX, (int) mouseY, width, height, widget);
    }

    void setDialogScreen(Screen screen) {
        if(minecraft == null) {
            return;
        }
        this.dialogScreen = screen;
        if(dialogScreen != null) {
            this.dialogScreen.init(minecraft, width, height);
        }
    }

    /// Input handling methods below ///

    private boolean isCtrlDown;

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(dialogScreen != null) {
            return dialogScreen.keyPressed(keyCode, scanCode, modifiers);
        }
        if(screenshotProperties.renders()) {
            return screenshotProperties.keyPressed(keyCode, scanCode, modifiers);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.keyPressed(keyCode, scanCode, modifiers);
        }
        isCtrlDown = keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL;
        if(keyCode == GLFW.GLFW_KEY_F5) {
            list.init();
            return true;
        }
        if(list != null) {
            if(list.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if(dialogScreen != null) {
            return dialogScreen.keyReleased(keyCode, scanCode, modifiers);
        }
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
        if(dialogScreen != null) {
            return dialogScreen.charTyped(chr, modifiers);
        }
        if(screenshotProperties.renders()) {
            return screenshotProperties.charTyped(chr, modifiers);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double verticalMovement) {
        if(dialogScreen != null) {
            return dialogScreen.mouseScrolled(mouseX, mouseY, verticalMovement);
        }
        if(screenshotProperties.renders()) {
            return screenshotProperties.mouseScrolled(mouseX, mouseY, verticalMovement);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.mouseScrolled(mouseX, mouseY, verticalMovement);
        }
        if(list != null) {
            if(isCtrlDown) {
                list.updateScreenshotsPerRow(verticalMovement);
                return true;
            }

            return list.mouseScrolled(mouseX, mouseY, verticalMovement);
        }
        return super.mouseScrolled(mouseX, mouseY, verticalMovement);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(dialogScreen != null) {
            return dialogScreen.mouseClicked(mouseX, mouseY, button);
        }
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
        if(dialogScreen != null) {
            return dialogScreen.mouseReleased(mouseX, mouseY, button);
        }
        if(screenshotProperties.renders()) {
            return screenshotProperties.mouseReleased(mouseX, mouseY, button);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.mouseReleased(mouseX, mouseY, button);
        }
        if(list != null) {
            return list.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if(dialogScreen != null) {
            return dialogScreen.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        if(screenshotProperties.renders()) {
            return screenshotProperties.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public @NotNull Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        if(dialogScreen != null) {
            return dialogScreen.getChildAt(mouseX, mouseY);
        }
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
        this.enlargeAnimation = CONFIG.enableScreenshotEnlargementAnimation.get();
    }

    public static class ExtendedButtonWidget extends Button implements CustomHoverState {
        ExtendedButtonWidget(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, Supplier::get);
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            if (!this.visible) {
                return;
            }
            super.renderWidget(graphics, mouseX, mouseY, delta);
        }

        @Override
        public boolean isHoveredOrFocused() {
            return isHovered();
        }

        @Override
        public void updateHoveredState(int mouseX, int mouseY) {
            this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
        }
    }

    public static class ExtendedTexturedButtonWidget extends IconButtonWidget implements CustomHoverState {
        @Nullable
        private final Component tooltip;
        private boolean offsetTooltip;

        public ExtendedTexturedButtonWidget(int x, int y, int width, int height, @Nullable ResourceLocation texture, OnPress pressAction, @Nullable Component tooltip, Component text) {
            super(x, y, width, height, text, texture, pressAction);
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
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            if (!this.visible) {
                return;
            }
            this.renderWidget(graphics, mouseX, mouseY, delta);
            updateTooltip();
        }

        private void updateTooltip() {
            Component tooltip = getTooltipText();
            if (tooltip != null) {
                if (isHovered()) {
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
        protected @NotNull ClientTooltipPositioner createTooltipPositioner() {
            ClientTooltipPositioner positioner = DefaultTooltipPositioner.INSTANCE;
            return offsetTooltip ? (screen_width, screen_height, x, y, w, h) -> positioner.positionTooltip(screen_width, screen_height, x, y + height, w, h) : positioner;
        }

        @Override
        public boolean isHoveredOrFocused() {
            return isHovered();
        }

        @Override
        public void updateHoveredState(int mouseX, int mouseY) {
            this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
        }
    }

    public interface CustomHoverState {
        void updateHoveredState(int mouseX, int mouseY);
    }
}

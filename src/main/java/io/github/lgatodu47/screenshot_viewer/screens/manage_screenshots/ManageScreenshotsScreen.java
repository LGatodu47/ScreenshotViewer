package io.github.lgatodu47.screenshot_viewer.screens.manage_screenshots;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import io.github.lgatodu47.screenshot_viewer.ScreenshotThumbnailManager;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfig;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfigListener;
import io.github.lgatodu47.screenshot_viewer.screens.IconButtonWidget;
import io.github.lgatodu47.screenshot_viewer.screens.ScreenshotViewerTexts;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ManageScreenshotsScreen extends Screen implements ScreenshotViewerConfigListener, OldParentElementMethods {
    // Package-private config instance accessible in all the package classes
    static final ScreenshotViewerConfig CONFIG = ScreenshotViewer.getInstance().getConfig();
    static final ScreenshotThumbnailManager THUMBNAILS = ScreenshotViewer.getInstance().getThumbnailManager();
    static final Logger LOGGER = LogUtils.getLogger();

    private static final Identifier CONFIG_ICON = Identifier.fromNamespaceAndPath(ScreenshotViewer.MODID, "widget/icons/config");
    private static final Identifier REFRESH_ICON = Identifier.fromNamespaceAndPath(ScreenshotViewer.MODID, "widget/icons/refresh");
    private static final Identifier ASCENDING_ORDER_ICON = Identifier.fromNamespaceAndPath(ScreenshotViewer.MODID, "widget/icons/ascending_order");
    private static final Identifier DESCENDING_ORDER_ICON = Identifier.fromNamespaceAndPath(ScreenshotViewer.MODID, "widget/icons/descending_order");
    private static final Identifier OPEN_FOLDER_ICON = Identifier.fromNamespaceAndPath(ScreenshotViewer.MODID, "widget/icons/open_folder");
    private static final Identifier FAST_DELETE_ICON = Identifier.fromNamespaceAndPath(ScreenshotViewer.MODID, "widget/icons/delete");
    private static final Identifier FAST_DELETE_ENABLED_ICON = Identifier.fromNamespaceAndPath(ScreenshotViewer.MODID, "widget/icons/fast_delete_enabled");

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
        final int spacing = 8;
        final int btnHeight = 20;

        this.enlargedScreenshot.init(width, height);

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
        Optional<IConfigScreenFactory> configScreenFactory = ScreenshotViewer.getInstance().getConfigScreenFactory();
        Button configButton = new ExtendedTexturedButtonWidget(2, 2, btnSize, btnSize, CONFIG_ICON,
                button -> configScreenFactory.ifPresent(f -> minecraft.setScreen(f.createScreen(ScreenshotViewer.getInstance().getModContainer(), this))),
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
            public @Nullable Identifier getIconTexture() {
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
                                } else {
                                    list.resetDeleteSelection();
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
            public Identifier getIconTexture() {
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
    public void resize(int width, int height) {
        super.resize(width, height);
        // Adapts the size of the enlarged screenshot when resized
        this.enlargedScreenshot.resize(width, height);
        if(dialogScreen != null) {
            this.dialogScreen.resize(width, height);
        }
        // Hides the screenshot properties menu
        this.screenshotProperties.hide();
    }

    private float screenshotScaleAnimation;

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if(list != null) {
            list.render(graphics, mouseX, mouseY, delta, !(enlargedScreenshot.renders() || screenshotProperties.renders()) && dialogScreen == null);
        }
        graphics.centeredText(font, title,width / 2, 8, 0xFFFFFFFF);
        renderActionText(graphics);
        ScreenshotViewerUtils.forEachDrawable(this, drawable -> drawable.extractRenderState(graphics, mouseX, mouseY, delta));

        Matrix3x2fStack matrices = graphics.pose();
        if(enlargedScreenshot.renders()) {
            float animationTime = 1;

            if(enlargeAnimation) {
                if(screenshotScaleAnimation < 1f) {
                    animationTime = (float) (1 - Math.pow(1 - (screenshotScaleAnimation += 0.03F), 3));
                }
            }

            enlargedScreenshot.extractBackground(graphics, mouseX, mouseY, delta);
            matrices.pushMatrix();
            matrices.translate((enlargedScreenshot.width / 2f) * (1 - animationTime), (enlargedScreenshot.height / 2f) * (1 - animationTime));
            matrices.scale(animationTime, animationTime);
            enlargedScreenshot.renderImage(graphics);
            matrices.popMatrix();
            enlargedScreenshot.render(graphics, mouseX, mouseY, delta, !screenshotProperties.renders() && dialogScreen == null);
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
            dialogScreen.extractRenderState(graphics, mouseX, mouseY, delta);
        } else {
            screenshotProperties.extractRenderState(graphics, mouseX, mouseY, delta);
        }
    }

    private void renderActionText(GuiGraphicsExtractor context) {
        Component text = fastDelete ? ScreenshotViewerTexts.FAST_DELETE_MODE : ScreenshotViewerTexts.ZOOM_MODE;
        context.text(font, text, width - font.width(text) - 8, 8, fastDelete ? 0xFFEB4034 : isCtrlDown ? 0xFF18DE39 : 0xFFF0CA22);
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
        this.dialogScreen = screen;
        if(dialogScreen != null) {
            this.dialogScreen.init(width, height);
        }
    }

    /// Input handling methods below ///

    private boolean isCtrlDown;

    @Override
    public boolean keyPressed(@NonNull KeyEvent input) {
        if(dialogScreen != null) {
            return dialogScreen.keyPressed(input);
        }
        if(screenshotProperties.renders()) {
            return screenshotProperties.keyPressed(input);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.keyPressed(input);
        }
        isCtrlDown = input.key() == InputConstants.KEY_LCONTROL || input.key() == InputConstants.KEY_RCONTROL;
        if (input.key() == InputConstants.KEY_F5) {
            list.init();
            return true;
        }
        if(list != null) {
            if(list.keyPressed(input)) {
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(@NonNull KeyEvent input) {
        if(dialogScreen != null) {
            return dialogScreen.keyReleased(input);
        }
        if(screenshotProperties.renders()) {
            return screenshotProperties.keyReleased(input);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.keyReleased(input);
        }
        if(isCtrlDown) {
            isCtrlDown = false;
        }
        return super.keyReleased(input);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent input) {
        if(dialogScreen != null) {
            return dialogScreen.charTyped(input);
        }
        if(screenshotProperties.renders()) {
            return screenshotProperties.charTyped(input);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.charTyped(input);
        }
        return super.charTyped(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalMovement, double verticalMovement) {
        if(dialogScreen != null) {
            return dialogScreen.mouseScrolled(mouseX, mouseY, horizontalMovement, verticalMovement);
        }
        if(screenshotProperties.renders()) {
            return screenshotProperties.mouseScrolled(mouseX, mouseY, horizontalMovement, verticalMovement);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.mouseScrolled(mouseX, mouseY, horizontalMovement, verticalMovement);
        }
        if(list != null) {
            if(isCtrlDown) {
                list.updateScreenshotsPerRow(verticalMovement);
                return true;
            }

            return list.mouseScrolled(mouseX, mouseY, horizontalMovement, verticalMovement);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalMovement, verticalMovement);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubled) {
        if(dialogScreen != null) {
            return dialogScreen.mouseClicked(click, doubled);
        }
        if(screenshotProperties.renders()) {
            return screenshotProperties.mouseClicked(click, doubled);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.mouseClicked(click, doubled);
        }
        return OldParentElementMethods.super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent click) {
        if(dialogScreen != null) {
            return dialogScreen.mouseReleased(click);
        }
        if(screenshotProperties.renders()) {
            return screenshotProperties.mouseReleased(click);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.mouseReleased(click);
        }
        if(list != null) {
            return list.mouseReleased(click);
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent click, double offsetX, double offsetY) {
        if(dialogScreen != null) {
            return dialogScreen.mouseDragged(click, offsetX, offsetY);
        }
        if(screenshotProperties.renders()) {
            return screenshotProperties.mouseDragged(click, offsetX, offsetY);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.mouseDragged(click, offsetX, offsetY);
        }
        return super.mouseDragged(click, offsetX, offsetY);
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
        this.minecraft.setScreen(parent);
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

    public static class ExtendedButtonWidget extends Button.Plain implements CustomHoverState {
        ExtendedButtonWidget(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, Supplier::get);
        }

        @Override
        public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            if (!this.visible) {
                return;
            }
            super.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
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

        public ExtendedTexturedButtonWidget(int x, int y, int width, int height, @Nullable Identifier texture, OnPress pressAction, @Nullable Component tooltip, Component text) {
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
        public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            if (!this.visible) {
                return;
            }
            this.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
            applyTooltip(graphics, mouseX, mouseY);
        }

        private void applyTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
            Component tooltipText = getTooltipText();
            if (tooltipText != null && isHovered()) {
                graphics.setTooltipForNextFrame(Minecraft.getInstance().font, List.of(tooltipText.getVisualOrderText()), getTooltipPositioner(), mouseX, mouseY, isFocused());
            }
        }

        @Nullable
        protected Component getTooltipText() {
            return tooltip;
        }

        protected ClientTooltipPositioner getTooltipPositioner() {
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

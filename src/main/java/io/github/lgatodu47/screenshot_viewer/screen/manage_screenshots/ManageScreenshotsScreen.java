package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import com.mojang.logging.LogUtils;
import io.github.lgatodu47.catconfig.CatConfig;
import io.github.lgatodu47.catconfigmc.screen.ConfigListener;
import io.github.lgatodu47.screenshot_viewer.ScreenshotThumbnailManager;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions;
import io.github.lgatodu47.screenshot_viewer.screen.IconButtonWidget;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerConfigScreen;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.components.Button;
import org.joml.Matrix3x2fStack;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ManageScreenshotsScreen extends Screen implements ConfigListener, OldParentElementMethods {
    // Package-private config instance accessible in all the package classes
    static final CatConfig CONFIG = ScreenshotViewer.getInstance().getConfig();
    static final ScreenshotThumbnailManager THUMBNAILS = ScreenshotViewer.getInstance().getThumbnailManager();
    static final Logger LOGGER = LogUtils.getLogger();

    public static final WidgetSprites DEFAULT_BUTTON_TEXTURES = new WidgetSprites(Identifier.withDefaultNamespace("widget/button"), Identifier.withDefaultNamespace("widget/button_disabled"), Identifier.withDefaultNamespace("widget/button_highlighted"));
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
        this.enlargeAnimation = CONFIG.getOrFallback(ScreenshotViewerOptions.ENABLE_SCREENSHOT_ENLARGEMENT_ANIMATION, true);
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
        addRenderableWidget(new ExtendedTexturedButtonWidget(2, 2, btnSize, btnSize, CONFIG_ICON, button -> {
            minecraft.gui.setScreen(new ScreenshotViewerConfigScreen(this));
        }, ScreenshotViewerTexts.CONFIG, ScreenshotViewerTexts.CONFIG).offsetTooltip());
        // Order Button
        addRenderableWidget(new ExtendedTexturedButtonWidget(spacing, btnY, btnSize, btnSize, null, button -> {
            if(list != null) {
                list.invertOrder();
            }
        }, null, ScreenshotViewerTexts.ORDER) {
            @Override
            protected @Nullable net.minecraft.network.chat.Component getTooltipText() {
                return list == null ? null : list.isInvertedOrder() ? ScreenshotViewerTexts.DESCENDING_ORDER : ScreenshotViewerTexts.ASCENDING_ORDER;
            }

            @Override
            public @Nullable Identifier getIconTexture() {
                return list == null ? null : list.isInvertedOrder() ? DESCENDING_ORDER_ICON : ASCENDING_ORDER_ICON;
            }
        });
        // Screenshot Folder Button
        addRenderableWidget(new ExtendedTexturedButtonWidget(spacing * 2 + btnSize, btnY, btnSize, btnSize, OPEN_FOLDER_ICON, btn -> {
            Util.getPlatform().openFile(CONFIG.getOrFallback(ScreenshotViewerOptions.SCREENSHOTS_FOLDER, (Supplier<? extends File>) ScreenshotViewerUtils::getVanillaScreenshotsFolder));
        }, ScreenshotViewerTexts.OPEN_FOLDER, ScreenshotViewerTexts.OPEN_FOLDER));
        // Done/Delete n screenshots Button
        addRenderableWidget(new ExtendedButtonWidget((width - bigBtnWidth) / 2, btnY, bigBtnWidth, btnHeight, CommonComponents.GUI_DONE, button -> {
            List<ScreenshotWidget> toDelete = list.deletionList();
            if(fastDelete && !toDelete.isEmpty()) {
                if(CONFIG.getOrFallback(ScreenshotViewerOptions.PROMPT_WHEN_DELETING_SCREENSHOT, true)) {
                    setDialogScreen(new ConfirmDeletionScreen(value -> {
                        if(value) {
                            toDelete.forEach(ScreenshotWidget::deleteScreenshot);
                        } else {
                            list.resetDeleteSelection();
                        }
                        setDialogScreen(null);
                        }, Component.translatable("screen." + ScreenshotViewer.MODID + ".screenshot_manager.delete_n_screenshots", toDelete.size()),
                            toDelete.size() == 1 ? ScreenshotViewerTexts.DELETE_WARNING_MESSAGE : ScreenshotViewerTexts.DELETE_MULTIPLE_WARNING_MESSAGE)
                    );
                } else {
                    toDelete.forEach(ScreenshotWidget::deleteScreenshot);
                }
                this.fastDelete = false;
                return;
            }
            onClose();
        }) {
            @Override
            public net.minecraft.network.chat.Component getMessage() {
                List<ScreenshotWidget> toDelete = list.deletionList();
                if(fastDelete && !toDelete.isEmpty()) {
                    return net.minecraft.network.chat.Component.translatable("screen." + ScreenshotViewer.MODID + ".screenshot_manager.delete_n_screenshots", toDelete.size()).withStyle(ChatFormatting.RED);
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
        // Adapts the size of the enlarged screenshot and the dialog screen when resized
        this.enlargedScreenshot.resize(width, height);
        if(dialogScreen != null) {
            this.dialogScreen.resize(width, height);
        }
        // Hides the screenshot properties menu
        this.screenshotProperties.hide();
    }

    private float screenshotScaleAnimation;

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if(list != null) {
            list.render(context, mouseX, mouseY, delta, !(enlargedScreenshot.renders() || screenshotProperties.renders()) && dialogScreen == null);
        }
        context.centeredText(font, title,width / 2, 8, 0xFFFFFF);
        renderActionText(context);
        ScreenshotViewerUtils.forEachDrawable(this, drawable -> drawable.extractRenderState(context, mouseX, mouseY, delta));

        Matrix3x2fStack matrices = context.pose();
        if(enlargedScreenshot.renders()) {
            float animationTime = 1;

            if(enlargeAnimation) {
                if(screenshotScaleAnimation < 1f) {
                    animationTime = (float) (1 - Math.pow(1 - (screenshotScaleAnimation += 0.03F), 3));
                }
            }

            enlargedScreenshot.extractBackground(context, mouseX, mouseY, delta);
            matrices.pushMatrix();
            matrices.translate((enlargedScreenshot.width / 2f) * (1 - animationTime), (enlargedScreenshot.height / 2f) * (1 - animationTime));
            matrices.scale(animationTime, animationTime);
            enlargedScreenshot.renderImage(context);
            matrices.popMatrix();
            enlargedScreenshot.render(context, mouseX, mouseY, delta, !screenshotProperties.renders() && dialogScreen == null);
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
            dialogScreen.extractRenderState(context, mouseX, mouseY, delta);
        } else {
            screenshotProperties.extractRenderState(context, mouseX, mouseY, delta);
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
    public boolean keyPressed(KeyEvent input) {
        if(dialogScreen != null) {
            return dialogScreen.keyPressed(input);
        }
        if(screenshotProperties.renders()) {
            return screenshotProperties.keyPressed(input);
        }
        if(enlargedScreenshot.renders()) {
            return enlargedScreenshot.keyPressed(input);
        }
        isCtrlDown = input.key() == GLFW.GLFW_KEY_LEFT_CONTROL || input.key() == GLFW.GLFW_KEY_RIGHT_CONTROL;
        if (input.key() == GLFW.GLFW_KEY_F5) {
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
    public boolean keyReleased(KeyEvent input) {
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
    public boolean charTyped(CharacterEvent input) {
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
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
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
    public boolean mouseReleased(MouseButtonEvent click) {
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
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
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
    public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
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
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        list.close();
    }

    @Override // From ConfigListener
    public void configUpdated() {
        this.list.onConfigUpdate();
        this.enlargeAnimation = CONFIG.getOrFallback(ScreenshotViewerOptions.ENABLE_SCREENSHOT_ENLARGEMENT_ANIMATION, true);
    }

    public static class ExtendedButtonWidget extends Button.Plain implements CustomHoverState {
        public ExtendedButtonWidget(int x, int y, int width, int height, net.minecraft.network.chat.Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, Supplier::get);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            if (!this.visible) {
                return;
            }
            this.extractWidgetRenderState(context, mouseX, mouseY, delta);
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
        private final net.minecraft.network.chat.Component tooltip;
        private boolean offsetTooltip;

        public ExtendedTexturedButtonWidget(int x, int y, int width, int height, @Nullable Identifier texture, OnPress pressAction, @Nullable net.minecraft.network.chat.Component tooltip, net.minecraft.network.chat.Component text) {
            super(x, y, width, height, text, texture, pressAction);
            this.tooltip = tooltip;
        }

        public ExtendedTexturedButtonWidget offsetTooltip() {
            this.offsetTooltip = true;
            return this;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            if (!this.visible) {
                return;
            }
            this.extractWidgetRenderState(context, mouseX, mouseY, delta);
            applyTooltip(context, mouseX, mouseY);
        }

        private void applyTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
            net.minecraft.network.chat.Component tooltipText = getTooltipText();
            if (tooltipText != null && isHovered()) {
                context.setTooltipForNextFrame(Minecraft.getInstance().font, List.of(tooltipText.getVisualOrderText()), getTooltipPositioner(), mouseX, mouseY, isFocused());
            }
        }

        @Nullable
        protected net.minecraft.network.chat.Component getTooltipText() {
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

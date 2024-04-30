package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import com.mojang.logging.LogUtils;
import io.github.lgatodu47.catconfig.CatConfig;
import io.github.lgatodu47.catconfigmc.screen.ConfigListener;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions;
import io.github.lgatodu47.screenshot_viewer.screen.IconButtonWidget;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerConfigScreen;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ManageScreenshotsScreen extends Screen implements ConfigListener {
    // Package-private config instance accessible in all the package classes
    static final CatConfig CONFIG = ScreenshotViewer.getInstance().getConfig();
    static final Logger LOGGER = LogUtils.getLogger();

    public static final ButtonTextures DEFAULT_BUTTON_TEXTURES = new ButtonTextures(new Identifier("widget/button"), new Identifier("widget/button_disabled"), new Identifier("widget/button_highlighted"));
    private static final Identifier CONFIG_ICON = new Identifier(ScreenshotViewer.MODID, "widget/icons/config");
    private static final Identifier REFRESH_ICON = new Identifier(ScreenshotViewer.MODID, "widget/icons/refresh");
    private static final Identifier ASCENDING_ORDER_ICON = new Identifier(ScreenshotViewer.MODID, "widget/icons/ascending_order");
    private static final Identifier DESCENDING_ORDER_ICON = new Identifier(ScreenshotViewer.MODID, "widget/icons/descending_order");
    private static final Identifier OPEN_FOLDER_ICON = new Identifier(ScreenshotViewer.MODID, "widget/icons/open_folder");
    private static final Identifier FAST_DELETE_ICON = new Identifier(ScreenshotViewer.MODID, "widget/icons/delete");
    private static final Identifier FAST_DELETE_ENABLED_ICON = new Identifier(ScreenshotViewer.MODID, "widget/icons/fast_delete_enabled");

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

    MinecraftClient client() {
        return client;
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
            list.updateChildren(false);
        }
        // Adds it to the 'children' list which makes 'mouseClicked' and other methods work with it.
        addSelectableChild(list);

        // Button stuff
        final int btnY = height - spacing - btnHeight;
        final int btnSize = 20;
        final int bigBtnWidth = 200;

        // Config Button
        addDrawableChild(new ExtendedTexturedButtonWidget(2, 2, btnSize, btnSize, CONFIG_ICON, button -> {
            client.setScreen(new ScreenshotViewerConfigScreen(this));
        }, ScreenshotViewerTexts.CONFIG, ScreenshotViewerTexts.CONFIG).offsetTooltip());
        // Order Button
        addDrawableChild(new ExtendedTexturedButtonWidget(spacing, btnY, btnSize, btnSize, null, button -> {
            if(list != null) {
                list.invertOrder();
            }
        }, null, ScreenshotViewerTexts.ORDER) {
            @Override
            protected @Nullable Text getTooltipText() {
                return list == null ? null : list.isInvertedOrder() ? ScreenshotViewerTexts.DESCENDING_ORDER : ScreenshotViewerTexts.ASCENDING_ORDER;
            }

            @Override
            public @Nullable Identifier getIconTexture() {
                return list == null ? null : list.isInvertedOrder() ? DESCENDING_ORDER_ICON : ASCENDING_ORDER_ICON;
            }
        });
        // Screenshot Folder Button
        addDrawableChild(new ExtendedTexturedButtonWidget(spacing * 2 + btnSize, btnY, btnSize, btnSize, OPEN_FOLDER_ICON, btn -> {
            Util.getOperatingSystem().open(CONFIG.getOrFallback(ScreenshotViewerOptions.SCREENSHOTS_FOLDER, (Supplier<? extends File>) ScreenshotViewerUtils::getVanillaScreenshotsFolder));
        }, ScreenshotViewerTexts.OPEN_FOLDER, ScreenshotViewerTexts.OPEN_FOLDER));
        // Done/Delete n screenshots Button
        addDrawableChild(new ExtendedButtonWidget((width - bigBtnWidth) / 2, btnY, bigBtnWidth, btnHeight, ScreenTexts.DONE, button -> {
            List<ScreenshotWidget> toDelete = list.deletionList();
            if(fastDelete && !toDelete.isEmpty()) {
                if(CONFIG.getOrFallback(ScreenshotViewerOptions.PROMPT_WHEN_DELETING_SCREENSHOT, true)) {
                    setDialogScreen(new ConfirmDeletionScreen(value -> {
                        if(value) {
                            toDelete.forEach(ScreenshotWidget::deleteScreenshot);
                        }
                        setDialogScreen(null);
                        }, Text.translatable("screen." + ScreenshotViewer.MODID + ".screenshot_manager.delete_n_screenshots", toDelete.size()),
                            toDelete.size() == 1 ? ScreenshotViewerTexts.DELETE_WARNING_MESSAGE : ScreenshotViewerTexts.DELETE_MULTIPLE_WARNING_MESSAGE)
                    );
                } else {
                    toDelete.forEach(ScreenshotWidget::deleteScreenshot);
                }
                this.fastDelete = false;
                return;
            }
            close();
        }) {
            @Override
            public Text getMessage() {
                List<ScreenshotWidget> toDelete = list.deletionList();
                if(fastDelete && !toDelete.isEmpty()) {
                    return Text.translatable("screen." + ScreenshotViewer.MODID + ".screenshot_manager.delete_n_screenshots", toDelete.size()).formatted(Formatting.RED);
                }
                return super.getMessage();
            }
        });
        // Fast Delete Button
        addDrawableChild(new ExtendedTexturedButtonWidget(width - spacing * 2 - btnSize * 2, btnY, btnSize, btnSize, null, button -> {
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
        addDrawableChild(new ExtendedTexturedButtonWidget(width - spacing - btnSize, btnY, btnSize, btnSize, REFRESH_ICON, button -> {
            list.init();
        }, ScreenshotViewerTexts.REFRESH, ScreenshotViewerTexts.REFRESH));

        if(enlargedScreenshotFile != null) {
            list.findByFileName(enlargedScreenshotFile).ifPresentOrElse(this::enlargeScreenshot, () -> LOGGER.warn("Tried to enlarge screenshot with a path '{}' that could not be located in the screenshots folder!", enlargedScreenshotFile.getAbsolutePath()));
            enlargedScreenshotFile = null;
        }
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        // Adapts the size of the enlarged screenshot and the dialog screen when resized
        this.enlargedScreenshot.resize(client, width, height);
        if(dialogScreen != null) {
            this.dialogScreen.resize(client, width, height);
        }
        // Hides the screenshot properties menu
        this.screenshotProperties.hide();
    }

    private float screenshotScaleAnimation;

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        if(list != null) {
            list.render(context, mouseX, mouseY, delta, !(enlargedScreenshot.renders() || screenshotProperties.renders()) && dialogScreen == null);
        }
        context.drawCenteredTextWithShadow(textRenderer, title,width / 2, 8, 0xFFFFFF);
        renderActionText(context);
        ScreenshotViewerUtils.forEachDrawable(this, drawable -> drawable.render(context, mouseX, mouseY, delta));
        MatrixStack matrices = context.getMatrices();
        if(enlargedScreenshot.renders()) {
            float animationTime = 1;

            if(enlargeAnimation) {
                if(screenshotScaleAnimation < 1f) {
                    animationTime = (float) (1 - Math.pow(1 - (screenshotScaleAnimation += 0.03F), 3));
                }
            }

            matrices.push();
            matrices.translate(0, 0, 1);
            enlargedScreenshot.renderBackground(context, mouseX, mouseY, delta);
            matrices.push();
            matrices.translate((enlargedScreenshot.width / 2f) * (1 - animationTime), (enlargedScreenshot.height / 2f) * (1 - animationTime), 0);
            matrices.scale(animationTime, animationTime, animationTime);
            enlargedScreenshot.renderImage(context);
            matrices.pop();
            enlargedScreenshot.render(context, mouseX, mouseY, delta, !screenshotProperties.renders() && dialogScreen == null);
            matrices.pop();
        } else {
            if(screenshotScaleAnimation > 0) {
                screenshotScaleAnimation = 0;
            }

            if(!screenshotProperties.renders() && dialogScreen == null) {
                for (Element element : this.children()) {
                    if (element instanceof CustomHoverState hover) {
                        hover.updateHoveredState(mouseX, mouseY);
                    }
                }
            }
        }
        if(dialogScreen != null) {
            matrices.push();
            matrices.translate(0, 0, 5);
            dialogScreen.render(context, mouseX, mouseY, delta);
            matrices.pop();
        } else {
            matrices.push();
            matrices.translate(0, 0, 2);
            screenshotProperties.render(context, mouseX, mouseY, delta);
            matrices.pop();
        }
    }

    private void renderActionText(DrawContext context) {
        Text text = fastDelete ? ScreenshotViewerTexts.FAST_DELETE_MODE : ScreenshotViewerTexts.ZOOM_MODE;
        context.drawTextWithShadow(textRenderer, text, width - textRenderer.getWidth(text) - 8, 8, fastDelete ? 0xEB4034 : isCtrlDown ? 0x18DE39 : 0xF0CA22);
    }

    /// Methods shared between the classes of the package ///

    void enlargeScreenshot(@Nullable ScreenshotImageHolder showing) {
        if(showing == null) {
            this.enlargedScreenshot.close();
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
        if(client == null) {
            return;
        }
        this.dialogScreen = screen;
        if(dialogScreen != null) {
            this.dialogScreen.init(client, width, height);
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
    public Optional<Element> hoveredElement(double mouseX, double mouseY) {
        if(dialogScreen != null) {
            return dialogScreen.hoveredElement(mouseX, mouseY);
        }
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
        this.enlargeAnimation = CONFIG.getOrFallback(ScreenshotViewerOptions.ENABLE_SCREENSHOT_ENLARGEMENT_ANIMATION, true);
    }

    public static class ExtendedButtonWidget extends ButtonWidget implements CustomHoverState {
        public ExtendedButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
            super(x, y, width, height, message, onPress, Supplier::get);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            if (!this.visible) {
                return;
            }
            this.renderWidget(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean isSelected() {
            return isHovered();
        }

        @Override
        public void updateHoveredState(int mouseX, int mouseY) {
            this.hovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
        }
    }

    public static class ExtendedTexturedButtonWidget extends IconButtonWidget implements CustomHoverState {
        @Nullable
        private final Text tooltip;
        private boolean offsetTooltip;

        public ExtendedTexturedButtonWidget(int x, int y, int width, int height, @Nullable Identifier texture, PressAction pressAction, @Nullable Text tooltip, Text text) {
            super(x, y, width, height, text, texture, pressAction);
            this.tooltip = tooltip;
            if(tooltip != null) {
                setTooltip(Tooltip.of(tooltip));
            }
        }

        public ExtendedTexturedButtonWidget offsetTooltip() {
            this.offsetTooltip = true;
            return this;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            if (!this.visible) {
                return;
            }
            this.renderWidget(context, mouseX, mouseY, delta);
            applyTooltip();
        }

        private void applyTooltip() {
            Text tooltipText = getTooltipText();
            if (tooltipText != null) {
                if (isHovered()) {
                    Screen screen = MinecraftClient.getInstance().currentScreen;
                    if (screen != null) {
                        screen.setTooltip(Tooltip.of(tooltipText), getTooltipPositioner(), isFocused());
                    }
                }
            }
        }

        @Nullable
        protected Text getTooltipText() {
            return tooltip;
        }

        protected TooltipPositioner getTooltipPositioner() {
            TooltipPositioner positioner = HoveredTooltipPositioner.INSTANCE;
            return offsetTooltip ? (screen_width, screen_height, x, y, w, h) -> positioner.getPosition(screen_width, screen_height, x, y + height, w, h) : positioner;
        }

        @Override
        public boolean isSelected() {
            return isHovered();
        }

        @Override
        public void updateHoveredState(int mouseX, int mouseY) {
            this.hovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
        }
    }

    public interface CustomHoverState {
        void updateHoveredState(int mouseX, int mouseY);
    }
}

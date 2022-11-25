package io.github.lgatodu47.screenshot_viewer.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import io.github.lgatodu47.catconfig.CatConfig;
import io.github.lgatodu47.catconfigmc.screen.ConfigListener;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;

public class ManageScreenshotsScreen extends Screen implements ConfigListener {
    private static final CatConfig CONFIG = ScreenshotViewer.getInstance().getConfig();

    private static final Identifier CONFIG_BUTTON_TEXTURE = new Identifier(ScreenshotViewer.MODID, "textures/gui/config_button.png");
    private static final Identifier REFRESH_BUTTON_TEXTURE = new Identifier(ScreenshotViewer.MODID, "textures/gui/refresh_button.png");
    private static final Identifier ASCENDING_ORDER_BUTTON_TEXTURE = new Identifier(ScreenshotViewer.MODID, "textures/gui/ascending_order_button.png");
    private static final Identifier DESCENDING_ORDER_BUTTON_TEXTURE = new Identifier(ScreenshotViewer.MODID, "textures/gui/descending_order_button.png");
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Screen parent;
    private final SelectedScreenshotScreen selectedScreenshot;
    private ScreenshotList list;

    public ManageScreenshotsScreen(Screen parent) {
        super(ScreenshotViewer.translatable("screen", "manage_screenshots"));
        this.parent = parent;
        this.selectedScreenshot = new SelectedScreenshotScreen();
    }

    @Override
    protected void init() {
        if(client == null) {
            return;
        }

        final int spacing = 8;
        final int btnHeight = 20;

        this.selectedScreenshot.init(client, width, height);

        //Main content
        int contentWidth = width - 24;
        int contentHeight = height - spacing * 5 - btnHeight;
        // We avoid creating the list every time we refresh the screen, so we don't have to load the screenshots again
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

        // Buttons
        final int btnY = height - spacing - btnHeight;
        final int refreshBtnSize = 20;
        final int bigBtnWidth = /*(width - spacing * 4 - refreshBtnSize) / 2*/200;
        addDrawableChild(new ExtendedTexturedButtonWidget(2, 2, refreshBtnSize, refreshBtnSize, 0, 0, refreshBtnSize, CONFIG_BUTTON_TEXTURE, 32, 64, button -> {
            client.setScreen(new ScreenshotViewerConfigScreen(this));
        }, (button, matrices, x, y) -> {
            renderOrderedTooltip(matrices, client.textRenderer.wrapLines(ScreenshotViewer.translatable("screen", "button.config"), Math.max(width / 2 - 43, 170)), x, y + refreshBtnSize);
        }, ScreenshotViewer.translatable("screen", "button.config")));
        addDrawableChild(new ExtendedTexturedButtonWidget(spacing, btnY, refreshBtnSize, refreshBtnSize, 0, 0, refreshBtnSize, null, 32, 64, button -> {
            if(list != null) {
                list.invertOrder();
            }
        }, (button, matrices, x, y) -> {
            if(list != null) {
                renderOrderedTooltip(matrices, client.textRenderer.wrapLines(ScreenshotViewer.translatable("screen", list.isInvertedOrder() ? "button.order.descending" : "button.order.ascending"), Math.max(width / 2 - 43, 170)), x, y);
            }
        }, ScreenshotViewer.translatable("screen", "button.order")) {
            @Override
            public @Nullable Identifier getTexture() {
                return list == null ? null : list.isInvertedOrder() ? DESCENDING_ORDER_BUTTON_TEXTURE : ASCENDING_ORDER_BUTTON_TEXTURE;
            }
        });
        addDrawableChild(new ExtendedButtonWidget((width - bigBtnWidth) / 2, btnY, bigBtnWidth, btnHeight, ScreenTexts.DONE, button -> close()));
        addDrawableChild(new ExtendedTexturedButtonWidget(width - spacing - refreshBtnSize, btnY, refreshBtnSize, refreshBtnSize, 0, 0, refreshBtnSize, REFRESH_BUTTON_TEXTURE, 32, 64, button -> {
            list.init();
        }, (btn, matrices, x, y) -> {
            renderOrderedTooltip(matrices, client.textRenderer.wrapLines(ScreenshotViewer.translatable("screen", "button.refresh"), Math.max(width / 2 - 43, 170)), x, y);
        }, ScreenshotViewer.translatable("screen", "button.refresh")));
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        this.selectedScreenshot.init(client, width, height);
    }

    private float screenshotScaleAnimation;

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        if(list != null) {
            list.render(matrices, mouseX, mouseY, delta, !selectedScreenshot.hasInfo());
        }
        drawCenteredText(matrices, textRenderer, title,width / 2, 8, 0xFFFFFF);
        Text text = ScreenshotViewer.translatable("screen", "screenshot_manager.zoom");
        drawTextWithShadow(matrices, textRenderer, text, width - textRenderer.getWidth(text) - 8, 8, isCtrlDown ? 0x18DE39 : 0xF0CA22);
        super.render(matrices, mouseX, mouseY, delta);
        if(selectedScreenshot.hasInfo()) {
            float animationTime = 1;

            if(CONFIG.getOrFallback(ScreenshotViewerOptions.ENABLE_SCREENSHOT_ENLARGEMENT_ANIMATION, true)) {
                if(screenshotScaleAnimation < 1f) {
                    animationTime = (float) (1 - Math.pow(1 - (screenshotScaleAnimation += 0.03), 3));
                }
            }

            matrices.push();
            matrices.translate(0, 0, 1);
            selectedScreenshot.renderBackground(matrices);
            matrices.translate((selectedScreenshot.width / 2f) * (1 - animationTime), (selectedScreenshot.height / 2f) * (1 - animationTime), 0);
            matrices.scale(animationTime, animationTime, animationTime);
            selectedScreenshot.render(matrices, mouseX, mouseY, delta);
            matrices.pop();
        } else {
            if(screenshotScaleAnimation > 0) {
                screenshotScaleAnimation = 0;
            }

            for(Element element : this.children()) {
                if(element instanceof CustomHoverState hover) {
                    hover.updateHoveredState(mouseX, mouseY);
                }
            }
        }
    }

    void showScreenshot(ScreenshotImageHolder showing) {
        this.selectedScreenshot.show(showing, list);
    }

    private boolean isCtrlDown;

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(selectedScreenshot.hasInfo()) {
            return selectedScreenshot.keyPressed(keyCode, scanCode, modifiers);
        }
        isCtrlDown = keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if(selectedScreenshot.hasInfo()) {
            return selectedScreenshot.keyReleased(keyCode, scanCode, modifiers);
        }
        if(isCtrlDown) {
            isCtrlDown = false;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if(selectedScreenshot.hasInfo()) {
            return selectedScreenshot.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if(selectedScreenshot.hasInfo()) {
            return selectedScreenshot.mouseScrolled(mouseX, mouseY, amount);
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
        if(selectedScreenshot.hasInfo()) {
            return selectedScreenshot.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(selectedScreenshot.hasInfo()) {
            return selectedScreenshot.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if(selectedScreenshot.hasInfo()) {
            return selectedScreenshot.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public Optional<Element> hoveredElement(double mouseX, double mouseY) {
        if(selectedScreenshot.hasInfo()) {
            return selectedScreenshot.hoveredElement(mouseX, mouseY);
        }
        return super.hoveredElement(mouseX, mouseY);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public void removed() {
        list.close(); // Free textures
    }

    @Override
    public void configUpdated() {
        this.list.onConfigUpdate();
    }

    private static final class ScreenshotList extends AbstractParentElement implements Drawable, Selectable, ScreenshotImageList {
        private final ManageScreenshotsScreen parent;
        private final int x, y;
        private final MinecraftClient client;
        private final List<ScreenshotWidget> screenshotWidgets = new ArrayList<>();
        private final List<Element> elements = new ArrayList<>();

        private int width, height;
        private int scrollY;
        private int scrollSpeedFactor;
        private int screenshotsPerRow;
        private int spacing, childWidth, childHeight;
        private boolean invertedOrder;

        ScreenshotList(ManageScreenshotsScreen parent, int x, int y, int width, int height) {
            this.parent = parent;
            this.client = parent.client;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.scrollSpeedFactor = CONFIG.getOrFallback(ScreenshotViewerOptions.SCREEN_SCROLL_SPEED, 10);
            this.screenshotsPerRow = CONFIG.getOrFallback(ScreenshotViewerOptions.INITIAL_SCREENSHOT_AMOUNT_PER_ROW, 4);
            updateVariables();
        }

        void updateSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        private void updateVariables() {
            float windowAspect = (float) client.getWindow().getWidth() / (float) client.getWindow().getHeight();
            final int scrollbarWidth = 6;
            final int scrollbarSpacing = 2;
            spacing = 4;
            childWidth = (width - (screenshotsPerRow + 1) * spacing - scrollbarWidth - scrollbarSpacing) / screenshotsPerRow;
            childHeight = (int) (1.08 * childWidth / windowAspect);
        }

        void onConfigUpdate() {
            this.scrollSpeedFactor = CONFIG.getOrFallback(ScreenshotViewerOptions.SCREEN_SCROLL_SPEED, 10);
            this.screenshotsPerRow = CONFIG.getOrFallback(ScreenshotViewerOptions.INITIAL_SCREENSHOT_AMOUNT_PER_ROW, 4);
            updateChildren();
        }

        void init() {
            clearChildren();

            File[] files = new File(client.runDirectory, "screenshots").listFiles();
            if(files != null) {
                updateVariables();
                final int maxXOff = screenshotsPerRow - 1;

                int childX = x + spacing;
                int childY = y + spacing;
                int xOff = 0;

                ScreenshotWidget.Context context = ScreenshotWidget.Context.create(() -> screenshotsPerRow, screenshotWidgets::indexOf);
                for(File file : files) {
                    if(file.isFile() && file.getName().endsWith(".png")) {
                        ScreenshotWidget widget = new ScreenshotWidget(parent, childX, childY, childWidth, childHeight, context, file);
                        this.screenshotWidgets.add(widget);
                        this.elements.add(widget);

                        if(xOff == maxXOff) {
                            xOff = 0;
                            childX = x + spacing;
                            childY += childHeight + spacing;
                        } else {
                            xOff++;
                            childX += childWidth + spacing;
                        }
                    }
                }
            }
        }

        private void clearChildren() {
            close();
            screenshotWidgets.clear();
            elements.clear();
        }

        void updateChildren() {
            scrollY = 0;
            updateVariables();
            final int maxXOff = screenshotsPerRow - 1;

            int childX = x + spacing;
            int childY = y + spacing;
            int xOff = 0;

            for(ScreenshotWidget widget : screenshotWidgets) {
                widget.x = childX;
                widget.updateBaseY(childY);
                widget.setWidth(childWidth);
                widget.setHeight(childHeight);

                if(xOff == maxXOff) {
                    xOff = 0;
                    childX = x + spacing;
                    childY += childHeight + spacing;
                } else {
                    xOff++;
                    childX += childWidth + spacing;
                }
            }
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        }

        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta, boolean updateHoverState) {
            fill(matrices, x, y, x + width, y + height, ColorHelper.Argb.getArgb((int) (0.7f * 255), 0, 0, 0));
            if(screenshotWidgets.isEmpty()) {
                drawCenteredText(matrices, client.textRenderer, ScreenshotViewer.translatable("screen", "screenshot_manager.no_screenshots"), (x + width) / 2, (y + height + 8) / 2, 0xFFFFFF);
            }
            for(ScreenshotWidget screenshotWidget : screenshotWidgets) {
                screenshotWidget.updateY(scrollY);
                if(screenshotWidget.y + screenshotWidget.getHeight() < y || screenshotWidget.y > y + height) {
                    continue;
                }
                screenshotWidget.render(matrices, mouseX, mouseY, delta, y + spacing, y + height - spacing, updateHoverState);
            }
            if(canScroll()) {
                renderScrollbar(matrices);
            }
        }

        public void close() {
            screenshotWidgets.forEach(ScreenshotWidget::close);
        }

        private void renderScrollbar(MatrixStack matrices) {
            final int scrollbarWidth = 6;
            final int scrollbarSpacing = 2;
            final int scrollbarX = x + width - scrollbarSpacing - scrollbarWidth;

            final int rows = MathHelper.ceil(screenshotWidgets.size() / (float) screenshotsPerRow);
            final int scrollbarTrackX = scrollbarX + 2;
            final int scrollbarTrackWidth = 2;
            final int scrollbarTrackHeight = height - spacing * 2;
            final int scrollbarTrackY = y + spacing;

            final int contentHeight = rows * childHeight + spacing * rows;
            final int scrollbarHeight = (scrollbarTrackHeight + 2) * (scrollbarTrackHeight + 2) / contentHeight;
            final int scrollbarY = (scrollY * (scrollbarTrackHeight + 4) / contentHeight) + y + scrollbarSpacing;

            DrawableHelper.fill(matrices, scrollbarTrackX, scrollbarTrackY, scrollbarTrackX + scrollbarTrackWidth, scrollbarTrackY + scrollbarTrackHeight, ColorHelper.Argb.getArgb(255, 255, 255, 255));
            DrawableHelper.fill(matrices, scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight, ColorHelper.Argb.getArgb(255, 30, 30, 30));
        }

        void invertOrder() {
            Collections.reverse(screenshotWidgets);
            invertedOrder = !invertedOrder;
            updateChildren();
        }

        boolean isInvertedOrder() {
            return invertedOrder;
        }

        @Override
        public List<? extends Element> children() {
            return elements;
        }

        private boolean canScroll() {
            final int rows = MathHelper.ceil(screenshotWidgets.size() / (float) screenshotsPerRow);

            return rows * (childHeight + spacing) > height - spacing * 2;
        }

        private boolean canScrollDown() {
            final int rows = MathHelper.ceil(screenshotWidgets.size() / (float) screenshotsPerRow);
            final int leftOver = rows * childHeight + spacing * (rows - 1) - height + spacing * 2;

            return scrollY < leftOver;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
            if(canScroll()) {
                final int scrollSpeed = Math.abs((int) (scrollSpeedFactor * (6.0f / screenshotsPerRow) * amount));
                if(scrollY > 0 && amount > 0) {
                    scrollY = Math.max(0, scrollY - scrollSpeed);
                }
                if(canScrollDown() && amount < 0) {
                    final int rows = MathHelper.ceil(screenshotWidgets.size() / (float) screenshotsPerRow);
                    final int leftOver = rows * childHeight + spacing * rows - height + spacing * 2;

                    scrollY = Math.min(leftOver, scrollY + scrollSpeed);
                }
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, amount);
        }

        void updateScreenshotsPerRow(double scrollAmount) {
            if(scrollAmount > 0) {
                if(screenshotsPerRow < 8) {
                    screenshotsPerRow = Math.min(8, screenshotsPerRow + 1);
                }
            } else if(scrollAmount < 0) {
                if(screenshotsPerRow > 2) {
                    screenshotsPerRow = Math.max(2, screenshotsPerRow - 1);
                }
            }
            updateChildren();
        }

        @Override
        public void appendNarrations(NarrationMessageBuilder builder) {
        }

        @Override
        public SelectionType getType() {
            return SelectionType.NONE;
        }

        @Override
        public ScreenshotImageHolder getScreenshot(int index) {
            return screenshotWidgets.get(index);
        }

        @Override
        public int size() {
            return screenshotWidgets.size();
        }
    }

    private static final class ScreenshotWidget extends ClickableWidget implements AutoCloseable, ScreenshotImageHolder {
        private float bgOpacity = 0;
        private int baseY;

        private final ManageScreenshotsScreen parent;
        private final MinecraftClient client;
        private final Context ctx;
        private final File screenshotFile;
        private CompletableFuture<NativeImage> image;
        @Nullable
        private NativeImageBackedTexture texture;

        public ScreenshotWidget(ManageScreenshotsScreen parent, int x, int y, int width, int height, Context ctx, File screenshotFile) {
            super(x, y, width, height, Text.literal(screenshotFile.getName()));
            this.parent = parent;
            this.client = parent.client;
            this.baseY = y;
            this.ctx = ctx;
            this.screenshotFile = screenshotFile;
            this.image = getImage(screenshotFile);
        }

        void updateBaseY(int baseY) {
            this.y = this.baseY = baseY;
        }

        void updateY(int scrollY) {
            this.y = baseY - scrollY;
        }

        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta, int viewportY, int viewportBottom, boolean updateHoverState) {
            this.hovered = updateHoverState && (mouseX >= this.x && mouseY >= Math.max(this.y, viewportY) && mouseX < this.x + this.width && mouseY < Math.min(this.y + this.height, viewportBottom));
            renderBackground(matrices, client, mouseX, mouseY, viewportY, viewportBottom);
            final int spacing = 2;

            NativeImageBackedTexture image = texture();
            if(image != null && image.getImage() != null) {
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.setShaderTexture(0, image.getGlId());
                RenderSystem.enableBlend();
                int renderY = Math.max(y + spacing, viewportY);
                int imgHeight = (int) (height / 1.08 - spacing * 3);
                int topOffset = Math.max(0, viewportY - y - spacing);
                int bottomOffset = Math.max(0, y + spacing + imgHeight - viewportBottom);
                int topV = topOffset * image.getImage().getHeight() / imgHeight;
                int bottomV = bottomOffset * image.getImage().getHeight() / imgHeight;
                DrawableHelper.drawTexture(matrices,
                        x + spacing,
                        renderY,
                        width - spacing * 2,
                        imgHeight - topOffset - bottomOffset,
                        0,
                        topV,
                        image.getImage().getWidth(),
                        image.getImage().getHeight() - topV - bottomV,
                        image.getImage().getWidth(),
                        image.getImage().getHeight()
                );
                RenderSystem.disableBlend();
            }
            int textY = y + (int) (height / 1.08) - spacing;
            if(textY > viewportY && textY + 8 < viewportBottom) {
                matrices.push();
                matrices.translate(x + width / 2f, textY, 0);
                float scaleFactor = (float) (client.getWindow().getScaledHeight() / 96) / ctx.screenshotsPerRow();
                matrices.scale(scaleFactor, scaleFactor, scaleFactor);
                drawCenteredText(matrices, client.textRenderer, getMessage(), 0, 0, 0xFFFFFF);
                matrices.pop();
            }
        }

        @Override
        public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        }

        private void renderBackground(MatrixStack matrices, MinecraftClient client, int mouseX, int mouseY, int viewportY, int viewportBottom) {
            if(hovered) {
                if(bgOpacity < 1) {
                    bgOpacity = Math.min(1, bgOpacity + 0.05F);
                }
            }
            else {
                if(bgOpacity > 0) {
                    bgOpacity = Math.max(0, bgOpacity - 0.05F);
                }
            }
            int renderY = Math.max(y, viewportY);
            int renderHeight = Math.min(y + height, viewportBottom);
            DrawableHelper.fill(matrices, x, renderY, x + width, renderHeight, ColorHelper.Argb.getArgb((int) (bgOpacity * 255), 255, 255, 255));
        }

        private CompletableFuture<NativeImage> getImage(File file) {
            return CompletableFuture.supplyAsync(() -> {
                try (InputStream inputStream = new FileInputStream(file)) {
                    return NativeImage.read(inputStream);
                } catch (Exception e) {
                    LOGGER.error("Failed to load screenshot: {}", file.getName(), e);
                }
                return null;
            }, Util.getMainWorkerExecutor());
        }

        private void onClick() {
            this.parent.showScreenshot(this);
        }

        private void onRightClick() {

        }

        private void playDownSound(SoundManager soundManager, int button) {
            soundManager.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }

        @Override
        public void close() {
            if (texture != null) {
                texture.close(); // Also closes the image
            } else if(image != null) {
                image.thenAcceptAsync(image -> {
                    if (image != null) {
                        image.close();
                    }
                }, this.client);
            }
            image = null;
            texture = null;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if(isHovered()) {
                playDownSound(this.client.getSoundManager(), button);
                if(button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    onClick();
                }
                if(button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    onRightClick();
                }
                return true;
            }
            return false;
        }

        @Override
        protected boolean clicked(double mouseX, double mouseY) {
            return false;
        }

        @Override
        public boolean isHovered() {
            return hovered;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return isHovered();
        }

        @Override
        public void appendNarrations(NarrationMessageBuilder builder) {
        }

        public void setHeight(int height) {
            this.height = height;
        }

        @Override
        public int indexInList() {
            return ctx.currentIndex(this);
        }

        @Override
        public int imageId() {
            NativeImageBackedTexture texture = texture();
            return texture != null ? texture.getGlId() : 0;
        }

        @Nullable
        @Override
        public NativeImage image() {
            if (image == null) {
                image = getImage(screenshotFile);
            }
            return image.getNow(null);
        }

        @Nullable
        public NativeImageBackedTexture texture() {
            if (texture != null) {
                return texture;
            }
            if (image == null) {
                image = getImage(screenshotFile);
            }
            if (image.isDone()) {
                return texture = new NativeImageBackedTexture(image.join());
            }
            return null;
        }

        interface Context {
            int screenshotsPerRow();

            int currentIndex(ScreenshotWidget widget);

            static Context create(IntSupplier screenshotsPerRow, ToIntFunction<ScreenshotWidget> currentIndex) {
                return new Context() {
                    @Override
                    public int screenshotsPerRow() {
                        return screenshotsPerRow.getAsInt();
                    }

                    @Override
                    public int currentIndex(ScreenshotWidget widget) {
                        return currentIndex.applyAsInt(widget);
                    }
                };
            }
        }
    }

    private static class SelectedScreenshotScreen extends Screen {
        @Nullable
        private ScreenshotImageHolder showing;
        @Nullable
        private ScreenshotImageList imageList;
        private ButtonWidget nextBtn, prevBtn;

        SelectedScreenshotScreen() {
            super(Text.empty());
        }

        @Override
        protected void init() {
            super.init();
            clearChildren();
            addDrawableChild(new ButtonWidget((width - 52) / 2, height - 20 - 8, 52, 20, ScreenTexts.DONE, btn -> close()));
            addDrawableChild(prevBtn = new ButtonWidget(8, (height - 20) / 2, 20, 20, Text.literal("<"), btn -> previousScreenshot()));
            addDrawableChild(nextBtn = new ButtonWidget(width - 8 - 20, (height - 20) / 2, 20, 20, Text.literal(">"), btn -> nextScreenshot()));
        }

        private void nextScreenshot() {
            if(hasInfo()) {
                int i = showing.indexInList() + 1;
                if(i < imageList.size()) {
                    showing = imageList.getScreenshot(i);
                    updateButtonsState();
                }
            }
        }

        private void previousScreenshot() {
            if(hasInfo()) {
                int i = showing.indexInList() - 1;
                if(i >= 0) {
                    showing = imageList.getScreenshot(i);
                    updateButtonsState();
                }
            }
        }

        private void updateButtonsState() {
            if(hasInfo()) {
                int i = showing.indexInList();
                prevBtn.active = i > 0;
                nextBtn.active = i < imageList.size() - 1;
            }
        }

        void show(ScreenshotImageHolder showing, ScreenshotImageList imageList) {
            this.showing = showing;
            this.imageList = imageList;
            this.hasInfoCached = null;
            updateButtonsState();
        }

        @Nullable
        private Boolean hasInfoCached;

        boolean hasInfo() {
            if(hasInfoCached == null) {
                hasInfoCached = showing != null && imageList != null;
            }
            return hasInfoCached;
        }

        @Override
        public void renderBackground(MatrixStack matrices) {
            this.fillGradient(matrices, 0, 0, this.width, this.height, -1072689136, -804253680);
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            if(showing != null) {
                final int spacing = 8;

                NativeImage image = showing.image();
                if(image != null) {
                    RenderSystem.setShader(GameRenderer::getPositionTexShader);
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                    RenderSystem.setShaderTexture(0, showing.imageId());
                    RenderSystem.enableBlend();
                    float imgRatio = (float) image.getWidth() / image.getHeight();
                    int texHeight = height - spacing * 3 - 20;
                    int texWidth = (int) (texHeight * imgRatio);
                    DrawableHelper.drawTexture(matrices, (width - texWidth) / 2, spacing, texWidth, texHeight, 0, 0, image.getWidth(), image.getHeight(), image.getWidth(), image.getHeight());
                    RenderSystem.disableBlend();
                }

                super.render(matrices, mouseX, mouseY, delta);
            }
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
            if(amount > 0) {
                nextScreenshot();
            }
            if(amount < 0) {
                previousScreenshot();
            }
            return super.mouseScrolled(mouseX, mouseY, amount);
        }

        @Override
        public void close() {
            showing = null;
            imageList = null;
            this.hasInfoCached = null;
        }
    }

    private static class ScreenshotPropertiesWidget extends AbstractParentElement implements Drawable {
        private final List<ClickableWidget> buttons = new ArrayList<>();

        private boolean shouldRender;

        ScreenshotPropertiesWidget() {

        }

        void show(int x, int y) {
            buttons.clear();

            shouldRender = true;
        }

        void hide() {
            buttons.clear();
            shouldRender = false;
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            if(shouldRender) {

            }
        }

        @Override
        public List<? extends Element> children() {
            return buttons;
        }
    }

    private static final class ExtendedButtonWidget extends ButtonWidget implements CustomHoverState {
        ExtendedButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
            super(x, y, width, height, message, onPress);
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            if (!this.visible) {
                return;
            }
            this.renderButton(matrices, mouseX, mouseY, delta);
        }

        public void updateHoveredState(int mouseX, int mouseY) {
            this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
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

        ExtendedTexturedButtonWidget(int x, int y, int width, int height, int u, int v, int hoveredVOffset, @Nullable Identifier texture, int textureWidth, int textureHeight, PressAction pressAction, TooltipSupplier tooltipSupplier, Text text) {
            super(x, y, width, height, u, v, hoveredVOffset, texture, textureWidth, textureHeight, pressAction, tooltipSupplier, text);
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
        public Identifier getTexture() {
            return texture;
        }

        @Override
        public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            Identifier texture = getTexture();
            if(texture == null) {
                DrawableHelper.fill(matrices, x, y, x + width, y + height, 0xFFFFFF);
            } else {
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, texture);
                int vOffset = this.v;
                if (!this.isNarratable()) {
                    vOffset += this.hoveredVOffset * 2;
                } else if (this.isHovered()) {
                    vOffset += this.hoveredVOffset;
                }
                RenderSystem.enableDepthTest();
                DrawableHelper.drawTexture(matrices, this.x, this.y, this.u, vOffset, this.width, this.height, this.textureWidth, this.textureHeight);
                if (this.hovered) {
                    this.renderTooltip(matrices, mouseX, mouseY);
                }
            }
        }

        public void updateHoveredState(int mouseX, int mouseY) {
            this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        }
    }

    private interface CustomHoverState {
        void updateHoveredState(int mouseX, int mouseY);
    }

    private interface ScreenshotImageList {
        ScreenshotImageHolder getScreenshot(int index);

        int size();
    }

    private interface ScreenshotImageHolder {
        int indexInList();

        int imageId();

        @Nullable
        NativeImage image();
    }
}

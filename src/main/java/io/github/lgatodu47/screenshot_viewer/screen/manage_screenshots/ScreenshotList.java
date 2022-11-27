package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ScreenshotList extends AbstractParentElement implements Drawable, Selectable, ScreenshotImageList {
    private final ManageScreenshotsScreen mainScreen;
    private final MinecraftClient client;
    private final int x, y;
    private final List<ScreenshotWidget> screenshotWidgets = new ArrayList<>();
    private final List<Element> elements = new ArrayList<>();

    private int width, height;
    private int scrollY;
    private int scrollSpeedFactor;
    private int screenshotsPerRow;
    private int spacing, childWidth, childHeight;
    private boolean invertedOrder;

    ScreenshotList(ManageScreenshotsScreen mainScreen, int x, int y, int width, int height) {
        this.mainScreen = mainScreen;
        this.client = mainScreen.client();
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scrollSpeedFactor = ManageScreenshotsScreen.CONFIG.getOrFallback(ScreenshotViewerOptions.SCREEN_SCROLL_SPEED, 10);
        this.screenshotsPerRow = ManageScreenshotsScreen.CONFIG.getOrFallback(ScreenshotViewerOptions.INITIAL_SCREENSHOT_AMOUNT_PER_ROW, 4);
        updateVariables();
    }

    /// Accessible methods from the main screen ///

    void updateSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    void onConfigUpdate() {
        this.scrollSpeedFactor = ManageScreenshotsScreen.CONFIG.getOrFallback(ScreenshotViewerOptions.SCREEN_SCROLL_SPEED, 10);
        this.screenshotsPerRow = ManageScreenshotsScreen.CONFIG.getOrFallback(ScreenshotViewerOptions.INITIAL_SCREENSHOT_AMOUNT_PER_ROW, 4);
        updateChildren();
    }

    /**
     * Creates all the child elements of this list.
     */
    void init() {
        clearChildren();

        File[] files = new File(client.runDirectory, "screenshots").listFiles();
        if (files != null) {
            updateVariables();
            final int maxXOff = screenshotsPerRow - 1;

            int childX = x + spacing;
            int childY = y + spacing;
            int xOff = 0;

            ScreenshotWidget.Context context = ScreenshotWidget.Context.create(() -> screenshotsPerRow, screenshotWidgets::indexOf);
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".png")) {
                    ScreenshotWidget widget = new ScreenshotWidget(mainScreen, childX, childY, childWidth, childHeight, context, file);
                    this.screenshotWidgets.add(widget);
                    this.elements.add(widget);

                    if (xOff == maxXOff) {
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

    /**
     * Updates the number of screenshots per row. Called when the `ctrl` key is held and when the user is scrolling.
     * @param scrollAmount A value that determines the scrolling direction and intensity (value from -1.0 to 1.0).
     */
    void updateScreenshotsPerRow(double scrollAmount) {
        if (scrollAmount > 0) {
            if (screenshotsPerRow < 8) {
                screenshotsPerRow = Math.min(8, screenshotsPerRow + 1);
            }
        } else if (scrollAmount < 0) {
            if (screenshotsPerRow > 2) {
                screenshotsPerRow = Math.max(2, screenshotsPerRow - 1);
            }
        }
        updateChildren();
    }

    /**
     * Updates the children positions.
     */
    void updateChildren() {
        scrollY = 0;
        updateVariables();
        final int maxXOff = screenshotsPerRow - 1;

        int childX = x + spacing;
        int childY = y + spacing;
        int xOff = 0;

        for (ScreenshotWidget widget : screenshotWidgets) {
            widget.x = childX;
            widget.updateBaseY(childY);
            widget.setWidth(childWidth);
            widget.setHeight(childHeight);

            if (xOff == maxXOff) {
                xOff = 0;
                childX = x + spacing;
                childY += childHeight + spacing;
            } else {
                xOff++;
                childX += childWidth + spacing;
            }
        }
    }

    void removeEntry(ScreenshotWidget widget) {
        screenshotWidgets.remove(widget);
        elements.remove(widget);
        updateChildren();
    }

    /**
     * Updates the list variables (width and height of children, etc.).
     */
    private void updateVariables() {
        float windowAspect = (float) client.getWindow().getWidth() / (float) client.getWindow().getHeight();
        final int scrollbarWidth = 6;
        final int scrollbarSpacing = 2;
        spacing = 4;
        childWidth = (width - (screenshotsPerRow + 1) * spacing - scrollbarWidth - scrollbarSpacing) / screenshotsPerRow;
        childHeight = (int) (1.08 * childWidth / windowAspect);
    }

    private void clearChildren() {
        close();
        screenshotWidgets.clear();
        elements.clear();
    }

    public void close() {
        screenshotWidgets.forEach(ScreenshotWidget::close);
    }

    /// Common Methods ///

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
    }

    // The boolean added controls whether the screenshot widgets should update its `hovered` state.
    void render(MatrixStack matrices, int mouseX, int mouseY, float delta, boolean updateHoverState) {
        fill(matrices, x, y, x + width, y + height, ColorHelper.Argb.getArgb((int) (0.7f * 255), 0, 0, 0));
        if (screenshotWidgets.isEmpty()) {
            drawCenteredText(matrices, client.textRenderer, ScreenshotViewer.translatable("screen", "screenshot_manager.no_screenshots"), (x + width) / 2, (y + height + 8) / 2, 0xFFFFFF);
        }
        for (ScreenshotWidget screenshotWidget : screenshotWidgets) {
            screenshotWidget.updateY(scrollY);
            // skips rendering the widget if it is not at all in the render area
            if (screenshotWidget.y + screenshotWidget.getHeight() < y || screenshotWidget.y > y + height) {
                continue;
            }
            screenshotWidget.render(matrices, mouseX, mouseY, delta, y + spacing, y + height - spacing, updateHoverState);
        }
        if (canScroll()) {
            renderScrollbar(matrices);
        }
    }

    @Override
    public List<? extends Element> children() {
        return elements;
    }

    /// Methods from ScreenshotImageList ///

    @Override
    public ScreenshotImageHolder getScreenshot(int index) {
        return screenshotWidgets.get(index);
    }

    @Override
    public int size() {
        return screenshotWidgets.size();
    }

    /// List order ///

    void invertOrder() {
        Collections.reverse(screenshotWidgets);
        invertedOrder = !invertedOrder;
        updateChildren();
    }

    boolean isInvertedOrder() {
        return invertedOrder;
    }

    /// Scrolling and Scrollbar ///

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
        if (canScroll()) {
            final int scrollSpeed = Math.abs((int) (scrollSpeedFactor * (6.0f / screenshotsPerRow) * amount));
            if (scrollY > 0 && amount > 0) {
                scrollY = Math.max(0, scrollY - scrollSpeed);
            }
            if (canScrollDown() && amount < 0) {
                final int rows = MathHelper.ceil(screenshotWidgets.size() / (float) screenshotsPerRow);
                final int leftOver = rows * childHeight + spacing * rows - height + spacing * 2;

                scrollY = Math.min(leftOver, scrollY + scrollSpeed);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    /// Random implementation methods ///

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
    }

    @Override
    public SelectionType getType() {
        return SelectionType.NONE;
    }
}

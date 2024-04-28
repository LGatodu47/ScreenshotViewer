package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotListOrder;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions;
import io.github.lgatodu47.screenshot_viewer.config.VisibilityState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.*;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

final class ScreenshotList extends AbstractParentElement implements Drawable, Selectable, ScreenshotImageList {
    private final ManageScreenshotsScreen mainScreen;
    private final MinecraftClient client;
    private final int x, y;
    private final List<ScreenshotWidget> screenshotWidgets = new ArrayList<>();
    private final List<Element> elements = new ArrayList<>();
    private final Scrollbar scrollbar = new Scrollbar();

    private int width, height;
    // Offset from top
    private int scrollY;
    private int scrollSpeedFactor;
    private int screenshotsPerRow;
    private int spacing, childWidth, childHeight;
    private boolean invertedOrder, invertedScroll, namesHidden;
    private File screenshotsFolder;

    ScreenshotList(ManageScreenshotsScreen mainScreen, int x, int y, int width, int height) {
        this.mainScreen = mainScreen;
        this.client = mainScreen.client();
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scrollSpeedFactor = ManageScreenshotsScreen.CONFIG.getOrFallback(ScreenshotViewerOptions.SCREEN_SCROLL_SPEED, 10);
        this.screenshotsPerRow = ManageScreenshotsScreen.CONFIG.getOrFallback(ScreenshotViewerOptions.INITIAL_SCREENSHOT_AMOUNT_PER_ROW, 4);
        this.screenshotsFolder = ManageScreenshotsScreen.CONFIG.getOrFallback(ScreenshotViewerOptions.SCREENSHOTS_FOLDER, (Supplier<? extends File>) ScreenshotViewer::getVanillaScreenshotsFolder);
        this.invertedScroll = ManageScreenshotsScreen.CONFIG.getOrFallback(ScreenshotViewerOptions.INVERT_ZOOM_DIRECTION, false);
        this.namesHidden = ManageScreenshotsScreen.CONFIG.get(ScreenshotViewerOptions.SCREENSHOT_ELEMENT_TEXT_VISIBILITY).filter(VisibilityState.HIDDEN::equals).isPresent();
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
        this.invertedScroll = ManageScreenshotsScreen.CONFIG.getOrFallback(ScreenshotViewerOptions.INVERT_ZOOM_DIRECTION, false);
        this.namesHidden = ManageScreenshotsScreen.CONFIG.get(ScreenshotViewerOptions.SCREENSHOT_ELEMENT_TEXT_VISIBILITY).filter(VisibilityState.HIDDEN::equals).isPresent();
        File currentScreenshotsFolder = ManageScreenshotsScreen.CONFIG.getOrFallback(ScreenshotViewerOptions.SCREENSHOTS_FOLDER, (Supplier<? extends File>) ScreenshotViewer::getVanillaScreenshotsFolder);
        if(screenshotsFolder != currentScreenshotsFolder) {
            screenshotsFolder = currentScreenshotsFolder;
            init();
            return;
        }
        if(invertedOrder != ManageScreenshotsScreen.CONFIG.getOrFallback(ScreenshotViewerOptions.DEFAULT_LIST_ORDER, ScreenshotListOrder.ASCENDING).isInverted()) {
            invertOrder();
            return;
        }
        updateChildren();
    }

    /**
     * Creates all the child elements of this list.
     */
    void init() {
        clearChildren();

        File[] files = screenshotsFolder.listFiles();
        if (files != null) {
            Arrays.sort(files);
            updateVariables();
            final int maxXOff = screenshotsPerRow - 1;

            int childX = x + spacing;
            int childY = y + spacing;
            int xOff = 0;

            ScreenshotWidget.Context context = ScreenshotWidget.Context.create(() -> screenshotsPerRow, screenshotWidgets::indexOf);
            for (File file : files) {
                if (file.isFile() && (file.getName().endsWith(".png") || file.getName().endsWith(".jpg") || file.getName().endsWith(".jpeg"))) {
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
        scrollbar.repositionScrollbar(x, y, width, height, spacing, getTotalHeightOfChildren());
        if(ManageScreenshotsScreen.CONFIG.getOrFallback(ScreenshotViewerOptions.DEFAULT_LIST_ORDER, ScreenshotListOrder.ASCENDING).isInverted()) {
            invertOrder();
        }
    }

    /**
     * Updates the number of screenshots per row. Called when the `ctrl` key is held and when the user is scrolling.
     * @param scrollAmount A value that determines the scrolling direction and intensity (value from -1.0 to 1.0).
     */
    void updateScreenshotsPerRow(double scrollAmount) {
        scrollAmount = invertedScroll ? -scrollAmount : scrollAmount;
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
            widget.setX(childX);
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
        scrollbar.repositionScrollbar(x, y, width, height, spacing, getTotalHeightOfChildren());
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
        childHeight = (int) ((namesHidden ? 1 : 1.08) * childWidth / windowAspect);
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    // The boolean added controls whether the screenshot widgets should update its `hovered` state.
    void render(DrawContext context, int mouseX, int mouseY, float delta, boolean updateHoverState) {
        context.fill(x, y, x + width, y + height, ColorHelper.Argb.getArgb((int) (0.7f * 255), 0, 0, 0));
        if (screenshotWidgets.isEmpty()) {
            context.drawCenteredTextWithShadow(client.textRenderer, ScreenshotViewer.translatable("screen", "screenshot_manager.no_screenshots"), (x + width) / 2, (y + height + 8) / 2, 0xFFFFFF);
        }
        for (ScreenshotWidget screenshotWidget : screenshotWidgets) {
            screenshotWidget.updateY(scrollY);
            int viewportY = y + spacing;
            int viewportBottom = y + height - spacing;
            screenshotWidget.updateHoverState(mouseX, mouseY, viewportY, viewportBottom, updateHoverState);
            // skips rendering the widget if it is not at all in the render area
            if (screenshotWidget.getY() + screenshotWidget.getHeight() < y || screenshotWidget.getY() > y + height) {
                continue;
            }
            screenshotWidget.render(context, mouseX, mouseY, delta, viewportY, viewportBottom);
        }
        if (canScroll()) {
            scrollbar.render(context, mouseX, mouseY, scrollY, scrollbarClicked);
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
    public Optional<ScreenshotImageHolder> findByFileName(File file) {
        return screenshotWidgets.stream().filter(screenshotWidget -> screenshotWidget.getScreenshotFile().equals(file)).map(ScreenshotImageHolder.class::cast).findFirst();
    }

    @Override
    public int size() {
        return screenshotWidgets.size();
    }

    /// List order ///

    void invertOrder() {
        Collections.reverse(screenshotWidgets);
        invertedOrder = !invertedOrder;
        int previousScrollY = scrollY;
        updateChildren();
        scrollY = previousScrollY;
    }

    boolean isInvertedOrder() {
        return invertedOrder;
    }

    /// Scrolling and Scrollbar ///

    private boolean canScroll() {
        final int totalHeightOfTheChildrens = getTotalHeightOfChildren();
        final int viewHeight = height - 2 * spacing;

        return totalHeightOfTheChildrens > viewHeight;
    }

    private boolean canScrollDown() {
        final int totalHeightOfTheChildrens = getTotalHeightOfChildren();
        final int viewHeight = height - 2 * spacing;
        // Maximum offset from the top
        final int leftOver = totalHeightOfTheChildrens - viewHeight;

        return scrollY < leftOver;
    }

    private int getTotalHeightOfChildren() {
        int rows = MathHelper.ceil(screenshotWidgets.size() / (float) screenshotsPerRow);
        return rows * childHeight + spacing * (rows - 1);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (canScroll()) {
            final int scrollSpeed = Math.abs((int) (scrollSpeedFactor * (6.0f / screenshotsPerRow) * verticalAmount));
            if (scrollY > 0 && verticalAmount > 0) {
                scrollY = Math.max(0, scrollY - scrollSpeed);
            }
            if (canScrollDown() && verticalAmount < 0) {
                final int totalHeightOfTheChildrens = getTotalHeightOfChildren();
                final int viewHeight = height - 2 * spacing;
                // Maximum offset from the top
                final int leftOver = totalHeightOfTheChildrens - viewHeight;

                scrollY = Math.min(leftOver, scrollY + scrollSpeed);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean scrollbarClicked;

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        scrollbarClicked = false;
        if(canScroll() && scrollbar.mouseClicked(mouseX, mouseY, button, scrollY)) {
            scrollbarClicked = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scrollbarClicked = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if(scrollbarClicked && canScroll()) {
            final int totalHeightOfTheChildrens = getTotalHeightOfChildren();
            int scrollDelta = scrollbar.getScrollOffsetDelta(deltaY, totalHeightOfTheChildrens);
            if (scrollY > 0 && scrollDelta > 0) {
                scrollY = Math.max(0, scrollY - scrollDelta);
            }
            if (canScrollDown() && scrollDelta < 0) {
                final int viewHeight = height - 2 * spacing;
                // Maximum offset from the top
                final int leftOver = totalHeightOfTheChildrens - viewHeight;

                scrollY = Math.min(leftOver, scrollY - scrollDelta);
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    /// Random implementation methods ///

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
    }

    @Override
    public SelectionType getType() {
        return SelectionType.NONE;
    }

    private static class Scrollbar {
        private final int spacing = 2;
        private final int width = 6;
        @SuppressWarnings("FieldCanBeLocal")
        private final int trackWidth = 2;
        private int x, height;
        private int trackX, trackY, trackHeight;
        private IntUnaryOperator scrollbarYGetter;

        void repositionScrollbar(int listX, int listY, int listWith, int listHeight, int listSpacing, int totalHeightOfTheChildrens) {
            this.x = listX + listWith - spacing - width;
            this.trackX = x + spacing;
            this.trackY = listY + listSpacing;
            this.trackHeight = listHeight - 2 * listSpacing;
            // Takes into account the fact that the scrollbar is offset from the track
            int scrollbarSpacedTrackHeight = trackHeight + 2 * spacing;

            this.scrollbarYGetter = scrollOffset -> MathHelper.ceil(scrollOffset * scrollbarSpacedTrackHeight / (float) totalHeightOfTheChildrens) + listY + spacing;
            this.height = (trackHeight * scrollbarSpacedTrackHeight) / totalHeightOfTheChildrens;
        }

        void render(DrawContext context, double mouseX, double mouseY, int scrollOffset, boolean clicked) {
            int y = scrollbarYGetter.applyAsInt(scrollOffset);
            context.fill(trackX, trackY, trackX + trackWidth, trackY + trackHeight, 0xFFFFFFFF);
            context.fill(x, y, x + width, y + height, isHovered(mouseX, mouseY, y) || clicked ? 0xFF6D6D6D : 0xFF1E1E1E);
        }

        boolean mouseClicked(double mouseX, double mouseY, double button, int scrollOffset) {
            return button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isHovered(mouseX, mouseY, scrollbarYGetter.applyAsInt(scrollOffset));
        }

        int getScrollOffsetDelta(double scrollbarDelta, double totalHeightOfTheChildrens) {
            int scrollbarSpacedTrackHeight = trackHeight + 2 * spacing;
            return MathHelper.ceil(-scrollbarDelta * totalHeightOfTheChildrens / (float) scrollbarSpacedTrackHeight);
        }

        private boolean isHovered(double mouseX, double mouseY, int y) {
            return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        }
    }
}

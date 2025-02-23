package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions;
import io.github.lgatodu47.screenshot_viewer.config.VisibilityState;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerTexts;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots.ManageScreenshotsScreen.*;

final class ScreenshotWidget extends ClickableWidget implements AutoCloseable, ScreenshotImageHolder {
    private final ManageScreenshotsScreen mainScreen;
    private final MinecraftClient client;
    private final Context ctx;

    private VisibilityState textVisibility;
    private float backgroundOpacityPercentage;
    private int textColor;
    private boolean renderTextShadow, promptOnDelete;
    private List<TooltipComponent> hintTooltip;

    private final ImageLoader screenshotImage = new ImageLoader("screenshot");
    private final ImageLoader thumbnailImage = new ImageLoader("thumbnail");
    private File screenshotFile;
    private boolean selectedForDeletion;
    private float hoverTime;
    private int baseY;

    public ScreenshotWidget(ManageScreenshotsScreen mainScreen, int x, int y, int width, int height, Context ctx, File screenshotFile) {
        super(x, y, width, height, Text.literal(screenshotFile.getName()));
        this.mainScreen = mainScreen;
        this.client = mainScreen.client();
        this.baseY = y;
        this.ctx = ctx;
        this.screenshotFile = screenshotFile;
        THUMBNAILS.getThumbnail(screenshotFile).ifPresentOrElse(thumbnailFile -> {
            this.screenshotImage.file = CompletableFuture.completedFuture(screenshotFile);
            this.thumbnailImage.load(thumbnailFile);
        }, () -> this.screenshotImage.load(CompletableFuture.completedFuture(screenshotFile)));
        onConfigUpdate();
    }

    void updateBaseY(int baseY) {
        setY(this.baseY = baseY);
    }

    void updateY(int scrollY) {
        setY(baseY - scrollY);
    }

    void deleteScreenshot() {
        close();
        screenshotImage.deleteFile();
        THUMBNAILS.removeThumbnail(screenshotFile);
        ctx.removeEntry(this);
    }

    void onConfigUpdate() {
        this.textVisibility = CONFIG.getOrFallback(ScreenshotViewerOptions.SCREENSHOT_ELEMENT_TEXT_VISIBILITY, VisibilityState.VISIBLE);
        this.backgroundOpacityPercentage = CONFIG.getOrFallback(ScreenshotViewerOptions.SCREENSHOT_ELEMENT_BACKGROUND_OPACITY, 100) / 100f;
        this.textColor = CONFIG.get(ScreenshotViewerOptions.SCREENSHOT_ELEMENT_TEXT_COLOR).map(TextColor::getRgb).orElse(0xFFFFFF);
        this.renderTextShadow = CONFIG.getOrFallback(ScreenshotViewerOptions.RENDER_SCREENSHOT_ELEMENT_FONT_SHADOW, true);
        this.promptOnDelete = CONFIG.getOrFallback(ScreenshotViewerOptions.PROMPT_WHEN_DELETING_SCREENSHOT, true);
        this.hintTooltip = CONFIG.getOrFallback(ScreenshotViewerOptions.DISPLAY_HINT_TOOLTIP, false) ? ScreenshotViewerUtils.toColoredComponents(client, ScreenshotViewerTexts.translatable("tooltip", "menu_hint").formatted(Formatting.GRAY)) : List.of();
    }

    void updateHoverState(int mouseX, int mouseY, int viewportY, int viewportBottom, boolean updateHoverState) {
        this.hovered = updateHoverState && (mouseX >= this.getX() && mouseY >= Math.max(this.getY(), viewportY) && mouseX < this.getX() + this.width && mouseY < Math.min(this.getY() + this.height, viewportBottom));
    }

    boolean isSelectedForDeletion() {
        return selectedForDeletion;
    }

    void deselectForDeletion() {
        this.selectedForDeletion = false;
    }

    /// Rendering Methods ///

    void render(DrawContext context, int mouseX, int mouseY, float partialTick, int viewportY, int viewportBottom) {
        this.hoverTime = hovered ? (hoverTime + partialTick) : 0;
        renderBackground(context, viewportY, viewportBottom);
        final int spacing = 2;

        NativeImageBackedTexture image = thumbnailTexture();
        if (image != null && image.getImage() != null) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShaderTexture(0, image.getGlId());
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(515);
            int renderY = Math.max(getY() + spacing, viewportY);
            int imgHeight = (int) (height / (VisibilityState.HIDDEN.equals(textVisibility) ? 1 : 1.08) - spacing * 3);
            int topOffset = Math.max(0, viewportY - getY() - spacing);
            int bottomOffset = Math.max(0, getY() + spacing + imgHeight - viewportBottom);
            int topV = topOffset * image.getImage().getHeight() / imgHeight;
            int bottomV = bottomOffset * image.getImage().getHeight() / imgHeight;

            context.getMatrices().translate(0, 0, 1);
            ScreenshotViewerUtils.drawTexture(
                    context,
                    getX() + spacing,
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
            if(mainScreen.isFastDeleteToggled() && selectedForDeletion) {
                context.fill(getX() + spacing, renderY, getX() + width - spacing, renderY + imgHeight - topOffset - bottomOffset, 0x50FF0000);
            }
            context.getMatrices().translate(0, 0, -1);
            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
        }

        if(VisibilityState.VISIBLE.equals(textVisibility) || VisibilityState.SHOW_ON_HOVER.equals(textVisibility) && hovered) {
            float scaleFactor = (float) (client.getWindow().getScaledHeight() / 96) / ctx.screenshotsPerRow();
            int textY = getY() + (int) (height / 1.08) - spacing;
            if (textY > viewportY && (float) textY + scaleFactor * (client.textRenderer.fontHeight) < viewportBottom) {
                MatrixStack matrices = context.getMatrices();
                matrices.push();
                matrices.translate(getX() + width / 2f, textY, 0);
                matrices.scale(scaleFactor, scaleFactor, scaleFactor);
                Text message = getMessage();
                float centerX = (float) (-client.textRenderer.getWidth(getMessage()) / 2);
                context.drawText(client.textRenderer, message, (int) centerX, 0, textColor, renderTextShadow);
                matrices.pop();
            }
        }

        if(!mainScreen.isFastDeleteToggled() && !hintTooltip.isEmpty() && hoverTime > 20) {
            RenderSystem.setShaderColor(1, 1, 1, Math.min(hoverTime - 20, 10) / 10 * 0.7f);
            ScreenshotViewerUtils.renderTooltip(context, client.textRenderer, hintTooltip, mouseX, mouseY);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private void renderBackground(DrawContext context, int viewportY, int viewportBottom) {
        int renderY = Math.max(getY(), viewportY);
        int renderHeight = Math.min(getY() + height, viewportBottom);
        context.fill(getX(), renderY, getX() + width, renderHeight, ColorHelper.getWhite((Math.min(hoverTime, 10) / 10) * backgroundOpacityPercentage));
    }

    /// Utility methods ///

    private void onClick() {
        if(mainScreen.isFastDeleteToggled()) {
            selectedForDeletion = !selectedForDeletion;
        } else {
            this.mainScreen.enlargeScreenshot(this);
        }
    }

    private void onRightClick(double mouseX, double mouseY) {
        this.mainScreen.showScreenshotProperties(mouseX, mouseY, this);
    }

    private void updateScreenshotFile(File screenshotFile) {
        THUMBNAILS.removeThumbnail(this.screenshotFile); // remove old thumbnail first
        this.screenshotFile = screenshotFile;
        THUMBNAILS.getThumbnail(screenshotFile).ifPresentOrElse(thumbnailFile -> {
            this.screenshotImage.close();
            this.screenshotImage.file = CompletableFuture.completedFuture(screenshotFile);
            this.thumbnailImage.setImage(thumbnailFile);
        }, () -> this.screenshotImage.setImage(CompletableFuture.completedFuture(screenshotFile)));
    }

    @Nullable
    public NativeImageBackedTexture thumbnailTexture() {
        if(!thumbnailImage.file.isDone()) {
            return screenshotImage.texture();
        }
        NativeImageBackedTexture texture = thumbnailImage.texture();
        return texture == null ? screenshotImage.texture() : texture;
    }

    /// ScreenshotImageHolder implementations ///

    @Override
    public File getScreenshotFile() {
        return screenshotFile;
    }

    @Override
    public void openFile() {
        Util.getOperatingSystem().open(screenshotFile);
    }

    @Override
    public void copyScreenshot() {
        ScreenshotViewerUtils.copyImageToClipboard(screenshotFile);
    }

    @Override
    public void requestFileDeletion() {
        BooleanConsumer deleteAction = value -> {
            if (value) {
                deleteScreenshot();
                mainScreen.enlargeScreenshot(null);
            }
            mainScreen.setDialogScreen(null);
        };

        if (promptOnDelete) {
            mainScreen.setDialogScreen(new ConfirmDeletionScreen(deleteAction, Text.translatable("screen." + ScreenshotViewer.MODID + ".delete_prompt", screenshotFile.getName()), ScreenshotViewerTexts.DELETE_WARNING_MESSAGE));
            return;
        }
        deleteAction.accept(true);
    }

    @Override
    public void renameFile() {
        String fileName = screenshotFile.getName();
        mainScreen.setDialogScreen(new RenameScreenshotScreen(fileName.substring(0, fileName.lastIndexOf('.')), s -> {
            try {
                Path moved = Files.move(screenshotFile.toPath(), screenshotFile.toPath().resolveSibling(s));
                updateScreenshotFile(moved.toFile());
            } catch (IOException e) {
                LOGGER.error("Failed to rename 'screenshot' file at '" + screenshotFile.toPath().toAbsolutePath() + "' from '" + screenshotFile.getName() + "' to '" + s + "'" , e);
            }
        }, () -> mainScreen.setDialogScreen(null)));
    }

    @Override
    public int indexInList() {
        return ctx.currentIndex(this);
    }

    @Override
    public int imageId() {
        NativeImageBackedTexture texture = screenshotImage.texture();
        return texture != null ? texture.getGlId() : 0;
    }

    @Nullable
    @Override
    public NativeImage image() {
        return screenshotImage.image();
    }

    /// Common Widget implementations ///

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(hovered && keyCode == InputUtil.GLFW_KEY_C && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            this.copyScreenshot();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public Text getMessage() {
        return this.screenshotFile == null ? super.getMessage() : Text.literal(this.screenshotFile.getName());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered()) {
            playDownSound(this.client.getSoundManager());
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                onClick();
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                onRightClick(mouseX, mouseY);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return isHovered();
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }

    @Override
    public void close() {
        this.screenshotImage.close();
        this.thumbnailImage.close();
    }

    interface Context {
        int screenshotsPerRow();

        int currentIndex(ScreenshotWidget widget);

        void removeEntry(ScreenshotWidget widget);
    }

    class ImageLoader implements AutoCloseable {
        private final String imageType;
        private CompletableFuture<File> file = new CompletableFuture<>();
        private CompletableFuture<NativeImage> image;
        @Nullable
        private NativeImageBackedTexture texture;

        ImageLoader(String imageType) {
            this.imageType = imageType;
        }

        public void load(CompletableFuture<File> file) {
            this.file = file;
            this.image = getImage();
        }

        public void setImage(CompletableFuture<File> file) {
            if(!this.file.isDone()) {
                this.file.cancel(true);
            }
            this.file = file;
            if (texture != null) {
                texture.close();
            } else if (image != null) {
                image.thenAcceptAsync(image -> {
                    if (image != null) {
                        image.close();
                    }
                }, client);
            }
            texture = null;
            image = getImage();
        }

        public void deleteFile() {
            File f = file.getNow(null);
            if (f != null && f.exists() && !f.delete()) {
                LOGGER.error("Failed to delete '{}' file at location '{}'", imageType, f.toPath().toAbsolutePath());
            }
        }

        private CompletableFuture<NativeImage> getImage() {
            return file.thenApplyAsync(file -> {
                try (InputStream inputStream = new FileInputStream(file)) {
                    return NativeImage.read(inputStream);
                } catch (FileNotFoundException e) {
                    // ignore thumbnails not found errors
                    if(imageType.equalsIgnoreCase("screenshot")) {
                        LOGGER.error("Could not find screenshot with name {}:", file.getName(), e);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to load {}: {}", imageType, file.getName(), e);
                }
                return null;
            }, Util.getMainWorkerExecutor());
        }

        @Nullable
        public NativeImageBackedTexture texture() {
            if (texture != null) {
                return texture;
            }
            if (image == null) {
                image = getImage();
            }
            NativeImage nativeImage;
            if (image.isDone() && (nativeImage = image.join()) != null) {
                return texture = new NativeImageBackedTexture(nativeImage);
            }
            return null;
        }

        @Nullable
        public NativeImage image() {
            if (image == null) {
                image = getImage();
            }
            return image.getNow(null);
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
                }, client);
            }
            image = null;
            texture = null;
        }
    }
}

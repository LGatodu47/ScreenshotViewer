package io.github.lgatodu47.screenshot_viewer.screens.manage_screenshots;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfig;
import io.github.lgatodu47.screenshot_viewer.config.VisibilityState;
import io.github.lgatodu47.screenshot_viewer.screens.ScreenshotViewerTexts;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.github.lgatodu47.screenshot_viewer.screens.manage_screenshots.ManageScreenshotsScreen.*;

final class ScreenshotWidget extends AbstractWidget implements AutoCloseable, ScreenshotImageHolder {
    private final ManageScreenshotsScreen mainScreen;
    private final Minecraft client;
    private final Context ctx;

    private VisibilityState textVisibility;
    private int backgroundColor;
    private int textColor;
    private boolean renderTextShadow, promptOnDelete;
    private List<ClientTooltipComponent> hintTooltip;

    private final ImageLoader screenshotImage = new ImageLoader("screenshot");
    private final ImageLoader thumbnailImage = new ImageLoader("thumbnail");
    private File screenshotFile;
    private boolean selectedForDeletion;
    private float hoverTime;
    private int baseY;

    public ScreenshotWidget(ManageScreenshotsScreen mainScreen, int x, int y, int width, int height, Context ctx, File screenshotFile) {
        super(x, y, width, height, Component.literal(screenshotFile.getName()));
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
        this.textVisibility = CONFIG.screenshotElementTextVisibility.get();
        this.backgroundColor = ScreenshotViewerConfig.colorFromHex(CONFIG.screenshotElementBackgroundColor.get()).orElse(0xFFFFFFFF);
        this.textColor = ScreenshotViewerConfig.colorFromHex(CONFIG.screenshotElementTextColor.get()).orElse(0xFFFFFFFF);
        this.renderTextShadow = CONFIG.renderScreenshotElementFontShadow.get();
        this.promptOnDelete = CONFIG.promptWhenDeletingScreenshot.get();
        this.hintTooltip = CONFIG.displayHintTooltip.get() ? ScreenshotViewerUtils.toColoredComponents(client, ScreenshotViewerTexts.translatable("tooltip", "menu_hint").withStyle(ChatFormatting.GRAY)) : List.of();
    }

    void updateHoverState(int mouseX, int mouseY, int viewportY, int viewportBottom, boolean updateHoverState) {
        this.isHovered = updateHoverState && (mouseX >= this.getX() && mouseY >= Math.max(this.getY(), viewportY) && mouseX < this.getX() + this.width && mouseY < Math.min(this.getY() + this.height, viewportBottom));
    }

    boolean isSelectedForDeletion() {
        return selectedForDeletion;
    }

    void deselectForDeletion() {
        this.selectedForDeletion = false;
    }

    /// Rendering Methods ///

    void render(GuiGraphics context, int mouseX, int mouseY, float partialTick, int viewportY, int viewportBottom) {
        this.hoverTime = isHovered ? (hoverTime + partialTick) : 0;
        renderBackground(context, viewportY, viewportBottom);
        final int spacing = 2;

        NativeImage image = thumbnailImage.image();
        if (image == null) {
            image = screenshotImage.image();
        }
        if (image != null) {
            int renderY = Math.max(getY() + spacing, viewportY);
            int imgHeight = (int) (height / (VisibilityState.HIDDEN.equals(textVisibility) ? 1 : 1.08) - spacing * 3);
            int topOffset = Math.max(0, viewportY - getY() - spacing);
            int bottomOffset = Math.max(0, getY() + spacing + imgHeight - viewportBottom);
            int topV = topOffset * image.getHeight() / imgHeight;
            int bottomV = bottomOffset * image.getHeight() / imgHeight;

            Identifier texture = thumbnailTextureId();
            if (texture != null) {
                ScreenshotViewerUtils.drawTexture(
                        context,
                        texture,
                        getX() + spacing,
                        renderY,
                        width - spacing * 2,
                        imgHeight - topOffset - bottomOffset,
                        0,
                        topV,
                        image.getWidth(),
                        image.getHeight() - topV - bottomV,
                        image.getWidth(),
                        image.getHeight()
                );
            }
            if(mainScreen.isFastDeleteToggled() && selectedForDeletion) {
                context.fill(getX() + spacing, renderY, getX() + width - spacing, renderY + imgHeight - topOffset - bottomOffset, 0x50FF0000);
            }
        }

        if(VisibilityState.VISIBLE.equals(textVisibility) || VisibilityState.SHOW_ON_HOVER.equals(textVisibility) && isHovered) {
            float scaleFactor = (float) (client.getWindow().getGuiScaledHeight() / 96) / ctx.screenshotsPerRow();
            int textY = getY() + (int) (height / 1.08) - spacing;
            if (textY > viewportY && (float) textY + scaleFactor * (client.font.lineHeight) < viewportBottom) {
                Matrix3x2fStack matrices = context.pose();
                matrices.pushMatrix();
                matrices.translate(getX() + width / 2f, textY);
                matrices.scale(scaleFactor, scaleFactor);
                Component message = getMessage();
                float centerX = (float) (-client.font.width(getMessage()) / 2);
                context.drawString(client.font, message, (int) centerX, 0, textColor, renderTextShadow);
                matrices.popMatrix();
            }
        }

        if(!mainScreen.isFastDeleteToggled() && !hintTooltip.isEmpty() && hoverTime > 20) {
            ScreenshotViewerUtils.renderCustomTooltip(context, client.font, hintTooltip, mouseX, mouseY, ARGB.white(Math.min(hoverTime - 20, 10) / 10 * 0.7f));
        }
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    private void renderBackground(GuiGraphics graphics, int viewportY, int viewportBottom) {
        int renderY = Math.max(getY(), viewportY);
        int renderHeight = Math.min(getY() + height, viewportBottom);
        graphics.fill(getX(), renderY, getX() + width, renderHeight, ARGB.color((int) ((Math.min(hoverTime, 10) / 10) * ARGB.alpha(backgroundColor)), ARGB.red(backgroundColor), ARGB.green(backgroundColor), ARGB.blue(backgroundColor)));
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
    private Identifier thumbnailTextureId() {
        if(!thumbnailImage.file.isDone()) {
            return screenshotImage.textureId();
        }
        Identifier texture = thumbnailImage.textureId();
        return texture == null ? screenshotImage.textureId() : texture;
    }

    /// ScreenshotImageHolder implementations ///

    @Override
    public File getScreenshotFile() {
        return screenshotFile;
    }

    @Override
    public void openFile() {
        Util.getPlatform().openFile(screenshotFile);
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
            mainScreen.setDialogScreen(new ConfirmDeletionScreen(deleteAction, Component.translatable("screen." + ScreenshotViewer.MODID + ".delete_prompt", screenshotFile.getName()), ScreenshotViewerTexts.DELETE_WARNING_MESSAGE));
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
                LOGGER.error("Failed to rename 'screenshot' file at '{}' from '{}' to '{}'", screenshotFile.toPath().toAbsolutePath(), screenshotFile.getName(), s, e);
            }
        }, () -> mainScreen.setDialogScreen(null)));
    }

    @Override
    public int indexInList() {
        return ctx.currentIndex(this);
    }

    @Override
    public @Nullable Identifier textureId() {
        return screenshotImage.textureId();
    }

    @Nullable
    @Override
    public NativeImage image() {
        return screenshotImage.image();
    }

    /// Common Widget implementations ///

    @Override
    public boolean keyPressed(@NonNull KeyEvent input) {
        if(isHovered && input.key() == InputConstants.KEY_C && (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            this.copyScreenshot();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public @NotNull Component getMessage() {
        return this.screenshotFile == null ? super.getMessage() : Component.literal(this.screenshotFile.getName());
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubled) {
        if (isHovered()) {
            playDownSound(this.client.getSoundManager());
            if (click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                onClick();
            }
            if (click.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
                onRightClick(click.x(), click.y());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isHoveredOrFocused() {
        return isHovered;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return isHoveredOrFocused();
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput p_259858_) {
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
        private Identifier textureId;

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
            if (textureId != null) {
                client.getTextureManager().release(textureId);
                textureId = null;
            } else if (image != null) {
                image.thenAcceptAsync(image -> {
                    if (image != null) {
                        image.close();
                    }
                }, client);
            }
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
            }, Util.backgroundExecutor());
        }

        @Nullable
        public Identifier textureId() {
            if (textureId != null) {
                return textureId;
            }
            if (image == null) {
                image = getImage();
            }
            NativeImage nativeImage;
            if (image.isDone() && (nativeImage = image.join()) != null) {
                File f = file.getNow(null);
                int hash = f != null ? java.util.Objects.hash(f.getAbsolutePath(), f.lastModified(), f.length()) : System.identityHashCode(nativeImage);
                textureId = Identifier.fromNamespaceAndPath(ScreenshotViewer.MODID, "dynamic/" + imageType.toLowerCase() + "/" + Integer.toHexString(hash));
                client.getTextureManager().register(textureId, new DynamicTexture(textureId::toString, nativeImage));
                return textureId;
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
            if (textureId != null) {
                client.getTextureManager().release(textureId);
                textureId = null;
            } else if(image != null) {
                image.thenAcceptAsync(image -> {
                    if (image != null) {
                        image.close();
                    }
                }, client);
            }
            image = null;
        }
    }
}

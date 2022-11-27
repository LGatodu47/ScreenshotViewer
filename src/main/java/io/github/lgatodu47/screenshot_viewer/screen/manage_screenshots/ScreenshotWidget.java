package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;

import static io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots.ManageScreenshotsScreen.LOGGER;

final class ScreenshotWidget extends ClickableWidget implements AutoCloseable, ScreenshotImageHolder {
    private final ManageScreenshotsScreen mainScreen;
    private final MinecraftClient client;
    private final Context ctx;

    private File screenshotFile;
    private CompletableFuture<NativeImage> image;
    @Nullable
    private NativeImageBackedTexture texture;
    private float bgOpacity = 0;
    private int baseY;

    public ScreenshotWidget(ManageScreenshotsScreen mainScreen, int x, int y, int width, int height, Context ctx, File screenshotFile) {
        super(x, y, width, height, Text.literal(screenshotFile.getName()));
        this.mainScreen = mainScreen;
        this.client = mainScreen.client();
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

    void setHeight(int height) {
        this.height = height;
    }

    void updateScreenshotFile(File screenshotFile) {
        this.screenshotFile = screenshotFile;
        if (texture != null) {
            texture.close();
        } else if (image != null) {
            image.thenAcceptAsync(image -> {
                if (image != null) {
                    image.close();
                }
            }, this.client);
        }
        texture = null;
        image = getImage(screenshotFile);
    }


    File getScreenshotFile() {
        return screenshotFile;
    }

    /// Rendering Methods ///

    void render(MatrixStack matrices, int mouseX, int mouseY, float delta, int viewportY, int viewportBottom, boolean updateHoverState) {
        this.hovered = updateHoverState && (mouseX >= this.x && mouseY >= Math.max(this.y, viewportY) && mouseX < this.x + this.width && mouseY < Math.min(this.y + this.height, viewportBottom));
        renderBackground(matrices, client, mouseX, mouseY, viewportY, viewportBottom);
        final int spacing = 2;

        NativeImageBackedTexture image = texture();
        if (image != null && image.getImage() != null) {
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
        if (textY > viewportY && textY + 8 < viewportBottom) {
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
        if (hovered) {
            if (bgOpacity < 1) {
                bgOpacity = Math.min(1, bgOpacity + 0.05F);
            }
        } else {
            if (bgOpacity > 0) {
                bgOpacity = Math.max(0, bgOpacity - 0.05F);
            }
        }
        int renderY = Math.max(y, viewportY);
        int renderHeight = Math.min(y + height, viewportBottom);
        DrawableHelper.fill(matrices, x, renderY, x + width, renderHeight, ColorHelper.Argb.getArgb((int) (bgOpacity * 255), 255, 255, 255));
    }

    /// Utility methods ///

    private void onClick() {
        this.mainScreen.enlargeScreenshot(this);
    }

    private void onRightClick(double mouseX, double mouseY) {
        this.mainScreen.showScreenshotProperties(mouseX, mouseY, this);
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

    /// ScreenshotImageHolder implementations ///

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

    /// Common Widget implementations ///

    @Override
    public Text getMessage() {
        return this.screenshotFile == null ? super.getMessage() : Text.literal(this.screenshotFile.getName());
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        soundManager.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
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

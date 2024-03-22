package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions;
import io.github.lgatodu47.screenshot_viewer.config.VisibilityState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;

import static io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots.ManageScreenshotsScreen.CONFIG;
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
        setY(this.baseY = baseY);
    }

    void updateY(int scrollY) {
        setY(baseY - scrollY);
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

    void updateHoverState(int mouseX, int mouseY, int viewportY, int viewportBottom, boolean updateHoverState) {
        this.hovered = updateHoverState && (mouseX >= this.getX() && mouseY >= Math.max(this.getY(), viewportY) && mouseX < this.getX() + this.width && mouseY < Math.min(this.getY() + this.height, viewportBottom));
        int maxOpacity = CONFIG.getOrFallback(ScreenshotViewerOptions.SCREENSHOT_ELEMENT_BACKGROUND_OPACITY, 100);
        if (maxOpacity > 0 && hovered) {
            if (bgOpacity < maxOpacity / 100f) {
                bgOpacity = Math.min(maxOpacity / 100f, bgOpacity + 0.05F);
            }
        } else {
            if (bgOpacity > 0) {
                bgOpacity = Math.max(0, bgOpacity - 0.05F);
            }
        }
    }

    /// Rendering Methods ///

    void render(DrawContext context, int mouseX, int mouseY, float delta, int viewportY, int viewportBottom) {
        renderBackground(context, mouseX, mouseY, viewportY, viewportBottom);
        final int spacing = 2;

        NativeImageBackedTexture image = texture();
        if (image != null && image.getImage() != null) {
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.setShaderTexture(0, image.getGlId());
            RenderSystem.enableBlend();
            int renderY = Math.max(getY() + spacing, viewportY);
            int imgHeight = (int) (height / 1.08 - spacing * 3);
            int topOffset = Math.max(0, viewportY - getY() - spacing);
            int bottomOffset = Math.max(0, getY() + spacing + imgHeight - viewportBottom);
            int topV = topOffset * image.getImage().getHeight() / imgHeight;
            int bottomV = bottomOffset * image.getImage().getHeight() / imgHeight;

            drawTexture(
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
            RenderSystem.disableBlend();
        }
        VisibilityState textVisibility = CONFIG.getOrFallback(ScreenshotViewerOptions.SCREENSHOT_ELEMENT_TEXT_VISIBILITY, VisibilityState.VISIBLE);
        if(VisibilityState.HIDDEN.equals(textVisibility) || VisibilityState.SHOW_ON_HOVER.equals(textVisibility) && !hovered) {
            return;
        }
        float scaleFactor = (float) (client.getWindow().getScaledHeight() / 96) / ctx.screenshotsPerRow();
        int textY = getY() + (int) (height / 1.08) - spacing;
        if (textY > viewportY && (float) textY + scaleFactor * (client.textRenderer.fontHeight) < viewportBottom) {
            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(getX() + width / 2f, textY, 0);
            matrices.scale(scaleFactor, scaleFactor, scaleFactor);
            Text message = getMessage();
            float centerX = (float) (-client.textRenderer.getWidth(getMessage()) / 2);
            int textColor = CONFIG.get(ScreenshotViewerOptions.SCREENSHOT_ELEMENT_TEXT_COLOR).map(TextColor::getRgb).orElse(0xFFFFFF);
            context.drawText(client.textRenderer, message, (int) centerX, 0, textColor, CONFIG.getOrFallback(ScreenshotViewerOptions.RENDER_SCREENSHOT_ELEMENT_FONT_SHADOW, true));
            matrices.pop();
        }
    }

    public static void drawTexture(DrawContext context, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        int x2 = x + width;
        int y2 = y + height;
        float u1 = u / (float) textureWidth;
        float u2 = (u + (float) regionWidth) / (float) textureWidth;
        float v1 = v / (float) textureHeight;
        float v2 = (v + (float) regionHeight) / (float) textureHeight;

        Matrix4f matrix4f = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix4f, x, y, 0).texture(u1, v1).next();
        bufferBuilder.vertex(matrix4f, x, y2, 0).texture(u1, v2).next();
        bufferBuilder.vertex(matrix4f, x2, y2, 0).texture(u2, v2).next();
        bufferBuilder.vertex(matrix4f, x2, y, 0).texture(u2, v1).next();
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private void renderBackground(DrawContext context, int mouseX, int mouseY, int viewportY, int viewportBottom) {
        int renderY = Math.max(getY(), viewportY);
        int renderHeight = Math.min(getY() + height, viewportBottom);
        context.fill(getX(), renderY, getX() + width, renderHeight, ColorHelper.Argb.getArgb((int) (bgOpacity * 255), 255, 255, 255));
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
        NativeImage nativeImage;
        if (image.isDone() && (nativeImage = image.join()) != null) {
            return texture = new NativeImageBackedTexture(nativeImage);
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
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
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

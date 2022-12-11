package io.github.lgatodu47.screenshot_viewer.screens;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.ColorHelper;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;

import static io.github.lgatodu47.screenshot_viewer.screens.ManageScreenshotsScreen.CONFIG;
import static io.github.lgatodu47.screenshot_viewer.screens.ManageScreenshotsScreen.LOGGER;

final class ScreenshotWidget extends Widget implements AutoCloseable, ScreenshotImageHolder {
    private final ManageScreenshotsScreen mainScreen;
    private final Minecraft client;
    private final Context ctx;

    private File screenshotFile;
    private CompletableFuture<NativeImage> image;
    @Nullable
    private DynamicTexture texture;
    private float bgOpacity = 0;
    private int baseY;

    public ScreenshotWidget(ManageScreenshotsScreen mainScreen, int x, int y, int width, int height, Context ctx, File screenshotFile) {
        super(x, y, width, height, new StringTextComponent(screenshotFile.getName()));
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
        this.isHovered = updateHoverState && (mouseX >= this.x && mouseY >= Math.max(this.y, viewportY) && mouseX < this.x + this.width && mouseY < Math.min(this.y + this.height, viewportBottom));
        int maxOpacity = CONFIG.screenshotElementBackgroundOpacity.get();
        if (maxOpacity > 0 && isHovered) {
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

    void render(MatrixStack matrices, int mouseX, int mouseY, float delta, int viewportY, int viewportBottom) {
        renderBackground(matrices, mouseX, mouseY, viewportY, viewportBottom);
        final int spacing = 2;

        DynamicTexture image = texture();
        if (image != null && image.getPixels() != null) {
            RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.bindTexture(image.getId());
            RenderSystem.enableBlend();
            int renderY = Math.max(y + spacing, viewportY);
            int imgHeight = (int) (height / 1.08 - spacing * 3);
            int topOffset = Math.max(0, viewportY - y - spacing);
            int bottomOffset = Math.max(0, y + spacing + imgHeight - viewportBottom);
            int topV = topOffset * image.getPixels().getHeight() / imgHeight;
            int bottomV = bottomOffset * image.getPixels().getHeight() / imgHeight;
            blit(matrices,
                    x + spacing,
                    renderY,
                    width - spacing * 2,
                    imgHeight - topOffset - bottomOffset,
                    0,
                    topV,
                    image.getPixels().getWidth(),
                    image.getPixels().getHeight() - topV - bottomV,
                    image.getPixels().getWidth(),
                    image.getPixels().getHeight()
            );
            RenderSystem.disableBlend();
        }
        float scaleFactor = (float) (client.getWindow().getGuiScaledHeight() / 96) / ctx.screenshotsPerRow();
        int textY = y + (int) (height / 1.08) - spacing;
        if (textY > viewportY && (float) textY + scaleFactor * (client.font.lineHeight) < viewportBottom) {
            matrices.pushPose();
            matrices.translate(x + width / 2f, textY, 0);
            matrices.scale(scaleFactor, scaleFactor, scaleFactor);
            ITextComponent message = getMessage();
            float centerX = (float) (-client.font.width(getMessage()) / 2);
            int textColor = Optional.ofNullable(Color.parseColor(CONFIG.screenshotElementTextColor.get())).map(Color::getValue).orElse(0xFFFFFF);
            if(CONFIG.renderScreenshotElementFontShadow.get()) {
                client.font.drawShadow(matrices, message, centerX, 0, textColor);
            } else {
                client.font.draw(matrices, message, centerX, 0, textColor);
            }
            matrices.popPose();
        }
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
    }

    private void renderBackground(MatrixStack matrices, int mouseX, int mouseY, int viewportY, int viewportBottom) {
        int renderY = Math.max(y, viewportY);
        int renderHeight = Math.min(y + height, viewportBottom);
        fill(matrices, x, renderY, x + width, renderHeight, ColorHelper.PackedColor.color((int) (bgOpacity * 255), 255, 255, 255));
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
            try (InputStream inputStream = Files.newInputStream(file.toPath())) {
                return NativeImage.read(inputStream);
            } catch (Exception e) {
                LOGGER.error("Failed to load screenshot: {}", file.getName(), e);
            }
            return null;
        }, Util.backgroundExecutor());
    }

    @Nullable
    public DynamicTexture texture() {
        if (texture != null) {
            return texture;
        }
        if (image == null) {
            image = getImage(screenshotFile);
        }
        if (image.isDone()) {
            return texture = new DynamicTexture(image.join());
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
        DynamicTexture texture = texture();
        return texture != null ? texture.getId() : 0;
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
    public ITextComponent getMessage() {
        return this.screenshotFile == null ? super.getMessage() : new StringTextComponent(this.screenshotFile.getName());
    }

    @Override
    public void playDownSound(SoundHandler soundManager) {
        soundManager.play(SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
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
        return isHovered;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return isHovered();
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

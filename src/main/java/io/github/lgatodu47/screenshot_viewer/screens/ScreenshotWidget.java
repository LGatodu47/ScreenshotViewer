package io.github.lgatodu47.screenshot_viewer.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.HttpUtil;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;

import static io.github.lgatodu47.screenshot_viewer.screens.ManageScreenshotsScreen.CONFIG;
import static io.github.lgatodu47.screenshot_viewer.screens.ManageScreenshotsScreen.LOGGER;

final class ScreenshotWidget extends GuiButton implements AutoCloseable, ScreenshotImageHolder {
    private final ManageScreenshotsScreen mainScreen;
    private final Minecraft client;
    private final Context ctx;

    private File screenshotFile;
    private CompletableFuture<BufferedImage> image;
    @Nullable
    private DynamicTexture texture;
    private float bgOpacity = 0;
    private int baseY;

    public ScreenshotWidget(ManageScreenshotsScreen mainScreen, int x, int y, int width, int height, Context ctx, File screenshotFile) {
        super(-1, x, y, width, height, screenshotFile.getName());
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
            texture.deleteGlTexture();
        }
        if (image != null) {
            image.thenAcceptAsync(image -> {
                if (image != null) {
                    image.flush();
                }
            }, HttpUtil.DOWNLOADER_EXECUTOR);
        }
        texture = null;
        image = getImage(screenshotFile);
    }

    File getScreenshotFile() {
        return screenshotFile;
    }

    void updateHoverState(int mouseX, int mouseY, int viewportY, int viewportBottom, boolean updateHoverState) {
        this.hovered = updateHoverState && (mouseX >= this.x && mouseY >= Math.max(this.y, viewportY) && mouseX < this.x + this.width && mouseY < Math.min(this.y + this.height, viewportBottom));
        int maxOpacity = CONFIG.screenshotElementBackgroundOpacity.getAsInt();
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

    void render(int viewportY, int viewportBottom) {
        renderBackground(viewportY, viewportBottom);
        final int spacing = 2;

        DynamicTexture image = texture();
        if (image != null) {
            int texWidth = getWidth(image);
            int texHeight = getHeight(image);

            int renderY = Math.max(y + spacing, viewportY);
            int imgHeight = (int) (height / 1.08 - spacing * 3);
            int topOffset = Math.max(0, viewportY - y - spacing);
            int bottomOffset = Math.max(0, y + spacing + imgHeight - viewportBottom);
            int topV = topOffset * texHeight / imgHeight;
            int bottomV = bottomOffset * texHeight / imgHeight;
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.bindTexture(image.getGlTextureId());
            GlStateManager.enableBlend();
            drawScaledCustomSizeModalRect(x + spacing,
                    renderY,
                    0,
                    topV,
                    texWidth,
                    texHeight - topV - bottomV,
                    width - spacing * 2,
                    imgHeight - topOffset - bottomOffset,
                    texWidth,
                    texHeight
            );
            GlStateManager.disableBlend();
        }
        float scaleFactor = (float) (new ScaledResolution(client).getScaledHeight() / 96) / ctx.screenshotsPerRow();
        int textY = y + (int) (height / 1.08) - spacing;
        if (textY > viewportY && (float) textY + scaleFactor * (client.fontRenderer.FONT_HEIGHT) < viewportBottom) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + width / 2f, textY, 0);
            GlStateManager.scale(scaleFactor, scaleFactor, scaleFactor);
            String message = getMessage();
            float centerX = (float) (-client.fontRenderer.getStringWidth(getMessage()) / 2);
            int textColor;
            try {
                textColor = Integer.parseInt(CONFIG.screenshotElementTextColor.get().substring(1), 16);
            } catch (Throwable t) {
                textColor = 0xFFFFFF;
            }
            if(CONFIG.renderScreenshotElementFontShadow.getAsBoolean()) {
                client.fontRenderer.drawStringWithShadow(message, centerX, 0, textColor);
            } else {
                client.fontRenderer.drawString(message, (int) centerX, 0, textColor);
            }
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void drawButton(@Nonnull Minecraft mc, int mouseX, int mouseY, float delta) {
    }

    private void renderBackground(int viewportY, int viewportBottom) {
        int renderY = Math.max(y, viewportY);
        int renderHeight = Math.min(y + height, viewportBottom);
        drawRect(x, renderY, x + width, renderHeight, (int) (bgOpacity * 255) << 24 | 0x00FFFFFF);
    }

    /// Utility methods ///

    private void onClick() {
        this.mainScreen.enlargeScreenshot(this);
    }

    private void onRightClick(double mouseX, double mouseY) {
        this.mainScreen.showScreenshotProperties(mouseX, mouseY, this);
    }

    private CompletableFuture<BufferedImage> getImage(File file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ImageIO.read(file);
            } catch (Exception e) {
                LOGGER.error("Failed to load screenshot: {}", file.getName(), e);
            }
            return null;
        }, HttpUtil.DOWNLOADER_EXECUTOR);
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
        return texture != null ? texture.getGlTextureId() : 0;
    }

    @Nullable
    @Override
    public BufferedImage image() {
        if (image == null) {
            image = getImage(screenshotFile);
        }
        return image.getNow(null);
    }

    /// Common Widget implementations ///

    public String getMessage() {
        return this.screenshotFile == null ? "" : this.screenshotFile.getName();
    }

    @Override
    public void playPressSound(SoundHandler soundManager) {
        soundManager.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (hovered) {
            playPressSound(this.client.getSoundHandler());
            if (button == 0) {
                onClick();
            }
            if (button == 1) {
                onRightClick(mouseX, mouseY);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mousePressed(@Nonnull Minecraft mc, int mouseX, int mouseY) {
        return false;
    }

    @Override
    public void close() {
        if (texture != null) {
            texture.deleteGlTexture();
        }
        if(image != null) {
            image.thenAcceptAsync(image -> {
                if (image != null) {
                    image.flush();
                }
            }, HttpUtil.DOWNLOADER_EXECUTOR);
        }
        image = null;
        texture = null;
    }

    private static Field WIDTH;

    private static int getWidth(DynamicTexture texture) {
        if(WIDTH == null) {
            WIDTH = ObfuscationReflectionHelper.findField(DynamicTexture.class, "field_94233_j");
        }
        try {
            return (int) WIDTH.get(texture);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Field HEIGHT;

    private static int getHeight(DynamicTexture texture) {
        if(HEIGHT == null) {
            HEIGHT = ObfuscationReflectionHelper.findField(DynamicTexture.class, "field_94233_j");
        }
        try {
            return (int) HEIGHT.get(texture);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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

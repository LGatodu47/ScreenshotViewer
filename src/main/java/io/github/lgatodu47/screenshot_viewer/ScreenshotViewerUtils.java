package io.github.lgatodu47.screenshot_viewer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import io.github.lgatodu47.screenshot_viewer.screens.CopyScreenshotToast;
import io.github.lgatodu47.screenshot_viewer.screens.ScreenshotViewerTexts;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ScreenshotViewerUtils {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    private static final Clipboard AWT_CLIPBOARD = tryGetAWTClipboard();

    public static File getVanillaScreenshotsFolder() {
        return new File(Minecraft.getInstance().gameDirectory, "screenshots");
    }

    public static void drawTexture(GuiGraphics context, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        int x2 = x + width;
        int y2 = y + height;
        float u1 = u / (float) textureWidth;
        float u2 = (u + (float) regionWidth) / (float) textureWidth;
        float v1 = v / (float) textureHeight;
        float v2 = (v + (float) regionHeight) / (float) textureHeight;

        Matrix4f matrix4f = context.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix4f, x, y, 0).uv(u1, v1).endVertex();
        bufferBuilder.vertex(matrix4f, x, y2, 0).uv(u1, v2).endVertex();
        bufferBuilder.vertex(matrix4f, x2, y2, 0).uv(u2, v2).endVertex();
        bufferBuilder.vertex(matrix4f, x2, y, 0).uv(u2, v1).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    @Nullable
    private static Clipboard tryGetAWTClipboard() {
        if(Minecraft.ON_OSX) {
            return null;
        }
        try {
            return Toolkit.getDefaultToolkit().getSystemClipboard();
        } catch (Throwable t) {
            LOGGER.error("Unable to retrieve Java AWT Clipboard instance!", t);
        }
        return null;
    }

    public static void copyImageToClipboard(File screenshotFile) {
        if(Minecraft.ON_OSX) {
            ScreenshotViewerMacOsUtils.doCopyMacOS(screenshotFile.getAbsolutePath());
            return;
        }
        if(AWT_CLIPBOARD != null && screenshotFile.exists()) {
            CompletableFuture.runAsync(() -> {
                Component toastText;
                try {
                    BufferedImage img = ImageIO.read(screenshotFile);
                    BufferedImage rgbImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
                    rgbImg.createGraphics().drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
                    ImageTransferable imageTransferable = new ImageTransferable(rgbImg);
                    AWT_CLIPBOARD.setContents(imageTransferable, null);
                    toastText = ScreenshotViewerTexts.TOAST_COPY_SUCCESS;
                } catch (Throwable t) {
                    LOGGER.error("Failed to copy screenshot image to clipboard!", t);
                    toastText = ScreenshotViewerTexts.translatable("toast", "copy_fail", t.getClass().getSimpleName());
                }

                Minecraft client = Minecraft.getInstance();
                CopyScreenshotToast.show(client.getToasts(), toastText, Component.literal(screenshotFile.getName()), 3000L);
            }, Util.backgroundExecutor());
        }
    }

    public static List<ClientTooltipComponent> toColoredComponents(Minecraft client, Component text) {
        return Tooltip.splitTooltip(client, text).stream().map(ColoredTooltipComponent::new).collect(Collectors.toList());
    }

    private static Method DRAW_TOOLTIP;
    private static boolean errorLogged;

    public static void renderTooltip(GuiGraphics context, Font textRenderer, List<ClientTooltipComponent> tooltipComponents, int posX, int posY) {
        if(DRAW_TOOLTIP == null) {
            DRAW_TOOLTIP = ObfuscationReflectionHelper.findMethod(GuiGraphics.class, "m_280497_", Font.class, List.class, int.class, int.class, ClientTooltipPositioner.class);
            DRAW_TOOLTIP.setAccessible(true);
        }
        try {
            DRAW_TOOLTIP.invoke(context, textRenderer, tooltipComponents, posX, posY, DefaultTooltipPositioner.INSTANCE);
        } catch (Exception e) {
            if(!errorLogged) {
                LOGGER.error("Failed to render Screenshot Viewer tooltip", e);
                errorLogged = true;
            }
        }
    }

    public static void forEachDrawable(Screen screen, Consumer<Renderable> renderer) {
        forEachOfType(screen, Renderable.class, renderer);
    }

    public static <T> void forEachOfType(Screen screen, Class<T> type, Consumer<T> action) {
        screen.children().stream().filter(type::isInstance).map(type::cast).forEachOrdered(action);
    }

    static class ColoredTooltipComponent implements ClientTooltipComponent {
        private final FormattedCharSequence text;

        public ColoredTooltipComponent(FormattedCharSequence text) {
            this.text = text;
        }

        @Override
        public int getWidth(Font textRenderer) {
            return textRenderer.width(this.text);
        }

        @Override
        public int getHeight() {
            return 10;
        }

        @Override
        public void renderText(@NotNull Font textRenderer, int x, int y, @NotNull Matrix4f matrix, MultiBufferSource.@NotNull BufferSource vertexConsumers) {
            float[] colors = RenderSystem.getShaderColor();
            if(colors.length != 4) {
                colors = new float[]{0, 0, 0, 0};
            }
            // game tweaks the alpha value for some reason (see TextRenderer#tweakTransparency)
            int alpha = Math.max((int) (colors[3] * 255), 5);
            int textColor = FastColor.ARGB32.color(alpha, (int) (colors[0] * 255), (int) (colors[1] * 255), (int) (colors[2] * 255));
            textRenderer.drawInBatch(this.text, x, y, textColor, true, matrix, vertexConsumers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        }
    }

    record ImageTransferable(Image image) implements Transferable {
        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] {DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @NotNull
        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if(!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image();
        }
    }
}

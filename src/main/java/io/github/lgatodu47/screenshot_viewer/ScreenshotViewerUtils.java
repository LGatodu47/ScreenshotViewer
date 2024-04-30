package io.github.lgatodu47.screenshot_viewer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.render.*;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
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
        return new File(MinecraftClient.getInstance().runDirectory, "screenshots");
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

    @Nullable
    private static Clipboard tryGetAWTClipboard() {
        if(MinecraftClient.IS_SYSTEM_MAC) {
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
        if(MinecraftClient.IS_SYSTEM_MAC) {
            ScreenshotViewerMacOsUtils.doCopyMacOS(screenshotFile.getAbsolutePath());
            return;
        }
        if(AWT_CLIPBOARD != null && screenshotFile.exists()) {
            CompletableFuture.runAsync(() -> {
                try {
                    BufferedImage img = ImageIO.read(screenshotFile);
                    BufferedImage rgbImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
                    rgbImg.createGraphics().drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
                    ImageTransferable imageTransferable = new ImageTransferable(rgbImg);
                    AWT_CLIPBOARD.setContents(imageTransferable, null);
                } catch (Throwable t) {
                    LOGGER.error("Failed to copy screenshot image to clipboard!", t);
                }
            }, Util.getMainWorkerExecutor());
        }
    }

    public static List<TooltipComponent> toColoredComponents(MinecraftClient client, Text text) {
        return Tooltip.wrapLines(client, text).stream().map(ColoredTooltipComponent::new).collect(Collectors.toList());
    }

    private static Method DRAW_TOOLTIP;
    private static boolean errorLogged;

    public static void renderTooltip(DrawContext context, TextRenderer textRenderer, List<TooltipComponent> tooltipComponents, int posX, int posY) {
        if(DRAW_TOOLTIP == null) {
            try {
                MappingResolver mappingResolver = FabricLoader.getInstance().getMappingResolver();
                String methodName = mappingResolver.mapMethodName("intermediary", "net.minecraft.class_332", "method_51435", "(Lnet/minecraft/class_327;Ljava/util/List;IILnet/minecraft/class_8000;)V");
                DRAW_TOOLTIP = DrawContext.class.getDeclaredMethod(methodName, TextRenderer.class, List.class, int.class, int.class, TooltipPositioner.class);
            } catch (NoSuchMethodException e) {
                if(!errorLogged) {
                    LOGGER.error("Failed to render Screenshot Viewer tooltip", e);
                    errorLogged = true;
                }
                return;
            }
            DRAW_TOOLTIP.setAccessible(true);
        }
        try {
            DRAW_TOOLTIP.invoke(context, textRenderer, tooltipComponents, posX, posY, HoveredTooltipPositioner.INSTANCE);
        } catch (Exception e) {
            if(!errorLogged) {
                LOGGER.error("Failed to render Screenshot Viewer tooltip", e);
                errorLogged = true;
            }
        }
    }

    public static void forEachDrawable(Screen screen, Consumer<Drawable> renderer) {
        forEachOfType(screen, Drawable.class, renderer);
    }

    public static <T> void forEachOfType(Screen screen, Class<T> type, Consumer<T> action) {
        screen.children().stream().filter(type::isInstance).map(type::cast).forEachOrdered(action);
    }

    static class ColoredTooltipComponent implements TooltipComponent {
        private final OrderedText text;

        public ColoredTooltipComponent(OrderedText text) {
            this.text = text;
        }

        @Override
        public int getWidth(TextRenderer textRenderer) {
            return textRenderer.getWidth(this.text);
        }

        @Override
        public int getHeight() {
            return 10;
        }

        @Override
        public void drawText(TextRenderer textRenderer, int x, int y, Matrix4f matrix, VertexConsumerProvider.Immediate vertexConsumers) {
            float[] colors = RenderSystem.getShaderColor();
            if(colors.length != 4) {
                colors = new float[]{0, 0, 0, 0};
            }
            // game tweaks the alpha value for some reason (see TextRenderer#tweakTransparency)
            int alpha = Math.max((int) (colors[3] * 255), 5);
            int textColor = ColorHelper.Argb.getArgb(alpha, (int) (colors[0] * 255), (int) (colors[1] * 255), (int) (colors[2] * 255));
            textRenderer.draw(this.text, x, y, textColor, true, matrix, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
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

package io.github.lgatodu47.screenshot_viewer;

import com.mojang.logging.LogUtils;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2ic;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ScreenshotViewerUtils {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    private static final Clipboard AWT_CLIPBOARD = tryGetAWTClipboard();
    private static final SystemToast.SystemToastId COPY_SCREENSHOT = new SystemToast.SystemToastId(3000);

    public static File getVanillaScreenshotsFolder() {
        return new File(Minecraft.getInstance().gameDirectory, "screenshots");
    }

    public static File getDefaultThumbnailFolder() {
        return new File(Minecraft.getInstance().gameDirectory, "screenshots/thumbnails");
    }

    public static List<File> getScreenshotFiles(File screenshotsFolder) {
        File[] files = screenshotsFolder.listFiles();
        if(files == null) {
            return List.of();
        }
        return Arrays.stream(files).filter(file -> file.isFile() && (file.getName().endsWith(".png") || file.getName().endsWith(".jpg") || file.getName().endsWith(".jpeg"))).collect(Collectors.toList());
    }

    public static void drawTexture(GuiGraphicsExtractor context, Identifier texture, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, (float)u, (float)v, width, height, regionWidth, regionHeight, textureWidth, textureHeight);
    }
    @Nullable
    private static Clipboard tryGetAWTClipboard() {
        if(Util.getPlatform() == Util.OS.OSX) {
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
        if(Util.getPlatform() == Util.OS.OSX) {
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
                SystemToast.addOrUpdate(client.gui.toastManager(), COPY_SCREENSHOT, toastText, Component.literal(screenshotFile.getName()));
            }, Util.backgroundExecutor());
        }
    }

    public static List<ClientTooltipComponent> toColoredComponents(Minecraft client, Component text) {
        return Tooltip.splitTooltip(client, text).stream().map(ColoredTooltipComponents::new).collect(Collectors.toList());
    }

    private static Field TOOLTIP_DRAWER_FIELD;

    public static void renderCustomTooltip(GuiGraphicsExtractor context, Font textRenderer, List<ClientTooltipComponent> text, int posX, int posY, int color) {
        if(TOOLTIP_DRAWER_FIELD == null) {
            try {
                TOOLTIP_DRAWER_FIELD = GuiGraphicsExtractor.class.getDeclaredField("deferredTooltip");
                TOOLTIP_DRAWER_FIELD.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            Object tooltipDrawer = TOOLTIP_DRAWER_FIELD.get(context);
            if(tooltipDrawer == null) {
                TOOLTIP_DRAWER_FIELD.set(context, (Runnable) () -> drawCustomTooltip(context, textRenderer, text, posX, posY, color));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Identifier DEFAULT_TOOLTIP_BACKGROUND_TEXTURE = Identifier.withDefaultNamespace("tooltip/background");
    private static final Identifier DEFAULT_TOOLTIP_FRAME_TEXTURE = Identifier.withDefaultNamespace("tooltip/frame");

    private static void drawCustomTooltip(GuiGraphicsExtractor context, Font textRenderer, List<ClientTooltipComponent> text, int posX, int posY, int color) {
        int totWidth = 0;
        int totHeight = text.size() == 1 ? -2 : 0;

        for (ClientTooltipComponent comp : text) {
            int compWidth = comp.getWidth(textRenderer);
            if (compWidth > totWidth) {
                totWidth = compWidth;
            }

            totHeight += comp.getHeight(textRenderer);
        }

        ClientTooltipPositioner positioner = DefaultTooltipPositioner.INSTANCE;
        Vector2ic vector2ic = positioner.positionTooltip(context.guiWidth(), context.guiHeight(), posX, posY, totWidth, totHeight);
        int x = vector2ic.x();
        int y = vector2ic.y();
        context.pose().pushMatrix();

        int bgX = x - 3 - 9;
        int bgY = y - 3 - 9;
        int bgWidth = totWidth + 3 + 3 + 18;
        int bgHeight = totHeight + 3 + 3 + 18;
        context.blitSprite(RenderPipelines.GUI_TEXTURED, DEFAULT_TOOLTIP_BACKGROUND_TEXTURE, bgX, bgY, bgWidth, bgHeight, color);
        context.blitSprite(RenderPipelines.GUI_TEXTURED, DEFAULT_TOOLTIP_FRAME_TEXTURE, bgX, bgY, bgWidth, bgHeight, color);

        int drawY = y;

        for (int q = 0; q < text.size(); q++) {
            ClientTooltipComponent comp = text.get(q);
            if(comp instanceof ColoredTooltipComponents alpha) {
                alpha.drawColoredText(context, textRenderer, x, drawY, color);
            } else {
                comp.extractText(context, textRenderer, x, drawY);
            }
            drawY += comp.getHeight(textRenderer) + (q == 0 ? 2 : 0);
        }

        drawY = y;

        for (int q = 0; q < text.size(); q++) {
            ClientTooltipComponent comp = text.get(q);
            comp.extractImage(textRenderer, x, drawY, totWidth, totHeight, context);
            drawY += comp.getHeight(textRenderer) + (q == 0 ? 2 : 0);
        }

        context.pose().popMatrix();
    }

    public static void renderWidget(AbstractWidget widget, GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        widget.extractRenderState(context, mouseX, mouseY, delta);
    }

    public static void forEachDrawable(Screen screen, Consumer<Renderable> renderer) {
        forEachOfType(screen, Renderable.class, renderer);
    }

    public static <T> void forEachOfType(Screen screen, Class<T> type, Consumer<T> action) {
        screen.children().stream().filter(type::isInstance).map(type::cast).forEachOrdered(action);
    }

    static class ColoredTooltipComponents implements ClientTooltipComponent {
        private final FormattedCharSequence text;

        public ColoredTooltipComponents(FormattedCharSequence text) {
            this.text = text;
        }

        @Override
        public int getWidth(Font textRenderer) {
            return textRenderer.width(this.text);
        }

        @Override
        public int getHeight(Font textRenderer) {
            return 10;
        }

        @Override
        public void extractText(GuiGraphicsExtractor context, Font textRenderer, int x, int y) {
            context.text(textRenderer, this.text, x, y, -1, true);
        }

        public void drawColoredText(GuiGraphicsExtractor context, Font textRenderer, int x, int y, int color) {
            context.text(textRenderer, this.text, x, y, color, true);
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

    public static Component ofSupplied(Supplier<Component> textSupplier) {
        return MutableComponent.create(new ClientSideSuppliedTextContent(textSupplier));
    }

    record ClientSideSuppliedTextContent(@NotNull Supplier<Component> s) implements PlainTextContents {
        @Override
        public String text() {
            return "";
        }

        @Override
        public <T> Optional<T> visit(FormattedText.ContentConsumer<T> visitor) {
            Component r = s.get();
            return r == null ? Optional.empty() : r.visit(visitor);
        }

        @Override
        public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> visitor, Style style) {
            Component r = s.get();
            return r == null ? Optional.empty() : r.visit(visitor, style);
        }

        @Override
        public String toString() {
            return "clientsideSupplied{}";
        }
    }
}

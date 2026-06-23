package io.github.lgatodu47.screenshot_viewer;

import com.mojang.logging.LogUtils;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerTexts;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.*;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
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
    private static final SystemToast.Type COPY_SCREENSHOT = new SystemToast.Type(3000);

    public static File getVanillaScreenshotsFolder() {
        return new File(MinecraftClient.getInstance().runDirectory, "screenshots");
    }

    public static File getDefaultThumbnailFolder() {
        return new File(MinecraftClient.getInstance().runDirectory, "screenshots/thumbnails");
    }

    public static List<File> getScreenshotFiles(File screenshotsFolder) {
        File[] files = screenshotsFolder.listFiles();
        if(files == null) {
            return List.of();
        }
        return Arrays.stream(files).filter(file -> file.isFile() && (file.getName().endsWith(".png") || file.getName().endsWith(".jpg") || file.getName().endsWith(".jpeg"))).collect(Collectors.toList());
    }

    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, (float)u, (float)v, width, height, regionWidth, regionHeight, textureWidth, textureHeight);
    }
    @Nullable
    private static Clipboard tryGetAWTClipboard() {
        if(Util.getOperatingSystem() == Util.OperatingSystem.OSX) {
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
        if(Util.getOperatingSystem() == Util.OperatingSystem.OSX) {
            ScreenshotViewerMacOsUtils.doCopyMacOS(screenshotFile.getAbsolutePath());
            return;
        }
        if(AWT_CLIPBOARD != null && screenshotFile.exists()) {
            CompletableFuture.runAsync(() -> {
                Text toastText;
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

                MinecraftClient client = MinecraftClient.getInstance();
                if(client != null) {
                    SystemToast.show(client.getToastManager(), COPY_SCREENSHOT, toastText, Text.literal(screenshotFile.getName()));
                }
            }, Util.getMainWorkerExecutor());
        }
    }

    public static List<TooltipComponent> toColoredComponents(MinecraftClient client, Text text) {
        return Tooltip.wrapLines(client, text).stream().map(ColoredTooltipComponents::new).collect(Collectors.toList());
    }

    private static Field TOOLTIP_DRAWER_FIELD;

    public static void renderCustomTooltip(DrawContext context, TextRenderer textRenderer, List<TooltipComponent> text, int posX, int posY, int color) {
        if(TOOLTIP_DRAWER_FIELD == null) {
            try {
                String fieldName = FabricLoader.getInstance().getMappingResolver().mapFieldName("intermediary", "net.minecraft.class_332", "field_60305", "Ljava/lang/Runnable;");
                TOOLTIP_DRAWER_FIELD = DrawContext.class.getDeclaredField(fieldName);
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

    private static final Identifier DEFAULT_TOOLTIP_BACKGROUND_TEXTURE = Identifier.ofVanilla("tooltip/background");
    private static final Identifier DEFAULT_TOOLTIP_FRAME_TEXTURE = Identifier.ofVanilla("tooltip/frame");

    private static void drawCustomTooltip(DrawContext context, TextRenderer textRenderer, List<TooltipComponent> text, int posX, int posY, int color) {
        int totWidth = 0;
        int totHeight = text.size() == 1 ? -2 : 0;

        for (TooltipComponent comp : text) {
            int compWidth = comp.getWidth(textRenderer);
            if (compWidth > totWidth) {
                totWidth = compWidth;
            }

            totHeight += comp.getHeight(textRenderer);
        }

        TooltipPositioner positioner = HoveredTooltipPositioner.INSTANCE;
        Vector2ic vector2ic = positioner.getPosition(context.getScaledWindowWidth(), context.getScaledWindowHeight(), posX, posY, totWidth, totHeight);
        int x = vector2ic.x();
        int y = vector2ic.y();
        context.getMatrices().pushMatrix();

        int bgX = x - 3 - 9;
        int bgY = y - 3 - 9;
        int bgWidth = totWidth + 3 + 3 + 18;
        int bgHeight = totHeight + 3 + 3 + 18;
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, DEFAULT_TOOLTIP_BACKGROUND_TEXTURE, bgX, bgY, bgWidth, bgHeight, color);
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, DEFAULT_TOOLTIP_FRAME_TEXTURE, bgX, bgY, bgWidth, bgHeight, color);

        int drawY = y;

        for (int q = 0; q < text.size(); q++) {
            TooltipComponent comp = text.get(q);
            if(comp instanceof ColoredTooltipComponents alpha) {
                alpha.drawColoredText(context, textRenderer, x, drawY, color);
            } else {
                comp.drawText(context, textRenderer, x, drawY);
            }
            drawY += comp.getHeight(textRenderer) + (q == 0 ? 2 : 0);
        }

        drawY = y;

        for (int q = 0; q < text.size(); q++) {
            TooltipComponent comp = text.get(q);
            comp.drawItems(textRenderer, x, drawY, totWidth, totHeight, context);
            drawY += comp.getHeight(textRenderer) + (q == 0 ? 2 : 0);
        }

        context.getMatrices().popMatrix();
    }

    public static void renderWidget(ClickableWidget widget, DrawContext context, int mouseX, int mouseY, float delta) {
        widget.render(context, mouseX, mouseY, delta);
    }

    public static void forEachDrawable(Screen screen, Consumer<Drawable> renderer) {
        forEachOfType(screen, Drawable.class, renderer);
    }

    public static <T> void forEachOfType(Screen screen, Class<T> type, Consumer<T> action) {
        screen.children().stream().filter(type::isInstance).map(type::cast).forEachOrdered(action);
    }

    static class ColoredTooltipComponents implements TooltipComponent {
        private final OrderedText text;

        public ColoredTooltipComponents(OrderedText text) {
            this.text = text;
        }

        @Override
        public int getWidth(TextRenderer textRenderer) {
            return textRenderer.getWidth(this.text);
        }

        @Override
        public int getHeight(TextRenderer textRenderer) {
            return 10;
        }

        @Override
        public void drawText(DrawContext context, TextRenderer textRenderer, int x, int y) {
            context.drawText(textRenderer, this.text, x, y, -1, true);
        }

        public void drawColoredText(DrawContext context, TextRenderer textRenderer, int x, int y, int color) {
            context.drawText(textRenderer, this.text, x, y, color, true);
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

    public static Text ofSupplied(Supplier<Text> textSupplier) {
        return MutableText.of(new ClientSideSuppliedTextContent(textSupplier));
    }

    record ClientSideSuppliedTextContent(@NotNull Supplier<Text> s) implements PlainTextContent {
        @Override
        public String string() {
            return "";
        }

        @Override
        public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
            Text r = s.get();
            return r == null ? Optional.empty() : r.visit(visitor);
        }

        @Override
        public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
            Text r = s.get();
            return r == null ? Optional.empty() : r.visit(visitor, style);
        }

        @Override
        public String toString() {
            return "clientsideSupplied{}";
        }
    }
}

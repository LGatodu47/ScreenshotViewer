package io.github.lgatodu47.screenshot_viewer;

import com.mojang.logging.LogUtils;
import io.github.lgatodu47.catconfig.CatConfig;
import io.github.lgatodu47.catconfigmc.screen.ConfigListener;
import io.github.lgatodu47.screenshot_viewer.config.CompressionRatio;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class ScreenshotThumbnailManager implements ConfigListener {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final CatConfig config;
    private final Executor executor;
    private final Map<File, CompletableFuture<File>> screenshotToThumbnail = new HashMap<>();
    private File screenshotFolder, thumbnailFolder;
    private CompressionRatio ratio = CompressionRatio.NONE;

    public ScreenshotThumbnailManager(CatConfig config) {
        this.config = config;
        this.executor = Util.getMainWorkerExecutor();
        configUpdated();
    }

    @Override
    public void configUpdated() {
        File screenshotFolder = config.getOrFallback(ScreenshotViewerOptions.SCREENSHOTS_FOLDER, (Supplier<? extends File>) ScreenshotViewerUtils::getVanillaScreenshotsFolder);
        File thumbnailFolder = config.getOrFallback(ScreenshotViewerOptions.THUMBNAIL_FOLDER, (Supplier<? extends File>) ScreenshotViewerUtils::getDefaultThumbnailFolder);
        CompressionRatio ratio = config.getOrFallback(ScreenshotViewerOptions.COMPRESSION_RATIO, CompressionRatio.NONE);
        if(screenshotFolder == this.screenshotFolder && thumbnailFolder == this.thumbnailFolder && ratio == this.ratio) {
            return;
        }
        if(screenshotFolder.equals(thumbnailFolder)) {
            LOGGER.warn("Screenshots and thumbnails cannot be stored in the same folder: `{}`! Please disable compression by setting the compression ratio to `NONE`!", screenshotFolder);
            thumbnailFolder = new File(screenshotFolder, "thumbnails");
            config.put(ScreenshotViewerOptions.THUMBNAIL_FOLDER, thumbnailFolder);
        }
        boolean regenerate = ratio != this.ratio;
        this.screenshotFolder = screenshotFolder;
        this.thumbnailFolder = thumbnailFolder;
        this.ratio = ratio;
        reloadThumbnails(regenerate);
    }

    /**
     * @param screenshotFile The file to generate a thumbnail of.
     * @return An optional holding a file only a thumbnail was generated.
     */
    public Optional<CompletableFuture<File>> getThumbnail(File screenshotFile) {
        return Optional.ofNullable(screenshotToThumbnail.compute(screenshotFile, (stored, value) -> {
            if(ratio == CompressionRatio.NONE) {
                return null;
            }
            // if there's no value or the stored key is incorrect
            if(value == null || screenshotFile.length() != stored.length()) {
                return CompletableFuture.supplyAsync(() -> generateThumbnail(screenshotFile), this.executor);
            }
            return value;
        }));
    }

    public void removeThumbnail(File screenshotFile) {
        deleteThumbnailFile(screenshotToThumbnail.remove(screenshotFile));
    }

    private void reloadThumbnails(boolean regenerate) {
        List<File> screenshotFiles = ScreenshotViewerUtils.getScreenshotFiles(screenshotFolder);

        // clear thumbnails if necessary
        boolean noThumbs = ratio == CompressionRatio.NONE || screenshotFiles.isEmpty();
        if(noThumbs || regenerate) {
            screenshotToThumbnail.values().forEach(this::deleteThumbnailFile);
            screenshotToThumbnail.clear();

            if(noThumbs) {
                return;
            }
        }

        if(!thumbnailFolder.exists()) {
            try {
                Files.createDirectories(thumbnailFolder.toPath());
            } catch (IOException e) {
                LOGGER.error("Failed to create thumbnails folder!", e);
            }
        }

        // remove thumbnails that no longer exist
        if(!screenshotToThumbnail.isEmpty()) {
            List<Map.Entry<File, CompletableFuture<File>>> toRemove = screenshotToThumbnail.entrySet().stream().filter(e -> !screenshotFiles.contains(e.getKey())).toList();
            for (Map.Entry<File, CompletableFuture<File>> entry : toRemove) {
                deleteThumbnailFile(entry.getValue());
                screenshotToThumbnail.remove(entry.getKey());
            }
        }

        for (File file : screenshotFiles) {
            screenshotToThumbnail.compute(file, (stored, value) -> {
                // if there's no value or the stored key is incorrect
                if(value == null || file.length() != stored.length()) {
                    return CompletableFuture.supplyAsync(() -> generateThumbnail(file), this.executor);
                }
                return value;
            });
        }
    }

    @Nullable
    private File generateThumbnail(File screenshotFile) {
        // Obtaining image input stream.
        ImageInputStream stream;
        try {
            stream = ImageIO.createImageInputStream(screenshotFile);
        } catch (FileNotFoundException e) {
            LOGGER.error("Could not find file with location `{}`: how did we get here?", screenshotFile, e);
            return null;
        } catch (IOException e) {
            LOGGER.error("Failed to read screenshot file `{}`:", screenshotFile, e);
            return null;
        }
        if(stream == null) {
            return null;
        }
        Runnable closure = () -> {
            try {
                stream.close();
            } catch (IOException e) {
                LOGGER.error("IO error when trying to close Image Input Stream.", e);
            }
        };

        // finding a reader for our image
        Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
        if(!readers.hasNext()) {
            closure.run();
            return null;
        }

        // using that reader to obtain the image format and to read our image
        ImageReader reader = readers.next();
        String imageFormat;
        reader.setInput(stream, true, true);
        BufferedImage input;
        try {
            imageFormat = reader.getFormatName();
            input = reader.read(0, reader.getDefaultReadParam());
        } catch (IOException e) {
            LOGGER.error("Failed to read screenshot image for file `{}`: ", screenshotFile, e);
            return null;
        } finally {
            reader.dispose();
            closure.run();
        }

        // creating our resulting thumbnail
        BufferedImage result = new BufferedImage(ratio.scale(input.getWidth()), ratio.scale(input.getHeight()), input.getType());
        Graphics2D graphics2D = result.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(input, 0, 0, result.getWidth(), result.getHeight(), null);
        graphics2D.dispose();
        File thumbnailFile = new File(this.thumbnailFolder, screenshotFile.getName());

        try {
            // writing the resulting image and returning its path
            if(!thumbnailFile.exists()) {
                Files.createFile(thumbnailFile.toPath());
            }
            if(ImageIO.write(result, imageFormat, thumbnailFile)) {
                return thumbnailFile;
            }
            return null;
        } catch (IOException e) {
            LOGGER.error("Could not write thumbnail at location `{}`:", thumbnailFile, e);
            return null;
        }
    }

    private void deleteThumbnailFile(@Nullable CompletableFuture<File> thumbnailFuture) {
        if(thumbnailFuture == null) {
            return;
        }
        thumbnailFuture.thenAcceptAsync(file -> {
            if(file != null) {
                try {
                    Files.deleteIfExists(file.toPath());
                } catch (IOException e) {
                    LOGGER.error("Failed to delete thumbnail file `{}`:", file.getName(), e);
                }
            }
        }, this.executor);
    }
}

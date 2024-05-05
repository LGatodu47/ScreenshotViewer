package io.github.lgatodu47.screenshot_viewer.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class CopyScreenshotToast implements Toast {
    private final int width;
    private final long displayDuration;
    private Text title;
    @Nullable
    private Text description;
    private long startTime;
    private boolean justUpdated;

    public CopyScreenshotToast(Text title, @Nullable Text description, long displayDuration) {
        this.title = title;
        this.description = description;
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        this.width = Math.max(160, 30 + Math.max(textRenderer.getWidth(title), description == null ? 0 : textRenderer.getWidth(description)));
        this.displayDuration = displayDuration;
    }

    @Override
    public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
        if (this.justUpdated) {
            this.startTime = startTime;
            this.justUpdated = false;
        }

        int width = this.getWidth();
        if (width == 160) {
            context.drawTexture(TEXTURE, 0, 0, 0, 64, width, this.getHeight());
        } else {
            int height = this.getHeight();
            this.drawPart(context, width, 0, 0, 28);
            this.drawPart(context, width, 32 - 4, height - 4, 4);
        }

        if (description == null) {
            context.drawText(manager.getClient().textRenderer, title, 18, 12, -256, false);
        } else {
            context.drawText(manager.getClient().textRenderer, title, 18, 7, -256, false);
            context.drawText(manager.getClient().textRenderer, description, 18, 18, -1, false);
        }

        return (double) (startTime - this.startTime) < (double) displayDuration * manager.getNotificationDisplayTimeMultiplier() ? Visibility.SHOW : Visibility.HIDE;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return 32;
    }

    private void drawPart(DrawContext context, int width, int textureV, int y, int height) {
        int i = textureV == 0 ? 20 : 5;
        int j = Math.min(60, width - i);
        context.drawTexture(TEXTURE, 0, y, 0, 64 + textureV, i, height);

        for(int k = i; k < width - j; k += 64) {
            context.drawTexture(TEXTURE, k, y, 32, 64 + textureV, Math.min(64, width - k - j), height);
        }

        context.drawTexture(TEXTURE, width - j, y, 160 - j, 64 + textureV, j, height);
    }

    public void setContent(Text title, @Nullable Text description) {
        this.title = title;
        this.description = description;
        this.justUpdated = true;
    }

    public static void add(ToastManager manager, Text title, @Nullable Text description, long displayDuration) {
        manager.add(new CopyScreenshotToast(title, description, displayDuration));
    }

    public static void show(ToastManager manager, Text title, @Nullable Text description, long displayDuration) {
        CopyScreenshotToast toast = manager.getToast(CopyScreenshotToast.class, CopyScreenshotToast.TYPE);
        if (toast == null) {
            add(manager, title, description, displayDuration);
        } else {
            toast.setContent(title, description);
        }
    }
}

package io.github.lgatodu47.screenshot_viewer.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CopyScreenshotToast implements Toast {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("toast/system");

    private final int width;
    private final long displayDuration;
    private Component title;
    @Nullable
    private Component  description;
    private long startTime;
    private boolean justUpdated;

    public CopyScreenshotToast(Component  title, @Nullable Component  description, long displayDuration) {
        this.title = title;
        this.description = description;
        Font font = Minecraft.getInstance().font;
        this.width = Math.max(160, 30 + Math.max(font.width(title), description == null ? 0 : font.width(description)));
        this.displayDuration = displayDuration;
    }

    @Override
    public @NotNull Visibility render(@NotNull GuiGraphics context, @NotNull ToastComponent manager, long startTime) {
        if (this.justUpdated) {
            this.startTime = startTime;
            this.justUpdated = false;
        }

        int width = this.width();
        if (width == 160) {
            context.blitSprite(TEXTURE, 0, 0, width, this.height());
        } else {
            int height = this.height();
            this.drawPart(context, width, 0, 0, 28);
            this.drawPart(context, width, 32 - 4, height - 4, 4);
        }

        if (description == null) {
            context.drawString(manager.getMinecraft().font, title, 18, 12, -256, false);
        } else {
            context.drawString(manager.getMinecraft().font, title, 18, 7, -256, false);
            context.drawString(manager.getMinecraft().font, description, 18, 18, -1, false);
        }

        return (double) (startTime - this.startTime) < (double) displayDuration * manager.getNotificationDisplayTimeMultiplier() ? Visibility.SHOW : Visibility.HIDE;
    }

    @Override
    public int width() {
        return width;
    }

    private void drawPart(GuiGraphics context, int width, int textureV, int y, int height) {
        int i = textureV == 0 ? 20 : 5;
        int j = Math.min(60, width - i);
        context.blitSprite(TEXTURE, 160, 32, 0, textureV, 0, y, i, height);

        for(int k = i; k < width - j; k += 64) {
            context.blitSprite(TEXTURE, 160, 32, 32, textureV, k, y, Math.min(64, width - k - j), height);
        }

        context.blitSprite(TEXTURE, 160, 32, 160 - j, textureV, width - j, y, j, height);
    }

    public void setContent(Component  title, @Nullable Component  description) {
        this.title = title;
        this.description = description;
        this.justUpdated = true;
    }

    public static void add(ToastComponent manager, Component  title, @Nullable Component  description, long displayDuration) {
        manager.addToast(new CopyScreenshotToast(title, description, displayDuration));
    }

    public static void show(ToastComponent manager, Component  title, @Nullable Component  description, long displayDuration) {
        CopyScreenshotToast toast = manager.getToast(CopyScreenshotToast.class, CopyScreenshotToast.NO_TOKEN);
        if (toast == null) {
            add(manager, title, description, displayDuration);
        } else {
            toast.setContent(title, description);
        }
    }
}

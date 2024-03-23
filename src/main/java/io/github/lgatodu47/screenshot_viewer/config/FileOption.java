package io.github.lgatodu47.screenshot_viewer.config;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.github.lgatodu47.catconfig.ConfigAccess;
import io.github.lgatodu47.catconfig.ConfigOption;
import io.github.lgatodu47.catconfig.ValueSerializationHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public record FileOption(String name, @Nullable Supplier<File> defValue, @Nullable String optCategory) implements ConfigOption<File> {
    @Override
    @Nullable
    public File defaultValue() {
        return defValue == null ? null : defValue.get();
    }

    @Override
    public Optional<String> category() {
        return Optional.ofNullable(optCategory);
    }

    @Override
    public Class<File> type() {
        return File.class;
    }

    @Override
    public void write(JsonWriter writer, @NotNull File value, ValueSerializationHelper helper) throws IOException {
        writer.value(value.getAbsolutePath());
    }

    @Override
    public File read(JsonReader reader, ValueSerializationHelper helper) throws IOException {
        return new File(reader.nextString());
    }

    public static ClickableWidget createScreenshotsDirectoryWidget(ConfigAccess config) {
        FilePathWidget widget = new FilePathWidget(MinecraftClient.getInstance().textRenderer, 0, 0, 200, 20, Text.empty());
        Supplier<File> defaultValue = () -> Objects.requireNonNull(ScreenshotViewerOptions.SCREENSHOTS_FOLDER.defaultValue());
        widget.setText(config.get(ScreenshotViewerOptions.SCREENSHOTS_FOLDER).orElseGet(defaultValue).getAbsolutePath());
        AtomicBoolean corrected = new AtomicBoolean(false);
        widget.setAcceptChangesListener(() -> {
            File target = new File(widget.getText());
            if(target.exists() && target.isAbsolute() && target.isDirectory() && target.canRead()) {
                config.put(ScreenshotViewerOptions.SCREENSHOTS_FOLDER, target);
                corrected.set(false);
                return;
            }
            widget.setText(defaultValue.get().getAbsolutePath());
            corrected.set(true);
        });
        widget.setRenderTextProvider((text, i) -> OrderedText.styledForwardsVisitedString(text, corrected.get() ? Style.EMPTY.withColor(Formatting.RED) : Style.EMPTY));
        return widget;
    }

    public static class FilePathWidget extends TextFieldWidget {
//        protected static final Identifier OPEN_FOLDER_BUTTON_TEXTURE = new Identifier(ScreenshotViewer.MODID, "textures/gui/open_folder_button.png");
        protected Runnable acceptChangesListener;

        public FilePathWidget(TextRenderer renderer, int x, int y, int width, int height, Text text) {
            super(renderer, x, y, width /*- height*/, height, text);
            setMaxLength(Integer.MAX_VALUE);
        }

        /*@Override
        protected boolean clicked(double mouseX, double mouseY) {
            return this.active && this.visible && mouseX >= getX() && mouseY >= getY() && mouseX < getX() + getWidth() && mouseY < getY() + getHeight();
        }

        @Override
        public int getWidth() {
            return super.getWidth() + getHeight();
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if(mouseX >= getX() + getWidth() - getHeight()) {
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1));
                // opens file explorer
            }
            super.onClick(mouseX, mouseY);
        }

        @Override
        public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
            super.renderButton(context, mouseX, mouseY, delta);
            if(isVisible()) {
                boolean buttonHovered = mouseX >= getX() + getWidth() - getHeight() && mouseY >= getY() && mouseX < getX() + getWidth() && mouseY < getY() + getHeight();
                context.drawTexture(OPEN_FOLDER_BUTTON_TEXTURE, getX() + getWidth() - getHeight(), getY(), 50, 0, buttonHovered ? 20 : 0, getHeight(), getHeight(), 32, 64);
            }
        }*/

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            boolean res = super.mouseClicked(mouseX, mouseY, button);
            if(res && acceptChangesListener != null) {
                acceptChangesListener.run();
            }
            return res;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if(!isActive()) {
                return false;
            }
            if(keyCode == GLFW.GLFW_KEY_ENTER && acceptChangesListener != null) {
                acceptChangesListener.run();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);
            if(!focused && acceptChangesListener != null) {
                acceptChangesListener.run();
            }
        }

        public void setAcceptChangesListener(Runnable acceptChangesListener) {
            this.acceptChangesListener = acceptChangesListener;
        }
    }
}

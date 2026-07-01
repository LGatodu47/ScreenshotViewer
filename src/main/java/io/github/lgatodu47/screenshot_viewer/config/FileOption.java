package io.github.lgatodu47.screenshot_viewer.config;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.github.lgatodu47.catconfig.ConfigAccess;
import io.github.lgatodu47.catconfig.ConfigOption;
import io.github.lgatodu47.catconfig.ValueSerializationHelper;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.screen.IconButtonWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
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
        writer.value(value.getPath());
    }

    @Override
    public File read(JsonReader reader, ValueSerializationHelper helper) throws IOException {
        return new File(reader.nextString());
    }

    public static AbstractWidget createDirectoryWidget(ConfigAccess config, ConfigOption<File> option) {
        return new FileSettingsWidget(config, option, 200, 20);
    }

    public static class FileSettingsWidget extends AbstractContainerWidget {
        private static final Identifier OPEN_FOLDER = Identifier.fromNamespaceAndPath(ScreenshotViewer.MODID, "widget/icons/open_folder");

        private final ConfigAccess config;
        private final ConfigOption<File> option;
        private final EditBox editBox;
        private final AbstractButton button;
        private boolean errored;

        public FileSettingsWidget(ConfigAccess config, ConfigOption<File> option, int width, int height) {
            super(0, 0, width, height, Component.empty());
            this.config = config;
            this.option = option;
            int spacing = 8;
            this.editBox = new EditBox(Minecraft.getInstance().font, 0, 0, width - spacing - height, height, Component.empty());
            this.button = new IconButtonWidget(0, 0, height, height, Component.empty(), OPEN_FOLDER, _ -> tryOpenFolder()) {
                @Override
                public boolean isActive() {
                    return !errored;
                }
            };
            initEditBox();
        }

        private void initEditBox() {
            editBox.setMaxLength(Integer.MAX_VALUE);

            String initValue = config.get(option).or(() -> Optional.ofNullable(option.defaultValue())).map(File::getPath).orElse(".");
            editBox.setValue(initValue);

            editBox.addFormatter((text, _) -> FormattedCharSequence.forward(text, this.errored ? Style.EMPTY.withColor(ChatFormatting.RED) : Style.EMPTY));
            editBox.setResponder(s -> {
                File target = new File(s);
                if(target.exists() && target.isDirectory() && target.canRead()) {
                    config.put(option, target);
                    this.errored = false;
                    return;
                }
                this.errored = true;
            });
        }

        private void tryOpenFolder() {
            Util.getPlatform().openFile(new File(editBox.getValue()));
        }

        @Override
        public void setX(int x) {
            super.setX(x);
            editBox.setX(x);
            button.setX(x + getWidth() - getHeight());
        }

        @Override
        public void setY(int y) {
            super.setY(y);
            editBox.setY(y);
            button.setY(y);
        }

        @Override
        protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            editBox.extractWidgetRenderState(graphics, mouseX, mouseY, a);
            button.extractRenderState(graphics, mouseX, mouseY, a);
        }

        @Override
        protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {
        }

        @Override
        protected int contentHeight() {
            return getHeight();
        }

        @Override
        public @NonNull List<? extends GuiEventListener> children() {
            return List.of(editBox, button);
        }
    }
}

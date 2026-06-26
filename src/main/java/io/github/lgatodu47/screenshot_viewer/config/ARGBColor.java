package io.github.lgatodu47.screenshot_viewer.config;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import io.github.lgatodu47.catconfig.ConfigAccess;
import io.github.lgatodu47.catconfig.ConfigOption;
import io.github.lgatodu47.catconfig.ValueSerializationHelper;
import io.github.lgatodu47.catconfigmc.OldEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.Optional;

public record ARGBColor(int value) {
    public static final ARGBColor WHITE = new ARGBColor(0xFFFFFFFF);

    public int alpha() {
        return value() >>> 24;
    }

    public int red() {
        return value() >> 16 & 0xFF;
    }

    public int green() {
        return value() >> 8 & 0xFF;
    }

    public int blue() {
        return value() & 0xFF;
    }

    public String toHex() {
        return String.format("#%08X", value());
    }

    public static Optional<ARGBColor> fromHex(String hexString) {
        if(hexString.startsWith("#")) {
            hexString = hexString.substring(1);
        }
        try {
            int value = Integer.parseUnsignedInt(hexString.substring(0, Math.min(hexString.length(), 8)), 16);
            return Optional.of(new ARGBColor(value));
        } catch (Throwable ignored) {
        }
        return Optional.empty();
    }

    public static ARGBColor fromChannels(int a, int r, int g, int b) {
        return new ARGBColor((a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF));
    }

    public record Option(String name, @Nullable ARGBColor defaultValue, @Nullable String optCategory) implements ConfigOption<ARGBColor> {
        @Override
        public Optional<String> category() {
            return Optional.ofNullable(optCategory);
        }

        @Override
        public Class<ARGBColor> type() {
            return ARGBColor.class;
        }

        @Override
        public void write(JsonWriter writer, @NonNull ARGBColor value, ValueSerializationHelper helper) throws IOException {
            writer.value(value.value());
        }

        @Override
        public ARGBColor read(JsonReader reader, ValueSerializationHelper helper) throws IOException {
            if(reader.peek().equals(JsonToken.STRING)) {
                return TextColor.parseColor(reader.nextString()).result().map(textColor -> new ARGBColor(0xFF000000 | textColor.getValue())).orElse(null);
            }
            return new ARGBColor(reader.nextInt());
        }
    }

    public static AbstractWidget createWidget(ConfigAccess config, ConfigOption<ARGBColor> option) {
        ColorTextField widget = new ColorTextField(Minecraft.getInstance().font, 0, 0, 80, 20, Component.empty());
        widget.setMaxLength(9);
        widget.setValue(config.get(option).map(ARGBColor::toHex).orElse("#"));
        widget.setTextPredicate(s -> s.startsWith("#") && (s.substring(1).isEmpty() || fromHex(s).isPresent()));
        widget.setResponder(s -> {
            if(s.isEmpty() || s.substring(1).isEmpty()) {
                config.put(option, null);
                return;
            }
            fromHex(s).ifPresent(color -> config.put(option, color));
        });
        return widget;
    }

    public static class ColorTextField extends OldEditBox {
        public ColorTextField(Font renderer, int x, int y, int width, int height, Component text) {
            super(renderer, x, y, width, height, text);
        }

        @Override
        public boolean keyPressed(KeyEvent input) {
            if (canConsumeInput() && input.isPaste()) {
                String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
                if(clipboard.startsWith("#") && clipboard.length() < 8) {
                    clipboard = clipboard.substring(1);
                }
                this.insertText(clipboard);
                return true;
            }
            return super.keyPressed(input);
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            super.extractWidgetRenderState(context, mouseX, mouseY, delta);
            if(this.isVisible()) {
                String text = getValue();
                if(!text.isEmpty()) {
                    context.fill(this.getX(), this.getY(), this.getX() + 3, this.getY() + this.height / 2, 0xFFFFFFFF);
                    context.fill(this.getX(), this.getY() + this.height / 2, this.getX() + 3, this.getY() + this.height, 0xFF000000);
                    try {
                        int color = Integer.parseUnsignedInt(text.substring(1), 16);
                        context.fill(this.getX(), this.getY(), this.getX() + 3, this.getY() + this.height, color);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
    }
}

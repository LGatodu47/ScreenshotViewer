package io.github.lgatodu47.screenshot_viewer.config;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.github.lgatodu47.catconfig.ConfigAccess;
import io.github.lgatodu47.catconfig.ConfigOption;
import io.github.lgatodu47.catconfig.ValueSerializationHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

public record ColorOption(String name, @Nullable TextColor defaultValue, @Nullable String optCategory) implements ConfigOption<TextColor> {
    @Override
    public Optional<String> category() {
        return Optional.ofNullable(optCategory);
    }

    @Override
    public Class<TextColor> type() {
        return TextColor.class;
    }

    @Override
    public void write(JsonWriter writer, @NotNull TextColor value, ValueSerializationHelper helper) throws IOException {
        writer.value(getHexCode(value));
    }

    @Override
    public TextColor read(JsonReader reader, ValueSerializationHelper helper) throws IOException {
        return TextColor.parse(reader.nextString());
    }

    public static ClickableWidget createWidget(ConfigAccess config, ConfigOption<TextColor> option) {
        ColorTextField widget = new ColorTextField(MinecraftClient.getInstance().textRenderer, 0, 0, 60, 20, Text.empty());
        widget.setMaxLength(7);
        widget.setText(config.get(option).map(ColorOption::getHexCode).orElse("#"));
        widget.setTextPredicate(s -> s.startsWith("#") && (s.substring(1).isEmpty() || TextColor.parse(s) != null));
        widget.setChangedListener(s -> {
            if(s.isEmpty() || s.substring(1).isEmpty()) {
                config.put(option, null);
                return;
            }
            TextColor color = TextColor.parse(s);
            if(color != null) {
                config.put(option, color);
            }
        });
        return widget;
    }

    private static String getHexCode(TextColor textColor) {
        if (FabricLoader.getInstance().isModLoaded("fabric-transitive-access-wideners-v1")) {
            MappingResolver resolver = FabricLoader.getInstance().getMappingResolver();
            try {
                Method getHexCode = TextColor.class.getDeclaredMethod(resolver.mapMethodName(resolver.getCurrentRuntimeNamespace(), "net.minecraft.class_5251", "method_27723", "()Ljava/lang/String;"));
                if(getHexCode.canAccess(textColor)) {
                    return (String) getHexCode.invoke(textColor);
                }
            } catch (Throwable ignored) {
            }
        }
        return String.format(Locale.ROOT, "#%06X", textColor.getRgb());
    }

    public static class ColorTextField extends TextFieldWidget {
        public ColorTextField(TextRenderer renderer, int x, int y, int width, int height, Text text) {
            super(renderer, x, y, width, height, text);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (isActive() && Screen.isPaste(keyCode)) {
                String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
                if(clipboard.startsWith("#") && clipboard.length() < 8) {
                    clipboard = clipboard.substring(1);
                }
                this.write(clipboard);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
            super.renderButton(context, mouseX, mouseY, delta);
            if(this.isVisible()) {
                String text = getText();
                if(!text.isEmpty()) {
                    try {
                        int color = Integer.parseInt(text.substring(1), 16);
                        context.fill(this.getX(), this.getY(), this.getX() + 3, this.getY() + this.height, 0xFF000000 | color);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
    }
}
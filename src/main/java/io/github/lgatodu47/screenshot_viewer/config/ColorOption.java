package io.github.lgatodu47.screenshot_viewer.config;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.github.lgatodu47.catconfig.ConfigAccess;
import io.github.lgatodu47.catconfig.ConfigOption;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;

public record ColorOption(String name, @Nullable TextColor defaultValue) implements ConfigOption<TextColor> {
    @Override
    public void write(JsonWriter writer, @NotNull TextColor value) throws IOException {
        writer.value(getHexCode(value));
    }

    @Override
    public TextColor read(JsonReader reader) throws IOException {
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
        public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            super.renderButton(matrices, mouseX, mouseY, delta);
            if(this.isVisible()) {
                String text = getText();
                if(!text.isEmpty()) {
                    try {
                        int color = Integer.parseInt(text.substring(1), 16);
                        fill(matrices, this.x, this.y, this.x + 3, this.y + this.height, 0xFF000000 | color);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
    }
}
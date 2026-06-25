package io.github.lgatodu47.screenshot_viewer.config;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.github.lgatodu47.catconfig.ConfigAccess;
import io.github.lgatodu47.catconfig.ConfigOption;
import io.github.lgatodu47.catconfig.ValueSerializationHelper;
import io.github.lgatodu47.catconfigmc.OldEditBox;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
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
        return TextColor.parseColor(reader.nextString()).result().orElse(null);
    }

    public static AbstractWidget createWidget(ConfigAccess config, ConfigOption<TextColor> option) {
        ColorTextField widget = new ColorTextField(Minecraft.getInstance().font, 0, 0, 60, 20, Component.empty());
        widget.setMaxLength(7);
        widget.setValue(config.get(option).map(ColorOption::getHexCode).orElse("#"));
        widget.setTextPredicate(s -> s.startsWith("#") && (s.substring(1).isEmpty() || TextColor.parseColor(s).result().isPresent()));
        widget.setResponder(s -> {
            if(s.isEmpty() || s.substring(1).isEmpty()) {
                config.put(option, null);
                return;
            }
            TextColor.parseColor(s).result().ifPresent(color -> config.put(option, color));
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
        return String.format(Locale.ROOT, "#%06X", textColor.getValue());
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
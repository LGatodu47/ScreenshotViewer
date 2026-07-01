package io.github.lgatodu47.screenshot_viewer.config;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.github.lgatodu47.catconfig.ConfigAccess;
import io.github.lgatodu47.catconfig.ConfigOption;
import io.github.lgatodu47.catconfig.ValueSerializationHelper;
import io.github.lgatodu47.screenshot_viewer.screen.ConfigureButtonPlacementScreen;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public record WidgetPositionOption(String name, @Nullable WidgetPosition defaultValue, @Nullable String optCategory) implements ConfigOption<WidgetPositionOption.WidgetPosition> {
    @Override
    public Optional<String> category() {
        return Optional.ofNullable(optCategory);
    }

    @Override
    public Class<WidgetPosition> type() {
        return WidgetPosition.class;
    }

    @Override
    public void write(JsonWriter writer, @NotNull WidgetPosition value, ValueSerializationHelper helper) throws IOException {
        writer.beginObject();
        writer.name("x"); writer.value(value.x());
        writer.name("y"); writer.value(value.y());
        writer.endObject();
    }

    @Override
    public WidgetPosition read(JsonReader reader, ValueSerializationHelper helper) throws IOException {
        reader.beginObject();
        reader.nextName(); int x = reader.nextInt();
        reader.nextName(); int y = reader.nextInt();
        reader.endObject();
        return new WidgetPosition(x, y);
    }

    public static AbstractWidget createWidget(ConfigAccess access, ConfigOption<WidgetPosition> option, Supplier<Screen> configuringScreenFactory, ConfigureButtonPlacementScreen.WidgetRemover remover, BooleanSupplier canEdit) {
        Minecraft client = Minecraft.getInstance();
        Button btn = new Button.Builder(ScreenshotViewerTexts.EDIT_WIDGET_PLACEMENT, _ -> {
            client.gui.setScreen(new ConfigureButtonPlacementScreen(client.gui.screen(), access, option, configuringScreenFactory, remover));
        }).width(100).build();
        btn.active = canEdit.getAsBoolean();
        return btn;
    }

    // these are now relative to a specific widget called 'reference': for the pause menu, it is the "back to game" button, for the title screen, it is the "singleplayer" button
    public record WidgetPosition(int x, int y) {}
}

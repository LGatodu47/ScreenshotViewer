package io.github.lgatodu47.screenshot_viewer;

import io.github.lgatodu47.catconfig.CatConfig;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfig;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions;
import io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots.ManageScreenshotsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class ScreenshotViewer implements ClientModInitializer {
    public static final String MODID = "screenshot_viewer";

    private static ScreenshotViewer instance;

    private CatConfig config;
    private KeyBinding openScreenshotsScreenKey;

    @Override
    public void onInitializeClient() {
        config = new ScreenshotViewerConfig();

        initKeyBindings();
        registerEvents();
        instance = this;
    }

    private void initKeyBindings() {
        openScreenshotsScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(translation("key", "open_screenshots_screen"), InputUtil.UNKNOWN_KEY.getCode(), KeyBinding.MISC_CATEGORY));
    }

    private static final Identifier DELAYED_PHASE = new Identifier(MODID, "delayed");
    private static final Identifier MANAGE_SCREENSHOTS_BUTTON_TEXTURE = new Identifier(MODID, "textures/gui/screenshots_button.png");

    private void registerEvents() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if(client.world != null && client.currentScreen == null && openScreenshotsScreenKey != null && !openScreenshotsScreenKey.isUnbound()) {
                if(openScreenshotsScreenKey.isPressed()) {
                    client.setScreen(new ManageScreenshotsScreen(null));
                }
            }
        });
        ScreenEvents.AFTER_INIT.register(DELAYED_PHASE, (client, screen, scaledWidth, scaledHeight) -> {
            if(config.getOrFallback(ScreenshotViewerOptions.SHOW_BUTTON_IN_GAME_PAUSE_MENU, true) && screen instanceof GameMenuScreen) {
                List<ClickableWidget> buttons = Screens.getButtons(screen);
                ClickableWidget topButton = buttons.get(0);
                buttons.add(Util.make(new TexturedButtonWidget(topButton.getX() + topButton.getWidth() + config.getOrFallback(ScreenshotViewerOptions.PAUSE_MENU_BUTTON_OFFSET, 4), topButton.getY(), topButton.getHeight(), topButton.getHeight(), 0, 0, 20, MANAGE_SCREENSHOTS_BUTTON_TEXTURE, 32, 64, button -> {
                    client.setScreen(new ManageScreenshotsScreen(screen));
                }, translatable("screen", "manage_screenshots")), btn -> btn.setTooltip(Tooltip.of(translatable("screen", "manage_screenshots")))));
            }
            if(config.getOrFallback(ScreenshotViewerOptions.SHOW_BUTTON_ON_TITLE_SCREEN, true) && screen instanceof TitleScreen) {
                List<ClickableWidget> buttons = Screens.getButtons(screen);
                Optional<ClickableWidget> accessibilityWidgetOpt = buttons.stream()
                        .filter(TexturedButtonWidget.class::isInstance)
                        .filter(widget -> widget.getMessage().equals(Text.translatable("narrator.button.accessibility")))
                        .findFirst();

                int x = accessibilityWidgetOpt.map(ClickableWidget::getX).orElse(screen.width / 2 + 104);
                int y = accessibilityWidgetOpt.map(ClickableWidget::getY).orElse(screen.height / 4 + 132);
                int width = accessibilityWidgetOpt.map(ClickableWidget::getWidth).orElse(20);
                int height = accessibilityWidgetOpt.map(ClickableWidget::getHeight).orElse(20);
                buttons.add(Util.make(new TexturedButtonWidget(x + width + 4, y, width, height, 0, 0, 20, MANAGE_SCREENSHOTS_BUTTON_TEXTURE, 32, 64, button -> {
                    client.setScreen(new ManageScreenshotsScreen(screen));
                }, translatable("screen", "manage_screenshots")), btn -> btn.setTooltip(Tooltip.of(translatable("screen", "manage_screenshots")))));
            }
        });
        ScreenEvents.AFTER_INIT.addPhaseOrdering(Event.DEFAULT_PHASE, DELAYED_PHASE);
    }

    public CatConfig getConfig() {
        return config;
    }

    public KeyBinding getOpenScreenshotsScreenKey() {
        return openScreenshotsScreenKey;
    }

    @NotNull
    public static ScreenshotViewer getInstance() {
        if(instance == null) {
            throw new IllegalStateException("Screenshot Viewer Client is not loaded yet!");
        }

        return instance;
    }

    public static String translation(String prefix, String suffix) {
        return prefix + '.' + MODID + '.' + suffix;
    }

    public static Text translatable(String prefix, String suffix) {
        return Text.translatable(translation(prefix, suffix));
    }

    public static File getVanillaScreenshotsFolder() {
        return new File(MinecraftClient.getInstance().runDirectory, "screenshots");
    }
}

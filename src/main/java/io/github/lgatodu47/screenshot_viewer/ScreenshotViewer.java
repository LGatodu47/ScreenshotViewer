package io.github.lgatodu47.screenshot_viewer;

import io.github.lgatodu47.catconfig.CatConfig;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfig;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerOptions;
import io.github.lgatodu47.screenshot_viewer.config.WidgetPositionOption;
import io.github.lgatodu47.screenshot_viewer.screen.IconButtonWidget;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerTexts;
import io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots.ManageScreenshotsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class ScreenshotViewer implements ClientModInitializer {
    public static final String MODID = "screenshot_viewer";

    private static ScreenshotViewer instance;

    private CatConfig config;
    private ScreenshotThumbnailManager thumbnailManager;
    private KeyMapping openScreenshotsScreenKey;

    @Override
    public void onInitializeClient() {
        config = new ScreenshotViewerConfig();
        thumbnailManager = new ScreenshotThumbnailManager(config);

        initKeyBindings();
        registerEvents();
        instance = this;
    }

    private void initKeyBindings() {
        openScreenshotsScreenKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(ScreenshotViewerTexts.translation("key", "open_screenshots_screen"), InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC));
    }

    private static final Identifier DELAYED_PHASE = Identifier.fromNamespaceAndPath(MODID, "delayed");
    private static final Identifier SCREENSHOT_VIEWER_ICON = Identifier.fromNamespaceAndPath(MODID, "widget/icons/screenshot_viewer");

    private void registerEvents() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if(client.level != null && client.screen == null && openScreenshotsScreenKey != null && !openScreenshotsScreenKey.isUnbound()) {
                if(openScreenshotsScreenKey.isDown()) {
                    client.setScreen(new ManageScreenshotsScreen(null));
                }
            }
        });
        ScreenEvents.AFTER_INIT.register(DELAYED_PHASE, (client, screen, scaledWidth, scaledHeight) -> {
            if(config.getOrFallback(ScreenshotViewerOptions.SHOW_BUTTON_IN_GAME_PAUSE_MENU, true) && screen instanceof PauseScreen) {
                List<AbstractWidget> buttons = Screens.getWidgets(screen);
                AbstractWidget topButton = buttons.getFirst();

                Optional<WidgetPositionOption.WidgetPosition> optionalWidgetPos = config.get(ScreenshotViewerOptions.PAUSE_MENU_BUTTON_POSITION);
                int x = optionalWidgetPos.map(WidgetPositionOption.WidgetPosition::x).orElse(topButton.getX() + topButton.getWidth() + 4);
                int y = optionalWidgetPos.map(WidgetPositionOption.WidgetPosition::y).orElse(topButton.getY());
                buttons.add(Util.make(new IconButtonWidget(x, y, topButton.getHeight(), topButton.getHeight(), ScreenshotViewerTexts.MANAGE_SCREENSHOTS, SCREENSHOT_VIEWER_ICON, button -> {
                    client.setScreen(new ManageScreenshotsScreen(screen));
                }), btn -> btn.setTooltip(Tooltip.create(ScreenshotViewerTexts.MANAGE_SCREENSHOTS))));
            }
            if(config.getOrFallback(ScreenshotViewerOptions.SHOW_BUTTON_ON_TITLE_SCREEN, true) && screen instanceof TitleScreen) {
                List<AbstractWidget> buttons = Screens.getWidgets(screen);
                Optional<AbstractWidget> accessibilityWidgetOpt = buttons.stream()
                        .filter(SpriteIconButton.class::isInstance)
                        .filter(widget -> widget.getMessage().equals(Component.translatable("options.accessibility")))
                        .findFirst();

                Optional<WidgetPositionOption.WidgetPosition> optionalWidgetPos = config.get(ScreenshotViewerOptions.TITLE_SCREEN_BUTTON_POSITION);
                int width = accessibilityWidgetOpt.map(AbstractWidget::getWidth).orElse(20);
                int height = accessibilityWidgetOpt.map(AbstractWidget::getHeight).orElse(20);
                int x = optionalWidgetPos.map(WidgetPositionOption.WidgetPosition::x).or(() -> accessibilityWidgetOpt.map(AbstractWidget::getX).map(v -> v + width + 4)).orElse(screen.width / 2 + 104 + width + 4);
                int y = optionalWidgetPos.map(WidgetPositionOption.WidgetPosition::y).or(() -> accessibilityWidgetOpt.map(AbstractWidget::getY)).orElse(screen.height / 4 + 132);
                buttons.add(Util.make(new IconButtonWidget(x, y, width, height, ScreenshotViewerTexts.MANAGE_SCREENSHOTS, SCREENSHOT_VIEWER_ICON, button -> {
                    client.setScreen(new ManageScreenshotsScreen(screen));
                }), btn -> btn.setTooltip(Tooltip.create(ScreenshotViewerTexts.MANAGE_SCREENSHOTS))));
            }
        });
        ScreenEvents.AFTER_INIT.addPhaseOrdering(Event.DEFAULT_PHASE, DELAYED_PHASE);
    }

    public CatConfig getConfig() {
        return config;
    }

    public ScreenshotThumbnailManager getThumbnailManager() {
        return thumbnailManager;
    }

    public KeyMapping getOpenScreenshotsScreenKey() {
        return openScreenshotsScreenKey;
    }

    @NotNull
    public static ScreenshotViewer getInstance() {
        if(instance == null) {
            throw new IllegalStateException("Screenshot Viewer Client is not loaded yet!");
        }

        return instance;
    }
}

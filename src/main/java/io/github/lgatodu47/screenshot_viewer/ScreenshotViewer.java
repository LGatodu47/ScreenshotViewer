package io.github.lgatodu47.screenshot_viewer;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfig;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfigListener;
import io.github.lgatodu47.screenshot_viewer.screens.IconButtonWidget;
import io.github.lgatodu47.screenshot_viewer.screens.ScreenshotClickEvent;
import io.github.lgatodu47.screenshot_viewer.screens.ScreenshotViewerTexts;
import io.github.lgatodu47.screenshot_viewer.screens.manage_screenshots.ManageScreenshotsScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ScreenshotEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@Mod(ScreenshotViewer.MODID)
public class ScreenshotViewer {
    public static final String MODID = "screenshot_viewer";
    private static ScreenshotViewer instance;

    private final List<ScreenshotViewerConfigListener> configListeners = new ArrayList<>();
    private final ScreenshotViewerConfig config;
    private final ScreenshotThumbnailManager thumbnailManager;
    private final ModContainer modContainer;
    private KeyMapping openScreenshotsScreenKey;

    public ScreenshotViewer(@NotNull IEventBus bus, @NotNull ModContainer container) {
        bus.addListener(this::registerKeyMappings);
        bus.addListener(this::onConfigLoaded);
        bus.addListener(this::onConfigReloaded);

        this.config = ScreenshotViewerConfig.registerConfig(container);
        this.thumbnailManager = new ScreenshotThumbnailManager(this, config);
        this.modContainer = container;

        NeoForge.EVENT_BUS.register(this);
        instance = this;
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        openScreenshotsScreenKey = Util.make(new KeyMapping(ScreenshotViewerTexts.translation("key", "open_screenshots_screen"), KeyConflictContext.IN_GAME, InputConstants.UNKNOWN, KeyMapping.CATEGORY_MISC), event::register);
    }

    private void onConfigLoaded(ModConfigEvent.Loading event) {
        this.configListeners.forEach(ScreenshotViewerConfigListener::onConfigReloaded);
    }

    private void onConfigReloaded(ModConfigEvent.Reloading event) {
        this.configListeners.forEach(ScreenshotViewerConfigListener::onConfigReloaded);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        if(openScreenshotsScreenKey == null || openScreenshotsScreenKey.isUnbound()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        KeyMapping openScreenshotsScreenKey = getInstance().getOpenScreenshotsScreenKey();
        if(client.level != null && client.screen == null && event.getAction() == InputConstants.PRESS && openScreenshotsScreenKey.getKey().getValue() == event.getKey()) {
            client.setScreen(new ManageScreenshotsScreen(null));
        }
    }

    private static final ResourceLocation SCREENSHOT_VIEWER_ICON = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/sprites/widget/icons/screenshot_viewer.png");

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onScreenPostInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        Minecraft client = screen.getMinecraft();
        List<GuiEventListener> buttons = event.getListenersList();

        if(screen instanceof PauseScreen && config.showButtonInGamePauseMenu.get()) {
            buttons.stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).findFirst().ifPresent(topButton -> {
                event.addListener(Util.make(new IconButtonWidget(topButton.getX() + topButton.getWidth() + 4, topButton.getY() + config.pauseMenuButtonVerticalOffset.get(), topButton.getHeight(), topButton.getHeight(), ScreenshotViewerTexts.MANAGE_SCREENSHOTS, SCREENSHOT_VIEWER_ICON, button -> {
                    client.setScreen(new ManageScreenshotsScreen(screen));
                }), btn -> btn.setTooltip(Tooltip.create(ScreenshotViewerTexts.MANAGE_SCREENSHOTS))));
            });
        }
        if(screen instanceof TitleScreen && config.showButtonOnTitleScreen.get()) {
            Optional<SpriteIconButton> accessibilityWidgetOpt = buttons.stream()
                    .filter(SpriteIconButton.class::isInstance)
                    .map(SpriteIconButton.class::cast)
                    .filter(widget -> widget.getMessage().equals(Component.translatable("options.accessibility")))
                    .findFirst();

            int x = accessibilityWidgetOpt.map(AbstractWidget::getX).orElse(screen.width / 2 + 104);
            int y = accessibilityWidgetOpt.map(AbstractWidget::getY).orElse(screen.height / 4 + 132);
            int width = accessibilityWidgetOpt.map(SpriteIconButton::getWidth).orElse(20);
            int height = accessibilityWidgetOpt.map(SpriteIconButton::getHeight).orElse(20);
            event.addListener(Util.make(new IconButtonWidget(x + width + 4 + config.titleScreenButtonHorizontalOffset.get(), y, width, height, ScreenshotViewerTexts.MANAGE_SCREENSHOTS, SCREENSHOT_VIEWER_ICON, button -> {
                client.setScreen(new ManageScreenshotsScreen(screen));
            }), btn -> btn.setTooltip(Tooltip.create(ScreenshotViewerTexts.MANAGE_SCREENSHOTS))));
        }
    }

    @SubscribeEvent
    public void onScreenshotTaken(ScreenshotEvent event) {
        event.setResultMessage(
                Component.translatable("screenshot.success", Component.literal(event.getScreenshotFile().getName())
                        .withStyle(ChatFormatting.UNDERLINE)
                        .withStyle((style) -> style
                                .withClickEvent(new ScreenshotClickEvent(event.getScreenshotFile()))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, ScreenshotViewerTexts.REDIRECT_TO_SCREENSHOT_MANAGER) {
                                    @Nullable
                                    @Override
                                    public <T> T getValue(@Nonnull Action<T> action) {
                                        return config.redirectScreenshotChatLinks.get() ? super.getValue(action) : null;
                                    }
                                })
                ))
        );
    }

    public ScreenshotViewerConfig getConfig() {
        return config;
    }

    public ScreenshotThumbnailManager getThumbnailManager() {
        return thumbnailManager;
    }

    public KeyMapping getOpenScreenshotsScreenKey() {
        return openScreenshotsScreenKey;
    }

    public Optional<IConfigScreenFactory> getConfigScreenFactory() {
        return IConfigScreenFactory.getForMod(this.modContainer.getModInfo());
    }

    public ModContainer getModContainer() {
        return modContainer;
    }

    public void registerConfigListener(ScreenshotViewerConfigListener listener) {
        this.configListeners.add(listener);
    }

    public void unregisterConfigListener(ScreenshotViewerConfigListener listener) {
        this.configListeners.remove(listener);
    }

    @NotNull
    public static ScreenshotViewer getInstance() {
        if(instance == null) {
            throw new IllegalStateException("Screenshot Viewer is not loaded yet!");
        }

        return instance;
    }
}

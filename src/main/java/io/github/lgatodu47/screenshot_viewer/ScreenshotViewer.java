package io.github.lgatodu47.screenshot_viewer;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfig;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfigListener;
import io.github.lgatodu47.screenshot_viewer.screens.ManageScreenshotsScreen;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forgespi.language.IModInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

@Mod(ScreenshotViewer.MODID)
public class ScreenshotViewer {
    public static final String MODID = "screenshot_viewer";
    private static ScreenshotViewer instance;

    private final List<ScreenshotViewerConfigListener> configListeners = new ArrayList<>();
    private final ScreenshotViewerConfig config;
    private final IModInfo modInfo;
    private KeyMapping openScreenshotsScreenKey;

    public ScreenshotViewer() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::onConfigReloaded);
        registerKeyMappings();

        ModLoadingContext mlc = ModLoadingContext.get();
        this.config = ScreenshotViewerConfig.registerConfig(mlc);
        this.modInfo = mlc.getActiveContainer().getModInfo();

        MinecraftForge.EVENT_BUS.register(this);
        instance = this;
    }

    private void registerKeyMappings() {
        openScreenshotsScreenKey = Util.make(new KeyMapping(translation("key", "open_screenshots_screen"), KeyConflictContext.IN_GAME, InputConstants.UNKNOWN, KeyMapping.CATEGORY_MISC), ClientRegistry::registerKeyBinding);
    }

    private void onConfigReloaded(ModConfigEvent.Reloading event) {
        this.configListeners.forEach(ScreenshotViewerConfigListener::onConfigReloaded);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft client = Minecraft.getInstance();
        KeyMapping openScreenshotsScreenKey = getInstance().getOpenScreenshotsScreenKey();
        if(client.level != null && client.screen == null && event.getAction() == InputConstants.PRESS && openScreenshotsScreenKey != null && openScreenshotsScreenKey.getKey().getValue() == event.getKey()) {
            client.setScreen(new ManageScreenshotsScreen(null));
        }
    }

    private static final ResourceLocation MANAGE_SCREENSHOTS_BUTTON_TEXTURE = new ResourceLocation(MODID, "textures/gui/screenshots_button.png");

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onScreenPostInit(ScreenEvent.InitScreenEvent.Post event) {
        Screen screen = event.getScreen();
        Minecraft client = screen.getMinecraft();
        List<GuiEventListener> buttons = event.getListenersList();

        if(config.showButtonInGamePauseMenu.get() && screen instanceof PauseScreen) {
            AbstractWidget topButton = buttons.stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).toList().get(0);
            event.addListener(new ImageButton(topButton.x + topButton.getWidth() + 8, topButton.y, topButton.getHeight(), topButton.getHeight(), 0, 0, 20, MANAGE_SCREENSHOTS_BUTTON_TEXTURE, 32, 64, button -> {
                client.setScreen(new ManageScreenshotsScreen(screen));
            }, (button, matrices, mouseX, mouseY) -> {
                screen.renderTooltip(matrices, client.font.split(translatable("screen", "manage_screenshots"), Math.max(screen.width / 2 - 43, 170)), mouseX, mouseY);
            }, translatable("screen", "manage_screenshots")));
        }
        if(config.showButtonOnTitleScreen.get() && screen instanceof TitleScreen) {
            Optional<ImageButton> accessibilityWidgetOpt = buttons.stream()
                    .filter(ImageButton.class::isInstance)
                    .map(ImageButton.class::cast)
                    .filter(widget -> widget.getMessage().equals(new TranslatableComponent("narrator.button.accessibility")))
                    .findFirst();

            int x = accessibilityWidgetOpt.map(widget -> widget.x).orElse(screen.width / 2 + 104);
            int y = accessibilityWidgetOpt.map(widget -> widget.y).orElse(screen.height / 4 + 132);
            int width = accessibilityWidgetOpt.map(ImageButton::getWidth).orElse(20);
            int height = accessibilityWidgetOpt.map(ImageButton::getHeight).orElse(20);
            event.addListener(new ImageButton(x + width + 4, y, width, height, 0, 0, 20, MANAGE_SCREENSHOTS_BUTTON_TEXTURE, 32, 64, button -> {
                client.setScreen(new ManageScreenshotsScreen(screen));
            }, (button, matrices, mouseX, mouseY) -> {
                screen.renderTooltip(matrices, client.font.split(translatable("screen", "manage_screenshots"), Math.max(screen.width / 2 - 43, 170)), mouseX, mouseY);
            }, translatable("screen", "manage_screenshots")));
        }
    }

    public ScreenshotViewerConfig getConfig() {
        return config;
    }

    public KeyMapping getOpenScreenshotsScreenKey() {
        return openScreenshotsScreenKey;
    }

    public Optional<BiFunction<Minecraft, Screen, Screen>> getConfigScreenFactory() {
        return ConfigGuiHandler.getGuiFactoryFor(this.modInfo);
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

    public static String translation(String prefix, String suffix) {
        return prefix + '.' + MODID + '.' + suffix;
    }

    public static Component translatable(String prefix, String suffix) {
        return new TranslatableComponent(translation(prefix, suffix));
    }
}

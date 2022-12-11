package io.github.lgatodu47.screenshot_viewer;

import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfig;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfigListener;
import io.github.lgatodu47.screenshot_viewer.screens.ManageScreenshotsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.ConfigGuiHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
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
    private KeyBinding openScreenshotsScreenKey;

    public ScreenshotViewer() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::onConfigReloaded);
        registerKeyMappings();

        ModLoadingContext mlc = ModLoadingContext.get();
        this.config = ScreenshotViewerConfig.registerConfig(mlc);
        this.modInfo = mlc.getActiveContainer().getModInfo();
        mlc.registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> "client only mod", (version, bool) -> bool));

        MinecraftForge.EVENT_BUS.register(this);
        instance = this;
    }

    private void registerKeyMappings() {
        openScreenshotsScreenKey = Util.make(new KeyBinding(translation("key", "open_screenshots_screen"), KeyConflictContext.IN_GAME, InputMappings.UNKNOWN, "key.categories.misc"), ClientRegistry::registerKeyBinding);
    }

    private void onConfigReloaded(ModConfig.Reloading event) {
        this.configListeners.forEach(ScreenshotViewerConfigListener::onConfigReloaded);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft client = Minecraft.getInstance();
        KeyBinding openScreenshotsScreenKey = getInstance().getOpenScreenshotsScreenKey();
        if(client.level != null && client.screen == null && event.getAction() == 1 && openScreenshotsScreenKey != null && openScreenshotsScreenKey.getKey().getValue() == event.getKey()) {
            client.setScreen(new ManageScreenshotsScreen(null));
        }
    }

    private static final ResourceLocation MANAGE_SCREENSHOTS_BUTTON_TEXTURE = new ResourceLocation(MODID, "textures/gui/screenshots_button.png");

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onScreenPostInit(GuiScreenEvent.InitGuiEvent.Post event) {
        Screen screen = event.getGui();
        Minecraft client = screen.getMinecraft();
        List<Widget> buttons = event.getWidgetList();

        if(config.showButtonInGamePauseMenu.get() && screen instanceof IngameMenuScreen) {
            Widget topButton = buttons.get(0);
            event.addWidget(new ImageButton(topButton.x + topButton.getWidth() + 8, topButton.y, topButton.getHeight(), topButton.getHeight(), 0, 0, 20, MANAGE_SCREENSHOTS_BUTTON_TEXTURE, 32, 64, button -> {
                client.setScreen(new ManageScreenshotsScreen(screen));
            }, (button, matrices, mouseX, mouseY) -> {
                screen.renderTooltip(matrices, client.font.split(translatable("screen", "manage_screenshots"), Math.max(screen.width / 2 - 43, 170)), mouseX, mouseY);
            }, translatable("screen", "manage_screenshots")));
        }
        if(config.showButtonOnTitleScreen.get() && screen instanceof MainMenuScreen) {
            Optional<ImageButton> accessibilityWidgetOpt = buttons.stream()
                    .filter(ImageButton.class::isInstance)
                    .map(ImageButton.class::cast)
                    .filter(widget -> widget.getMessage().equals(new TranslationTextComponent("narrator.button.accessibility")))
                    .findFirst();

            int x = accessibilityWidgetOpt.map(widget -> widget.x).orElse(screen.width / 2 + 104);
            int y = accessibilityWidgetOpt.map(widget -> widget.y).orElse(screen.height / 4 + 132);
            int width = accessibilityWidgetOpt.map(ImageButton::getWidth).orElse(20);
            int height = accessibilityWidgetOpt.map(ImageButton::getHeight).orElse(20);
            event.addWidget(new ImageButton(x + width + 4, y, width, height, 0, 0, 20, MANAGE_SCREENSHOTS_BUTTON_TEXTURE, 32, 64, button -> {
                client.setScreen(new ManageScreenshotsScreen(screen));
            }, (button, matrices, mouseX, mouseY) -> {
                screen.renderTooltip(matrices, client.font.split(translatable("screen", "manage_screenshots"), Math.max(screen.width / 2 - 43, 170)), mouseX, mouseY);
            }, translatable("screen", "manage_screenshots")));
        }
    }

    public ScreenshotViewerConfig getConfig() {
        return config;
    }

    public KeyBinding getOpenScreenshotsScreenKey() {
        return openScreenshotsScreenKey;
    }

    public Optional<BiFunction<Minecraft, Screen, Screen>> getConfigScreenFactory() {
        return ConfigGuiHandler.getGuiFactoryFor((ModInfo) this.modInfo);
    }

    public void registerConfigListener(ScreenshotViewerConfigListener listener) {
        this.configListeners.add(listener);
    }

    public void unregisterConfigListener(ScreenshotViewerConfigListener listener) {
        this.configListeners.remove(listener);
    }

    @Nonnull
    public static ScreenshotViewer getInstance() {
        if(instance == null) {
            throw new IllegalStateException("Screenshot Viewer is not loaded yet!");
        }

        return instance;
    }

    public static String translation(String prefix, String suffix) {
        return prefix + '.' + MODID + '.' + suffix;
    }

    public static ITextComponent translatable(String prefix, String suffix) {
        return new TranslationTextComponent(translation(prefix, suffix));
    }
}

package io.github.lgatodu47.screenshot_viewer;

import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfig;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfigListener;
import io.github.lgatodu47.screenshot_viewer.screens.ManageScreenshotsScreen;
import io.github.lgatodu47.screenshot_viewer.screens.widgets.ButtonTooltip;
import io.github.lgatodu47.screenshot_viewer.screens.widgets.ScreenshotViewerButton;
import io.github.lgatodu47.screenshot_viewer.screens.widgets.ScreenshotViewerImageButton;
import io.github.lgatodu47.screenshot_viewer.util.DynamicButtonTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

@Mod(modid = ScreenshotViewer.MODID, name = "Screenshot Viewer", version = "1.1.1", acceptedMinecraftVersions = "1.12.2", clientSideOnly = true, guiFactory = "io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfigGuiFactory")
public class ScreenshotViewer {
    public static final String MODID = "screenshot_viewer";

    @Mod.Instance(ScreenshotViewer.MODID)
    private static ScreenshotViewer instance;

    private final List<ScreenshotViewerConfigListener> configListeners = new ArrayList<>();
    private ModContainer modContainer;
    private ScreenshotViewerConfig config;
    private KeyBinding openScreenshotsScreenKey;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        this.modContainer = Loader.instance().activeModContainer();
        this.config = ScreenshotViewerConfig.registerConfig(event.getModConfigurationDirectory());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        registerKeyBindings();
        MinecraftForge.EVENT_BUS.register(new ScreenshotViewerEvents());
    }

    private void registerKeyBindings() {
        openScreenshotsScreenKey = new KeyBinding(translation("key", "open_screenshots_screen"), KeyConflictContext.IN_GAME, Keyboard.KEY_NONE, "key.categories.misc");
        ClientRegistry.registerKeyBinding(openScreenshotsScreenKey);
    }

    public ScreenshotViewerConfig getConfig() {
        return config;
    }

    public KeyBinding getOpenScreenshotsScreenKey() {
        return openScreenshotsScreenKey;
    }

    public void registerConfigListener(ScreenshotViewerConfigListener listener) {
        this.configListeners.add(listener);
    }

    public void unregisterConfigListener(ScreenshotViewerConfigListener listener) {
        this.configListeners.remove(listener);
    }

    public Optional<UnaryOperator<GuiScreen>> getConfigScreenFactory() {
        return Optional.ofNullable(FMLClientHandler.instance().getGuiFactoryFor(this.modContainer)).filter(IModGuiFactory::hasConfigGui).map(factory -> factory::createConfigGui);
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
        return new TextComponentTranslation(translation(prefix, suffix));
    }

    public static String translated(String prefix, String suffix) {
        return translatable(prefix, suffix).getFormattedText();
    }

    private static final ResourceLocation MANAGE_SCREENSHOTS_BUTTON_TEXTURE = new DynamicButtonTexture("screenshots_button");

    public final class ScreenshotViewerEvents {
        @SubscribeEvent
        public void onConfigReloaded(ConfigChangedEvent.OnConfigChangedEvent event) {
            if(ScreenshotViewer.MODID.equals(event.getModID())) {
                config.save();
                configListeners.forEach(ScreenshotViewerConfigListener::onConfigReloaded);
            }
        }

        @SubscribeEvent
        public void onKeyInput(InputEvent.KeyInputEvent event) {
            Minecraft client = Minecraft.getMinecraft();
            KeyBinding openScreenshotsScreenKey = getInstance().getOpenScreenshotsScreenKey();
            if(client.world != null && client.currentScreen == null && Keyboard.getEventKeyState() && openScreenshotsScreenKey != null && openScreenshotsScreenKey.getKeyCode() == Keyboard.getEventKey()) {
                client.displayGuiScreen(new ManageScreenshotsScreen(null));
            }
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        public void onScreenPostInit(GuiScreenEvent.InitGuiEvent.Post event) {
            GuiScreen screen = event.getGui();
            Minecraft client = screen.mc;
            List<GuiButton> buttons = event.getButtonList();

            if(config.showButtonInGamePauseMenu.getAsBoolean() && screen instanceof GuiIngameMenu) {
                GuiButton topButton = buttons.stream().filter(button -> button.id == 4).findFirst().orElseGet(() -> buttons.get(1));
                //noinspection SuspiciousNameCombination
                buttons.add(new ScreenshotViewerImageButton(topButton.x + topButton.width + 8, topButton.y, topButton.height, topButton.height, 0, 0, 20, MANAGE_SCREENSHOTS_BUTTON_TEXTURE, 32, 64, translated("screen", "manage_screenshots"), button -> {
                    client.displayGuiScreen(new ManageScreenshotsScreen(screen));
                }, (button, mouseX, mouseY) -> {
                    screen.drawHoveringText(client.fontRenderer.trimStringToWidth(translated("screen", "manage_screenshots"), Math.max(screen.width / 2 - 43, 170)), mouseX, mouseY);
                }));
            }
            if(config.showButtonOnTitleScreen.getAsBoolean() && screen instanceof GuiMainMenu) {
                Optional<GuiButton> languageButton = buttons.stream()
                        .filter(GuiButtonLanguage.class::isInstance)
                        .findFirst();

                int x = screen.width / 2 + 104;
                int y = languageButton.map(widget -> widget.y).orElse(screen.height / 4 + 132);
                int width = languageButton.map(widget -> widget.width).orElse(20);
                int height = languageButton.map(widget -> widget.height).orElse(20);
                buttons.add(new ScreenshotViewerImageButton(x, y, width, height, 0, 0, 20, MANAGE_SCREENSHOTS_BUTTON_TEXTURE, 32, 64, translated("screen", "manage_screenshots"), button -> {
                    client.displayGuiScreen(new ManageScreenshotsScreen(screen));
                }, (button, mouseX, mouseY) -> {
                    screen.drawHoveringText(client.fontRenderer.trimStringToWidth(translated("screen", "manage_screenshots"), Math.max(screen.width / 2 - 43, 170)), mouseX, mouseY);
                }));
            }
        }

        @SubscribeEvent
        public void onScreenButtonPressed(GuiScreenEvent.ActionPerformedEvent.Post event) {
            if(event.getGui() instanceof GuiIngameMenu || event.getGui() instanceof GuiMainMenu) {
                if(event.getButton() instanceof ScreenshotViewerButton) {
                    ((ScreenshotViewerButton) event.getButton()).onPress(event.getButton());
                }
            }
        }

        private final Field buttonListField = ObfuscationReflectionHelper.findField(GuiScreen.class, "field_146292_n");

        @SubscribeEvent
        public void onScreenRendered(GuiScreenEvent.DrawScreenEvent.Post event) {
            if(event.getGui() instanceof GuiIngameMenu || event.getGui() instanceof GuiMainMenu) {
                try {
                    @SuppressWarnings("unchecked")
                    List<GuiButton> buttons = (List<GuiButton>) buttonListField.get(event.getGui());
                    buttons.stream()
                            .filter(GuiButton::isMouseOver)
                            .filter(ButtonTooltip.class::isInstance)
                            .forEach(btn -> ((ButtonTooltip) btn).renderTooltip(btn, event.getMouseX(), event.getMouseY()));
                } catch (IllegalArgumentException | IllegalAccessException | ExceptionInInitializerError ignored) {
                }
            }
        }
    }
}

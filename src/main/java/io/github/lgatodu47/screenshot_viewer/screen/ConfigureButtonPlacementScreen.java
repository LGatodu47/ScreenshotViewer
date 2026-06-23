package io.github.lgatodu47.screenshot_viewer.screen;

import io.github.lgatodu47.catconfig.ConfigAccess;
import io.github.lgatodu47.catconfig.ConfigOption;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import io.github.lgatodu47.screenshot_viewer.config.WidgetPositionOption;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;

public class ConfigureButtonPlacementScreen extends Screen {
    private final Screen parent;
    private final ConfigAccess config;
    private final ConfigOption<WidgetPositionOption.WidgetPosition> option;
    private final Screen configuringScreen;
    private final WidgetRemover remover;
    @Nullable
    private CompletableFuture<Identifier> screenImageTexture;
    @Nullable
    private ClickableWidget elementToPlace;

    public ConfigureButtonPlacementScreen(Screen parent, ConfigAccess config, ConfigOption<WidgetPositionOption.WidgetPosition> option, Supplier<Screen> configuringScreenFactory, WidgetRemover remover) {
        super(Text.empty());
        this.parent = parent;
        this.config = config;
        this.option = option;
        this.configuringScreen = configuringScreenFactory.get();
        this.remover = remover;
    }

    @Override
    protected void init() {
        super.init();

        // first screen initialization
        if(configuringScreen.getTextRenderer() == null) {
            configuringScreen.init(width, height);
            this.elementToPlace = remover.removeWidget(configuringScreen);
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        this.configuringScreen.resize(width, height);
        this.elementToPlace = remover.removeWidget(configuringScreen);
        disposeScreenImageTexture();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if(screenImageTexture == null) {
            initScreenImageTexture(context);
            return;
        }
        renderRegularScreen(context);
        if(elementToPlace != null) {
            ScreenshotViewerUtils.renderWidget(elementToPlace, context, mouseX, mouseY, delta);
        }
        renderTipTexts(context);
    }

    protected void initScreenImageTexture(DrawContext context) {
        if (this.client != null) {
            CompletableFuture<Identifier> future = new CompletableFuture<>();
            this.screenImageTexture = future;
            configuringScreen.render(context, 0, 0, 0);
            // calling render prevents a bug where parts of the previous screen could appear.
            configuringScreen.render(context, 0, 0, 0);
            ScreenshotRecorder.takeScreenshot(this.client.getFramebuffer(), image -> {
                int hash = java.util.Objects.hash(width, height, System.nanoTime());
                Identifier textureId = Identifier.of(ScreenshotViewer.MODID, "dynamic/" + Integer.toHexString(hash));
                this.client.getTextureManager().registerTexture(textureId, new NativeImageBackedTexture(textureId::toString, image));
                future.complete(textureId);
            });
        }
    }

    protected void renderRegularScreen(DrawContext context) {
        if(screenImageTexture == null) {
            return;
        }

        Identifier texture;
        if((texture = screenImageTexture.getNow(null)) != null) {
            ScreenshotViewerUtils.drawTexture(context, texture, 0, 0, width, height, 0, 0, width, height, width, height);
            context.fillGradient(0, 0, width, height, -1072689136, -804253680);
        }
    }

    protected void renderTipTexts(DrawContext context) {
        context.drawText(textRenderer, ScreenshotViewerTexts.BUTTON_PLACEMENT_MOVEMENT, 0, 0, 0xFFFFFFFF, false);
        context.drawText(textRenderer, ScreenshotViewerTexts.BUTTON_PLACEMENT_CONFIRM, 0, 10, 0xFFFFFFFF, false);
//        context.drawText(textRenderer, ScreenshotViewerTexts.BUTTON_PLACEMENT_SNAP_TO_GRID, 0, 20, 0xFFFFFFFF, false);
        Text message = Text.literal("Warning: this is experimental, bugs (will) arise.");
        context.drawText(textRenderer, message, this.width - textRenderer.getWidth(message), 0, 0xFFFFFF15, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if(elementToPlace != null && click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            elementToPlace.setPosition((int) click.x() - elementToPlace.getWidth() / 2, (int) click.y() - elementToPlace.getHeight() / 2);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if(elementToPlace != null && click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            elementToPlace.setPosition((int) click.x() - elementToPlace.getWidth() / 2, (int) click.y() - elementToPlace.getHeight() / 2);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if(elementToPlace != null) {
            if(input.key() == GLFW.GLFW_KEY_ENTER) {
                this.config.put(option, new WidgetPositionOption.WidgetPosition(this.elementToPlace.getX(), this.elementToPlace.getY()));
                ClickableWidget.playClickSound(client.getSoundManager());
            } else if(input.key() == GLFW.GLFW_KEY_R) {
                this.config.put(option, null);
                ClickableWidget.playClickSound(client.getSoundManager());
            }
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        super.close();
        disposeScreenImageTexture();
        this.client.setScreen(parent);
    }

    private void disposeScreenImageTexture() {
        CompletableFuture<Identifier> future = this.screenImageTexture;
        this.screenImageTexture = null;
        if (future != null && this.client != null) {
            future.thenAcceptAsync(this.client.getTextureManager()::destroyTexture, this.client);
        }
    }

    @FunctionalInterface
    public interface WidgetRemover {
        @Nullable
        ClickableWidget removeWidget(Screen screen);

        static WidgetRemover ofIndex(int index) {
            return screen -> {
                try {
                    List<ClickableWidget> widgets = Screens.getButtons(screen);

                    try {
                        return widgets.remove(index);
                    } catch (UnsupportedOperationException e) { // if the list is immutable, we just get a reference of the widget
                        return widgets.get(index);
                    } catch (Throwable t) { // other exceptions: index out of bound
                        return null;
                    }
                } catch (Throwable t) {
                    return null;
                }
            };
        }

        static WidgetRemover ofPredicate(@NotNull Predicate<ClickableWidget> widgetPredicate) {
            return screen -> {
                try {
                    List<ClickableWidget> widgets = Screens.getButtons(screen);
                    ClickableWidget widget = widgets.stream().filter(widgetPredicate).findFirst().orElse(null);
                    widgets.remove(widget);
                    return widget;
                } catch (Throwable t) {
                    return null;
                }
            };
        }
    }
}

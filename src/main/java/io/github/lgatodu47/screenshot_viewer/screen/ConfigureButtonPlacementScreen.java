package io.github.lgatodu47.screenshot_viewer.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lgatodu47.catconfig.ConfigAccess;
import io.github.lgatodu47.catconfig.ConfigOption;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import io.github.lgatodu47.screenshot_viewer.config.WidgetPositionOption;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ConfigureButtonPlacementScreen extends Screen {
    private final Screen parent;
    private final ConfigAccess config;
    private final ConfigOption<WidgetPositionOption.WidgetPosition> option;
    private final Screen configuringScreen;
    private final WidgetRemover remover;
    @Nullable
    private CompletableFuture<NativeImageBackedTexture> screenImageTexture;
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
            configuringScreen.init(client, width, height);
            this.elementToPlace = remover.removeWidget(configuringScreen);
        }
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        this.configuringScreen.resize(client, width, height);
        this.elementToPlace = remover.removeWidget(configuringScreen);
        if(screenImageTexture != null) {
            this.screenImageTexture.thenAcceptAsync(NativeImageBackedTexture::close, this.client);
            this.screenImageTexture = null;
        }
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
        screenImageTexture = CompletableFuture.supplyAsync(() -> {
            configuringScreen.render(context, 0, 0, 0);
            // calling render prevents a bug where parts of the previous screen could appear.
            configuringScreen.render(context, 0, 0, 0);
            return new NativeImageBackedTexture(ScreenshotRecorder.takeScreenshot(client.getFramebuffer()));
        }, this.client);
    }

    protected void renderRegularScreen(DrawContext context) {
        if(screenImageTexture == null) {
            return;
        }

        MatrixStack stack = context.getMatrices();
        NativeImageBackedTexture texture;
        if((texture = screenImageTexture.getNow(null)) != null) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShaderTexture(0, texture.getGlId());
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(515);

            stack.translate(0, 0, -5);
            ScreenshotViewerUtils.drawTexture(context, 0, 0, width, height, 0, 0, width, height, width, height);
            stack.translate(0, 0, 2);
            context.fillGradient(0, 0, width, height, -1072689136, -804253680);
            stack.translate(0, 0, 3);

            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(elementToPlace != null && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            elementToPlace.setPosition((int) mouseX - elementToPlace.getWidth() / 2, (int) mouseY - elementToPlace.getHeight() / 2);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if(elementToPlace != null && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            elementToPlace.setPosition((int) mouseX - elementToPlace.getWidth() / 2, (int) mouseY - elementToPlace.getHeight() / 2);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(elementToPlace != null) {
            if(keyCode == GLFW.GLFW_KEY_ENTER) {
                this.config.put(option, new WidgetPositionOption.WidgetPosition(this.elementToPlace.getX(), this.elementToPlace.getY()));
                ClickableWidget.playClickSound(client.getSoundManager());
            } else if(keyCode == GLFW.GLFW_KEY_R) {
                this.config.put(option, null);
                ClickableWidget.playClickSound(client.getSoundManager());
            }
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        super.close();
        if(screenImageTexture != null) {
            this.screenImageTexture.thenAcceptAsync(NativeImageBackedTexture::close, this.client);
        }
        this.client.setScreen(parent);
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
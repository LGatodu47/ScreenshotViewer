package io.github.lgatodu47.screenshot_viewer.screen;

import io.github.lgatodu47.catconfig.ConfigAccess;
import io.github.lgatodu47.catconfig.ConfigOption;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import io.github.lgatodu47.screenshot_viewer.config.WidgetPositionOption;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ConfigureButtonPlacementScreen extends Screen {
    private final Screen parent;
    private final ConfigAccess config;
    private final ConfigOption<WidgetPositionOption.WidgetPosition> option;
    private final Screen configuringScreen;
    private final WidgetRemover remover;
    @Nullable
    private ClickableWidget elementToPlace;
    @Nullable
    private WidgetPositionOption.WidgetPosition previousPosition;

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

        configuringScreen.init(width, height);
        initElementToPlace();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        this.configuringScreen.resize(width, height);
        initElementToPlace();
    }

    private void initElementToPlace() {
        this.elementToPlace = remover.removeWidget(configuringScreen);
        if(elementToPlace != null) {
            this.previousPosition = makeWidgetPosition(elementToPlace);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        configuringScreen.render(context, 0, 0, deltaTicks);
        context.fillGradient(0, 0, width, height, -1072689136, -804253680);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if(elementToPlace != null) {
            ScreenshotViewerUtils.renderWidget(elementToPlace, context, mouseX, mouseY, delta);
        }
        renderTipTexts(context);
    }

    private final Text warningText = Text.literal("/!\\ Warning: this will not work if you change the gui scale or window size.");

    protected void renderTipTexts(DrawContext context) {
        context.drawText(textRenderer, ScreenshotViewerTexts.BUTTON_PLACEMENT_MOVEMENT, 0, 0, 0xFFFFFFFF, false);
        context.drawWrappedText(textRenderer, ScreenshotViewerTexts.BUTTON_PLACEMENT_CONFIRM, 0, 10, 250, 0xFF15FFFF, false);
//        context.drawText(textRenderer, ScreenshotViewerTexts.BUTTON_PLACEMENT_SNAP_TO_GRID, 0, 20, 0xFFFFFFFF, false);
        context.drawWrappedText(textRenderer, warningText, this.width - 150, 0, 150, 0xFFFFFF15, false);
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
                this.config.put(option, makeWidgetPosition(elementToPlace));
                close();
                ClickableWidget.playClickSound(client.getSoundManager());
            } else if(input.key() == GLFW.GLFW_KEY_C && previousPosition != null) {
                this.elementToPlace.setPosition(previousPosition.x(), previousPosition.y());
                ClickableWidget.playClickSound(client.getSoundManager());
            } else if(input.key() == GLFW.GLFW_KEY_R) {
                WidgetPositionOption.WidgetPosition defaultPos = option.defaultValue();
                if(defaultPos == null) {
                    this.config.put(option, null);
                    close();
                    return true;
                }
                this.elementToPlace.setPosition(defaultPos.x(), defaultPos.y());
                ClickableWidget.playClickSound(client.getSoundManager());
            } else if(input.key() == GLFW.GLFW_KEY_ESCAPE) {
                close();
            }
            return true;
        }
        return super.keyPressed(input);
    }

    private WidgetPositionOption.WidgetPosition makeWidgetPosition(ClickableWidget elementToPlace) {
        return new WidgetPositionOption.WidgetPosition(elementToPlace.getX(), elementToPlace.getY());
    }

    @Override
    public void close() {
        super.close();
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

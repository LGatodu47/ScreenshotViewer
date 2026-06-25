package io.github.lgatodu47.screenshot_viewer.screen;

import io.github.lgatodu47.catconfig.ConfigAccess;
import io.github.lgatodu47.catconfig.ConfigOption;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import io.github.lgatodu47.screenshot_viewer.config.WidgetPositionOption;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
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
    private AbstractWidget elementToPlace;
    @Nullable
    private WidgetPositionOption.WidgetPosition previousPosition;

    public ConfigureButtonPlacementScreen(Screen parent, ConfigAccess config, ConfigOption<WidgetPositionOption.WidgetPosition> option, Supplier<Screen> configuringScreenFactory, WidgetRemover remover) {
        super(Component.empty());
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
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        configuringScreen.extractRenderState(context, 0, 0, deltaTicks);
        context.fillGradient(0, 0, width, height, -1072689136, -804253680);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if(elementToPlace != null) {
            ScreenshotViewerUtils.renderWidget(elementToPlace, context, mouseX, mouseY, delta);
        }
        renderTipTexts(context);
    }

    private final Component warningText = Component.literal("/!\\ Warning: this will not work if you change the gui scale or window size.");

    protected void renderTipTexts(GuiGraphicsExtractor context) {
        context.text(font, ScreenshotViewerTexts.BUTTON_PLACEMENT_MOVEMENT, 0, 0, 0xFFFFFFFF, false);
        context.textWithWordWrap(font, ScreenshotViewerTexts.BUTTON_PLACEMENT_CONFIRM, 0, 10, 250, 0xFF15FFFF, false);
//        context.drawText(textRenderer, ScreenshotViewerTexts.BUTTON_PLACEMENT_SNAP_TO_GRID, 0, 20, 0xFFFFFFFF, false);
        context.textWithWordWrap(font, warningText, this.width - 150, 0, 150, 0xFFFFFF15, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if(elementToPlace != null && click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            elementToPlace.setPosition((int) click.x() - elementToPlace.getWidth() / 2, (int) click.y() - elementToPlace.getHeight() / 2);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if(elementToPlace != null && click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            elementToPlace.setPosition((int) click.x() - elementToPlace.getWidth() / 2, (int) click.y() - elementToPlace.getHeight() / 2);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if(elementToPlace != null) {
            if(input.key() == GLFW.GLFW_KEY_ENTER) {
                this.config.put(option, makeWidgetPosition(elementToPlace));
                onClose();
                AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
            } else if(input.key() == GLFW.GLFW_KEY_C && previousPosition != null) {
                this.elementToPlace.setPosition(previousPosition.x(), previousPosition.y());
                AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
            } else if(input.key() == GLFW.GLFW_KEY_R) {
                WidgetPositionOption.WidgetPosition defaultPos = option.defaultValue();
                if(defaultPos == null) {
                    this.config.put(option, null);
                    onClose();
                    return true;
                }
                this.elementToPlace.setPosition(defaultPos.x(), defaultPos.y());
                AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
            } else if(input.key() == GLFW.GLFW_KEY_ESCAPE) {
                onClose();
            }
            return true;
        }
        return super.keyPressed(input);
    }

    private WidgetPositionOption.WidgetPosition makeWidgetPosition(AbstractWidget elementToPlace) {
        return new WidgetPositionOption.WidgetPosition(elementToPlace.getX(), elementToPlace.getY());
    }

    @Override
    public void onClose() {
        super.onClose();
        this.minecraft.setScreen(parent);
    }

    @FunctionalInterface
    public interface WidgetRemover {
        @Nullable
        AbstractWidget removeWidget(Screen screen);

        static WidgetRemover ofIndex(int index) {
            return screen -> {
                try {
                    List<AbstractWidget> widgets = Screens.getWidgets(screen);

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

        static WidgetRemover ofPredicate(@NotNull Predicate<AbstractWidget> widgetPredicate) {
            return screen -> {
                try {
                    List<AbstractWidget> widgets = Screens.getWidgets(screen);
                    AbstractWidget widget = widgets.stream().filter(widgetPredicate).findFirst().orElse(null);
                    widgets.remove(widget);
                    return widget;
                } catch (Throwable t) {
                    return null;
                }
            };
        }
    }
}

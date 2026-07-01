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
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ConfigureButtonPlacementScreen extends Screen {
    private final Screen parent;
    private final ConfigAccess config;
    private final ConfigOption<WidgetPositionOption.WidgetPosition> option;
    private final Screen configuringScreen;
    private final WidgetRemover remover;
    @Nullable
    private AbstractWidget referenceWidget;
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
        this.previousPosition = null;
        this.elementToPlace = remover.removeWidget(configuringScreen);
        Optional<WidgetPositionOption.WidgetPosition> optPos = config.get(option);
        if(elementToPlace != null) {
            this.previousPosition = optPos.orElseGet(() -> makeWidgetPosition(elementToPlace));
        }
        List<AbstractWidget> screenWidgets = Screens.getWidgets(configuringScreen);
        this.referenceWidget = screenWidgets.isEmpty() ? null : screenWidgets.getFirst();
        if(optPos.isPresent() && previousPosition != null) {
            int x = previousPosition.x() + (referenceWidget == null ? 0 : referenceWidget.getX());
            int y = previousPosition.y() + (referenceWidget == null ? 0 : referenceWidget.getY());
            this.elementToPlace.setPosition(x, y);
        }
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        configuringScreen.extractRenderState(context, 0, 0, deltaTicks);
        context.fillGradient(0, 0, width, height, -1072689136, -804253680);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if(elementToPlace != null) {
            ScreenshotViewerUtils.renderWidget(elementToPlace, context, mouseX, mouseY, delta);
        }
        renderTipTexts(context);
    }

    protected void renderTipTexts(GuiGraphicsExtractor context) {
        context.text(font, ScreenshotViewerTexts.BUTTON_PLACEMENT_MOVEMENT, 0, 0, 0xFFFFFFFF, false);
        context.textWithWordWrap(font, ScreenshotViewerTexts.BUTTON_PLACEMENT_CONFIRM, 0, 10, 250, 0xFF15FFFF, false);
//        context.drawText(textRenderer, ScreenshotViewerTexts.BUTTON_PLACEMENT_SNAP_TO_GRID, 0, 20, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubled) {
        if(elementToPlace != null && click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            elementToPlace.setPosition((int) click.x() - elementToPlace.getWidth() / 2, (int) click.y() - elementToPlace.getHeight() / 2);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent click, double offsetX, double offsetY) {
        if(elementToPlace != null && click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            elementToPlace.setPosition((int) click.x() - elementToPlace.getWidth() / 2, (int) click.y() - elementToPlace.getHeight() / 2);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent input) {
        if(elementToPlace != null) {
            if(input.key() == GLFW.GLFW_KEY_ENTER) {
                this.config.put(option, makeWidgetPosition(elementToPlace));
                onClose();
                AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
            } else if(input.key() == GLFW.GLFW_KEY_C && previousPosition != null) {
                this.elementToPlace.setPosition(previousPosition.x(), previousPosition.y());
                AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
            } else if(input.key() == GLFW.GLFW_KEY_R) {
                AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
                WidgetPositionOption.WidgetPosition defaultPos = option.defaultValue();
                if(defaultPos == null) {
                    this.config.put(option, null);
                    onClose();
                    return true;
                }
                this.elementToPlace.setPosition(defaultPos.x(), defaultPos.y());
            } else if(input.key() == GLFW.GLFW_KEY_ESCAPE) {
                onClose();
                AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
            }
            return true;
        }
        return super.keyPressed(input);
    }

    private WidgetPositionOption.WidgetPosition makeWidgetPosition(AbstractWidget elementToPlace) {
        if(referenceWidget == null) {
            return new WidgetPositionOption.WidgetPosition(elementToPlace.getX(), elementToPlace.getY());
        }
        return new WidgetPositionOption.WidgetPosition(elementToPlace.getX() - referenceWidget.getX(), elementToPlace.getY() - referenceWidget.getY());
    }

    @Override
    public void onClose() {
        super.onClose();
        this.minecraft.gui.setScreen(parent);
    }

    @FunctionalInterface
    public interface WidgetRemover {
        @Nullable
        AbstractWidget removeWidget(Screen screen);

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

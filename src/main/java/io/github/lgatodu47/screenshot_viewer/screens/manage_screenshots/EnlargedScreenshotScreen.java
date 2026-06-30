package io.github.lgatodu47.screenshot_viewer.screens.manage_screenshots;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import io.github.lgatodu47.screenshot_viewer.screens.ScreenshotViewerTexts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

import static io.github.lgatodu47.screenshot_viewer.screens.manage_screenshots.ScreenshotPropertiesMenu.*;

class EnlargedScreenshotScreen extends Screen {
    @Nullable
    private ScreenshotImageHolder showing;
    @Nullable
    private ScreenshotImageList imageList;
    private final PropertiesDisplay properties;
    private final AbstractWidget doneBtn, nextBtn, prevBtn, openBtn, copyBtn, deleteBtn, renameBtn;

    EnlargedScreenshotScreen(PropertiesDisplay properties) {
        super(Component.empty());
        this.properties = properties;
        this.doneBtn = new ManageScreenshotsScreen.ExtendedButtonWidget(0, 0, 52, 20, CommonComponents.GUI_DONE, btn -> onClose());
        this.prevBtn = new ManageScreenshotsScreen.ExtendedButtonWidget(0, 0, 20, 20, Component.literal("<"), btn -> previousScreenshot());
        this.nextBtn = new ManageScreenshotsScreen.ExtendedButtonWidget(0, 0, 20, 20, Component.literal(">"), btn -> nextScreenshot());
        this.openBtn = makeIconWidget(OPEN_ICON, ScreenshotViewerTexts.OPEN_FILE, ScreenshotImageHolder::openFile);
        this.copyBtn = makeIconWidget(COPY_ICON, ScreenshotViewerTexts.COPY, ScreenshotImageHolder::copyScreenshot);
        this.deleteBtn = makeIconWidget(DELETE_ICON, ScreenshotViewerTexts.DELETE, ScreenshotImageHolder::requestFileDeletion);
        this.renameBtn = makeIconWidget(RENAME_ICON, ScreenshotViewerTexts.RENAME_FILE, ScreenshotImageHolder::renameFile);
    }

    private AbstractWidget makeIconWidget(Identifier texture, Component description, Consumer<ScreenshotImageHolder> action) {
        return new ManageScreenshotsScreen.ExtendedTexturedButtonWidget(0, 0, 20, 20, texture, btn -> {
            if(showing != null) {
                action.accept(showing);
            }
        }, description, description);
    }

    // Package-private allows the main screen to show this child screen
    void show(ScreenshotImageHolder showing, ScreenshotImageList imageList) {
        this.showing = showing;
        this.imageList = imageList;
        updateButtonsState();
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        int spacing = 4;
        addPositioned(doneBtn, (width - 52) / 2, height - 20 - spacing * 2);
        addPositioned(prevBtn, spacing * 2, (height - 20) / 2);
        int rightButtonsX = width - spacing * 2 - 20;
        addPositioned(nextBtn, width - spacing * 2 - 20, (height - 20) / 2);
        addPositioned(openBtn, rightButtonsX, (height) - 20 * 5 - spacing * 6);
        addPositioned(copyBtn, rightButtonsX, (height) - 20 * 4 - spacing * 5);
        addPositioned(deleteBtn, rightButtonsX, (height) - 20 * 3 - spacing * 4);
        addPositioned(renameBtn, rightButtonsX, (height) - 20 * 2 - spacing * 3);
    }

    private void addPositioned(AbstractWidget button, int x, int y) {
        button.setX(x);
        button.setY(y);
        addRenderableWidget(button);
    }

    private void nextScreenshot() {
        if (hasInfo()) {
            int i = showing.indexInList() + 1;
            if (i < imageList.size()) {
                showing = imageList.getScreenshot(i);
                updateButtonsState();
            }
        }
    }

    private void previousScreenshot() {
        if (hasInfo()) {
            int i = showing.indexInList() - 1;
            if (i >= 0) {
                showing = imageList.getScreenshot(i);
                updateButtonsState();
            }
        }
    }

    private void updateButtonsState() {
        if (hasInfo()) {
            int i = showing.indexInList();
            prevBtn.active = i > 0;
            nextBtn.active = i < imageList.size() - 1;
        }
    }

    private boolean hasInfo() {
        return showing != null && imageList != null;
    }

    boolean renders() {
        return hasInfo();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    }

    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTicks, boolean updateHoverState) {
        this.children().forEach(element -> {
            if(element instanceof Renderable drawable) {
                drawable.extractRenderState(context, mouseX, mouseY, partialTicks);
            }
            if(element instanceof ManageScreenshotsScreen.CustomHoverState state) {
                // feeding negative values updates the state by tricking it into thinking that it isn't hovered.
                // it would otherwise still appear hovered when the dialog screen pops up and the tooltip would not disappear.
                int mul = updateHoverState ? 1 : -1;
                state.updateHoveredState(mouseX * mul, mouseY * mul);
            }
        });
    }

    public void renderImage(GuiGraphicsExtractor context) {
        if (showing != null) {
            final int spacing = 8;

            NativeImage image = showing.image();
            if (image != null) {
                float imgRatio = (float) image.getWidth() / image.getHeight();
                int texHeight = height - spacing * 3 - 20;
                int texWidth = (int) (texHeight * imgRatio);

                Identifier texture = showing.textureId();
                if (texture != null) {
                    ScreenshotViewerUtils.drawTexture(context, texture, (width - texWidth) / 2, spacing, texWidth, texHeight, 0, 0, image.getWidth(), image.getHeight(), image.getWidth(), image.getHeight());
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0) {
            nextScreenshot();
        }
        if (verticalAmount < 0) {
            previousScreenshot();
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == InputConstants.KEY_LEFT) {
            previousScreenshot();
            return true;
        }
        if (input.key() == InputConstants.KEY_RIGHT) {
            nextScreenshot();
            return true;
        }
        if(showing != null && input.key() == InputConstants.KEY_C && (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            showing.copyScreenshot();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubled) {
        if(showing != null && click.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            this.properties.showProperties(click.x(), click.y(), showing);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void onClose() {
        showing = null;
        imageList = null;
    }

    @FunctionalInterface
    interface PropertiesDisplay {
        void showProperties(double mouseX, double mouseY, ScreenshotImageHolder showing);
    }
}

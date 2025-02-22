package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerTexts;
import io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots.ManageScreenshotsScreen.ExtendedButtonWidget;
import io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots.ManageScreenshotsScreen.ExtendedTexturedButtonWidget;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

import static io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots.ScreenshotPropertiesMenu.*;

class EnlargedScreenshotScreen extends Screen {
    @Nullable
    private ScreenshotImageHolder showing;
    @Nullable
    private ScreenshotImageList imageList;
    private final PropertiesDisplay properties;
    private final ClickableWidget doneBtn, nextBtn, prevBtn, openBtn, copyBtn, deleteBtn, renameBtn;

    EnlargedScreenshotScreen(PropertiesDisplay properties) {
        super(Text.empty());
        this.properties = properties;
        this.doneBtn = new ExtendedButtonWidget(0, 0, 52, 20, ScreenTexts.DONE, btn -> close());
        this.prevBtn = new ExtendedButtonWidget(0, 0, 20, 20, Text.literal("<"), btn -> previousScreenshot());
        this.nextBtn = new ExtendedButtonWidget(0, 0, 20, 20, Text.literal(">"), btn -> nextScreenshot());
        this.openBtn = makeIconWidget(OPEN_ICON, ScreenshotViewerTexts.OPEN_FILE, ScreenshotImageHolder::openFile);
        this.copyBtn = makeIconWidget(COPY_ICON, ScreenshotViewerTexts.COPY, ScreenshotImageHolder::copyScreenshot);
        this.deleteBtn = makeIconWidget(DELETE_ICON, ScreenshotViewerTexts.DELETE, ScreenshotImageHolder::requestFileDeletion);
        this.renameBtn = makeIconWidget(RENAME_ICON, ScreenshotViewerTexts.RENAME_FILE, ScreenshotImageHolder::renameFile);
    }

    private ClickableWidget makeIconWidget(Identifier texture, Text description, Consumer<ScreenshotImageHolder> action) {
        return new ExtendedTexturedButtonWidget(0, 0, 20, 20, texture, btn -> {
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
        clearChildren();
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

    private void addPositioned(ClickableWidget button, int x, int y) {
        button.setX(x);
        button.setY(y);
        addDrawableChild(button);
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
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks, boolean updateHoverState) {
        this.children().forEach(element -> {
            if(element instanceof Drawable drawable) {
                drawable.render(context, mouseX, mouseY, partialTicks);
            }
            if(element instanceof ManageScreenshotsScreen.CustomHoverState state) {
                // feeding negative values updates the state by tricking it into thinking that it isn't hovered.
                // it would otherwise still appear hovered when the dialog screen pops up and the tooltip would not disappear.
                int mul = updateHoverState ? 1 : -1;
                state.updateHoveredState(mouseX * mul, mouseY * mul);
            }
        });
    }

    public void renderImage(DrawContext context) {
        if (showing != null) {
            final int spacing = 8;

            NativeImage image = showing.image();
            if (image != null) {
                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.setShaderTexture(0, showing.imageId());
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(515);
                RenderSystem.enableBlend();
                float imgRatio = (float) image.getWidth() / image.getHeight();
                int texHeight = height - spacing * 3 - 20;
                int texWidth = (int) (texHeight * imgRatio);

                context.getMatrices().translate(0, 0, 1);
                ScreenshotViewerUtils.drawTexture(context, (width - texWidth) / 2, spacing, texWidth, texHeight, 0, 0, image.getWidth(), image.getHeight(), image.getWidth(), image.getHeight());
                context.getMatrices().translate(0, 0, -1);
                RenderSystem.disableDepthTest();
                RenderSystem.disableBlend();
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputUtil.GLFW_KEY_LEFT) {
            previousScreenshot();
            return true;
        }
        if (keyCode == InputUtil.GLFW_KEY_RIGHT) {
            nextScreenshot();
            return true;
        }
        if(showing != null && keyCode == InputUtil.GLFW_KEY_C && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            showing.copyScreenshot();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(showing != null && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            this.properties.showProperties(mouseX, mouseY, showing);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        showing = null;
        imageList = null;
    }

    @FunctionalInterface
    interface PropertiesDisplay {
        void showProperties(double mouseX, double mouseY, ScreenshotImageHolder showing);
    }
}

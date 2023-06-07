package io.github.lgatodu47.screenshot_viewer.screens;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

class EnlargedScreenshotScreen extends Screen {
    @Nullable
    private ScreenshotImageHolder showing;
    @Nullable
    private ScreenshotImageList imageList;
    private final Button doneBtn, nextBtn, prevBtn;

    EnlargedScreenshotScreen() {
        super(Component.empty());
        this.doneBtn = Button.builder(CommonComponents.GUI_DONE, btn -> onClose()).width(52).build();
        this.prevBtn = Button.builder(Component.literal("<"), btn -> previousScreenshot()).width(20).build();
        this.nextBtn = Button.builder(Component.literal(">"), btn -> nextScreenshot()).width(20).build();
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
        addUpdatedButton(doneBtn, (width - 52) / 2, height - 20 - 8);
        addUpdatedButton(prevBtn, 8, (height - 20) / 2);
        addUpdatedButton(nextBtn, width - 8 - 20, (height - 20) / 2);
    }

    private void addUpdatedButton(Button button, int x, int y) {
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
    public void renderBackground(PoseStack matrices) {
        fillGradient(matrices, 0, 0, this.width, this.height, -1072689136, -804253680);
    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        if (showing != null) {
            final int spacing = 8;

            NativeImage image = showing.image();
            if (image != null) {
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.setShaderTexture(0, showing.imageId());
                RenderSystem.enableBlend();
                float imgRatio = (float) image.getWidth() / image.getHeight();
                int texHeight = height - spacing * 3 - 20;
                int texWidth = (int) (texHeight * imgRatio);
                GuiComponent.blit(matrices, (width - texWidth) / 2, spacing, texWidth, texHeight, 0, 0, image.getWidth(), image.getHeight(), image.getWidth(), image.getHeight());
                RenderSystem.disableBlend();
            }

            super.render(matrices, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount > 0) {
            nextScreenshot();
        }
        if (amount < 0) {
            previousScreenshot();
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_LEFT) {
            previousScreenshot();
            return true;
        }
        if (keyCode == InputConstants.KEY_RIGHT) {
            nextScreenshot();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        showing = null;
        imageList = null;
    }
}

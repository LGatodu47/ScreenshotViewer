package io.github.lgatodu47.screenshot_viewer.screens;

import io.github.lgatodu47.screenshot_viewer.screens.widgets.ButtonAction;
import io.github.lgatodu47.screenshot_viewer.screens.widgets.ScreenshotViewerButton;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.IOException;

class EnlargedScreenshotScreen extends GuiScreen {
    @Nullable
    private ScreenshotImageHolder showing;
    @Nullable
    private ScreenshotImageList imageList;
    private final GuiButton doneBtn, nextBtn, prevBtn;

    EnlargedScreenshotScreen() {
        this.doneBtn = new ScreenshotViewerButton(0, 0, 52, 20, ManageScreenshotsScreen.GUI_DONE, btn -> close());
        this.prevBtn = new ScreenshotViewerButton(0, 0, 20, 20, "<", btn -> previousScreenshot());
        this.nextBtn = new ScreenshotViewerButton(0, 0, 20, 20, ">", btn -> nextScreenshot());
    }

    // Package-private allows the main screen to show this child screen
    void show(ScreenshotImageHolder showing, ScreenshotImageList imageList) {
        this.showing = showing;
        this.imageList = imageList;
        updateButtonsState();
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        addUpdatedButton(doneBtn, (width - 52) / 2, height - 20 - 8);
        addUpdatedButton(prevBtn, 8, (height - 20) / 2);
        addUpdatedButton(nextBtn, width - 8 - 20, (height - 20) / 2);
    }

    private void addUpdatedButton(GuiButton button, int x, int y) {
        button.x = x;
        button.y = y;
        addButton(button);
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
            prevBtn.enabled = i > 0;
            nextBtn.enabled = i < imageList.size() - 1;
        }
    }

    private boolean hasInfo() {
        return showing != null && imageList != null;
    }

    boolean renders() {
        return hasInfo();
    }

    @Override
    public void drawWorldBackground(int tint) {
        this.drawGradientRect(0, 0, this.width, this.height, -1072689136, -804253680);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float delta) {
        if (showing != null) {
            final int spacing = 8;

            BufferedImage image = showing.image();
            if (image != null) {
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                GlStateManager.bindTexture(showing.imageId());
                GlStateManager.enableBlend();
                float imgRatio = (float) image.getWidth() / image.getHeight();
                int texHeight = height - spacing * 3 - 20;
                int texWidth = (int) (texHeight * imgRatio);
                drawScaledCustomSizeModalRect((width - texWidth) / 2, spacing, 0, 0, image.getWidth(), image.getHeight(), texWidth, texHeight, image.getWidth(), image.getHeight());
                GlStateManager.disableBlend();
            }

            super.drawScreen(mouseX, mouseY, delta);
        }
    }

    @Override
    protected void actionPerformed(@Nonnull GuiButton button) {
        if(button instanceof ButtonAction) {
            ((ButtonAction) button).onPress(button);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();
        if(scroll > 0) {
            nextScreenshot();
        }
        if(scroll < 0) {
            previousScreenshot();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if(keyCode == Keyboard.KEY_LEFT) {
            previousScreenshot();
            return;
        }
        if(keyCode == Keyboard.KEY_RIGHT) {
            nextScreenshot();
            return;
        }
        if(keyCode == Keyboard.KEY_ESCAPE) {
            close();
        }
    }

    public void close() {
        showing = null;
        imageList = null;
    }
}

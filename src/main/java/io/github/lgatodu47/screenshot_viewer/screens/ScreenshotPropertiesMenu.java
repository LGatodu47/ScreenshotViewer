package io.github.lgatodu47.screenshot_viewer.screens;

import com.google.common.collect.Lists;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.screens.widgets.ButtonAction;
import io.github.lgatodu47.screenshot_viewer.screens.widgets.ScreenshotViewerButton;
import io.github.lgatodu47.screenshot_viewer.screens.widgets.ScreenshotViewerImageButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static io.github.lgatodu47.screenshot_viewer.screens.ManageScreenshotsScreen.LOGGER;

class ScreenshotPropertiesMenu extends Gui {
    private static final int BUTTON_SIZE = 19;

    private final Supplier<Minecraft> mcSupplier;
    private final IntSupplier parentWidth, parentHeight;
    private final List<ScreenshotViewerButton> buttons = new ArrayList<>();

    private int x, y, width, height;
    @Nonnull
    private String fileName = "";
    private boolean shouldRender;
    @Nullable
    private GuiScreen childScreen;

    ScreenshotPropertiesMenu(Supplier<Minecraft> mcSupplier, IntSupplier parentWidth, IntSupplier parentHeight) {
        this.mcSupplier = mcSupplier;
        this.parentWidth = parentWidth;
        this.parentHeight = parentHeight;
    }

    void tick() {
        if (childScreen != null) {
            childScreen.updateScreen();
        }
    }

    void show(int x, int y, Runnable remover, File screenshotFile, Consumer<File> fileUpdater) {
        buttons.clear();

        this.fileName = screenshotFile.getName();
        final int spacing = 2;
        Consumer<Boolean> deleteAction = value -> {
            if (value) {
                if (screenshotFile.exists() && !screenshotFile.delete()) {
                    LOGGER.error("Failed to delete 'screenshot' file at location '{}'", screenshotFile.toPath().toAbsolutePath());
                    return;
                }
                remover.run();
            }
            hide();
        };
        // List of info about the buttons that are included in the widget.
        List<Triple<Integer, String, ButtonAction>> buttonInfo = Lists.newArrayList(
                Triple.of(0, ScreenshotViewer.translated("screen", "button.delete_screenshot"), btn -> {
                    if (ManageScreenshotsScreen.CONFIG.promptWhenDeletingScreenshot.getAsBoolean()) {
                        childScreen = new ConfirmationScreen(deleteAction, screenshotFile.getName());
                        childScreen.setWorldAndResolution(this.mcSupplier.get(), parentWidth.getAsInt(), parentHeight.getAsInt());
                    } else {
                        deleteAction.accept(true);
                    }
                }),
                Triple.of(1, ScreenshotViewer.translated("screen", "button.open_file"), btn -> ManageScreenshotsScreen.openFile(screenshotFile)),
                Triple.of(3, ScreenshotViewer.translated("screen", "button.rename_file"), btn -> {
                    childScreen = new RenameScreen(fileName.substring(0, fileName.lastIndexOf('.')), s -> {
                        try {
                            Path moved = Files.move(screenshotFile.toPath(), screenshotFile.toPath().resolveSibling(s));
                            fileUpdater.accept(moved.toFile());
                        } catch (IOException e) {
                            LOGGER.error("Failed to rename 'screenshot' file at '" + screenshotFile.toPath().toAbsolutePath() + "' from '" + screenshotFile.getName() + "' to '" + s + "'", e);
                        }
                    }, this::hide);
                    childScreen.setWorldAndResolution(this.mcSupplier.get(), parentWidth.getAsInt(), parentHeight.getAsInt());
                }),
                Triple.of(2, ScreenshotViewer.translated("screen", "button.close_properties_menu"), btn -> hide())
        );

        FontRenderer font = mcSupplier.get().fontRenderer;
        final int largestTextWidth = buttonInfo.stream().map(Triple::getMiddle).mapToInt(font::getStringWidth).max().orElse(0);
        this.width = spacing * 2 + Math.max(font.getStringWidth(this.fileName), BUTTON_SIZE + largestTextWidth + spacing);
        this.height = spacing * 3 + font.FONT_HEIGHT + BUTTON_SIZE * buttonInfo.size();

        // Offset the widget if it goes out of the screen
        if (x + width > parentWidth.getAsInt()) {
            this.x = x - width;
        } else {
            this.x = x;
        }
        if (y + height > parentHeight.getAsInt()) {
            this.y = y - height;
        } else {
            this.y = y;
        }

        for (int i = 0; i < buttonInfo.size(); i++) {
            Triple<Integer, String, ButtonAction> pair = buttonInfo.get(i);
            buttons.add(new ScreenButton(this.x + spacing, this.y + spacing * 2 + font.FONT_HEIGHT + BUTTON_SIZE * i, BUTTON_SIZE * pair.getLeft(), BUTTON_SIZE, pair.getMiddle(), pair.getRight()));
        }

        shouldRender = true;
    }

    void hide() {
        buttons.clear();
        shouldRender = false;
        childScreen = null;
        x = y = width = height = 0;
        fileName = "";
    }

    boolean renders() {
        return shouldRender;
    }

    public void render(int mouseX, int mouseY, float delta) {
        if (shouldRender) {
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0, 1);
            if (childScreen == null) {
                final int spacing = 2;
                drawRect(x, y, x + width, y + height, 0xFF424242);
                mcSupplier.get().fontRenderer.drawStringWithShadow(fileName, x + spacing, y + spacing, 0xFFFFFFFF);
                for (GuiButton widget : buttons) {
                    widget.drawButton(mcSupplier.get(), mouseX, mouseY, delta);
                    mcSupplier.get().fontRenderer.drawStringWithShadow(widget.displayString, widget.x + widget.width + spacing, widget.y + (widget.height - 9) / 2.f + spacing, 0xFFFFFFFF);
                }
            } else {
                childScreen.drawScreen(mouseX, mouseY, delta);
            }
            GlStateManager.popMatrix();
        }
    }

    public void handleMouseInput() throws IOException {
        if (childScreen != null) {
            childScreen.handleMouseInput();
            return;
        }

        Minecraft mc = mcSupplier.get();
        int mouseX = Mouse.getEventX() * parentWidth.getAsInt() / mc.displayWidth;
        int mouseY = parentHeight.getAsInt() - Mouse.getEventY() * parentHeight.getAsInt() / mc.displayHeight - 1;
        int button = Mouse.getEventButton();

        if (Mouse.getEventButtonState()) {
            this.mouseClicked(mouseX, mouseY, button);
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (mouseX < x || mouseY < y || mouseX > x + width || mouseY > y + height) {
            hide();
        }
        if (button == 0) {
            for (ScreenshotViewerButton btn : this.buttons) {
                if (btn.mousePressed(mcSupplier.get(), mouseX, mouseY)) {
                    btn.playPressSound(mcSupplier.get().getSoundHandler());
                    btn.onPress(btn);
                    break;
                }
            }
        }
    }

    public void handleKeyboardInput() throws IOException {
        if (childScreen instanceof RenameScreen) {
            childScreen.handleKeyboardInput();
            return;
        }

        char c = Keyboard.getEventCharacter();

        if (Keyboard.getEventKey() == 0 && c >= ' ' || Keyboard.getEventKeyState()) {
            this.keyTyped(Keyboard.getEventKey());
        }

        this.mcSupplier.get().dispatchKeypresses();
    }

    public void keyTyped(int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            hide();
        }
    }

    private static final class ScreenButton extends ScreenshotViewerImageButton {
        private static final ResourceLocation TEXTURE = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/screenshot_viewer_icons.png");
        private final int imgU, imgV;

        public ScreenButton(int x, int y, int imgU, int imgV, String title, ButtonAction pressAction) {
            super(x, y, BUTTON_SIZE, BUTTON_SIZE, 0, 0, 0, GuiButton.BUTTON_TEXTURES, 128, 128, title, pressAction);
            this.imgU = imgU;
            this.imgV = imgV;
        }

        @Override
        public void drawButton(@Nonnull Minecraft mc, int mouseX, int mouseY, float delta) {
            if(!visible) {
                return;
            }
            this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
            mc.getTextureManager().bindTexture(TEXTURE);
            GlStateManager.disableDepth();
            drawModalRectWithCustomSizedTexture(this.x, this.y, hovered ? BUTTON_SIZE : BUTTON_SIZE * 2, 0, this.width, this.height, 128, 128);
            drawModalRectWithCustomSizedTexture(this.x, this.y, this.imgU, this.imgV, this.width, this.height, 128, 128);
            GlStateManager.enableDepth();
        }
    }

    private static final class ConfirmationScreen extends GuiYesNo {
        public ConfirmationScreen(Consumer<Boolean> callback, String fileName) {
            super((result, id) -> callback.accept(result), new TextComponentTranslation("screen." + ScreenshotViewer.MODID + ".delete_prompt", fileName).getFormattedText(), ScreenshotViewer.translated("screen", "delete_prompt.message"), -1);
        }

        @Override
        public void drawWorldBackground(int tint) {
            this.drawGradientRect(0, 0, this.width, this.height, -1072689136, -804253680);
        }
    }

    private static final class RenameScreen extends GuiScreen {
        private final String previousName;
        private final Consumer<String> newNameConsumer;
        private final Runnable closeAction;
        private GuiTextField textField;
        private ScreenshotViewerButton doneBtn;

        private RenameScreen(String previousName, Consumer<String> newNameConsumer, Runnable closeAction) {
            this.previousName = previousName;
            this.newNameConsumer = newNameConsumer;
            this.closeAction = closeAction;
        }

        @Override
        public void updateScreen() {
            if (textField != null) {
                textField.updateCursorCounter();
            }
        }

        @Override
        public void initGui() {
            super.initGui();
            this.textField = new GuiTextField(-1, this.fontRenderer, (this.width - 150) / 2, (this.height - 20) / 2, 150, 20);
            textField.setMaxStringLength(128);
            textField.setValidator(RenameScreen::checkInvalidCharacters);
            this.doneBtn = new ScreenshotViewerButton(this.width / 2 - 4 - 150, this.height / 2 + 50, 150, 20, ManageScreenshotsScreen.GUI_DONE, btn -> {
                this.newNameConsumer.accept(textField.getText().trim().concat(".png"));
                this.closeAction.run();
            });
            doneBtn.enabled = false;
            textField.setGuiResponder(new GuiPageButtonList.GuiResponder() {
                @Override
                public void setEntryValue(int id, boolean value) {
                }

                @Override
                public void setEntryValue(int id, float value) {
                }

                @Override
                public void setEntryValue(int id, @Nonnull String value) {
                    doneBtn.enabled = !(StringUtils.isBlank(value) || value.trim().equals(previousName) || value.endsWith("."));
                }
            });
            textField.setText(previousName);
            textField.setFocused(true);
            this.addButton(doneBtn);
            this.addButton(new ScreenshotViewerButton(this.width / 2 + 4, this.height / 2 + 50, 150, 20, ManageScreenshotsScreen.GUI_BACK, btn -> closeAction.run()));
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float delta) {
            this.drawGradientRect(0, 0, this.width, this.height, -1072689136, -804253680);
            drawCenteredString(this.fontRenderer, ScreenshotViewer.translated("screen", "rename_screenshot"), this.width / 2, this.height / 2 - 70, 0xFFFFFF);
            super.drawScreen(mouseX, mouseY, delta);
            if(textField != null) {
                this.textField.drawTextBox();
            }
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) {
            if (keyCode == Keyboard.KEY_RETURN && doneBtn != null && doneBtn.enabled) {
                doneBtn.onPress(doneBtn);
                return;
            }
            if(keyCode == Keyboard.KEY_ESCAPE) {
                close();
            }
            if(textField.isFocused()) {
                this.textField.textboxKeyTyped(typedChar, keyCode);
            }
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            this.textField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        @Override
        protected void actionPerformed(@Nonnull GuiButton button) {
            if(button instanceof ButtonAction) {
                ((ButtonAction) button).onPress(button);
            }
        }

        private void close() {
            this.closeAction.run();
        }

        private static boolean checkInvalidCharacters(String s) {
            return s.chars().noneMatch(c -> c == '\\' || c == '/' || c == ':' || c == '*' || c == '?' || c == '"' || c == '<' || c == '>' || c == '|');
        }
    }
}

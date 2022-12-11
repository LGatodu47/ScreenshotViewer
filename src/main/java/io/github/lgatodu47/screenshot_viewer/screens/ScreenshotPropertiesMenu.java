package io.github.lgatodu47.screenshot_viewer.screens;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.Button.IPressable;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.lwjgl.glfw.GLFW;

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

class ScreenshotPropertiesMenu extends FocusableGui implements IRenderable {
    private static final int BUTTON_SIZE = 19;

    private final Supplier<Minecraft> mcSupplier;
    private final IntSupplier parentWidth, parentHeight;
    private final List<Widget> buttons = new ArrayList<>();

    private int x, y, width, height;
    @Nonnull
    private String fileName = "";
    private boolean shouldRender;
    @Nullable
    private Screen childScreen;

    ScreenshotPropertiesMenu(Supplier<Minecraft> mcSupplier, IntSupplier parentWidth, IntSupplier parentHeight) {
        this.mcSupplier = mcSupplier;
        this.parentWidth = parentWidth;
        this.parentHeight = parentHeight;
    }

    void tick() {
        if (childScreen != null) {
            childScreen.tick();
        }
    }

    void show(int x, int y, Runnable remover, File screenshotFile, Consumer<File> fileUpdater) {
        buttons.clear();

        this.fileName = screenshotFile.getName();
        final int spacing = 2;
        BooleanConsumer deleteAction = value -> {
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
        List<Triple<Integer, ITextComponent, IPressable>> buttonInfo = Lists.newArrayList(
                Triple.of(0, ScreenshotViewer.translatable("screen", "button.delete_screenshot"), btn -> {
                    if (ManageScreenshotsScreen.CONFIG.promptWhenDeletingScreenshot.get()) {
                        childScreen = new ConfirmationScreen(deleteAction, screenshotFile.getName());
                        childScreen.init(this.mcSupplier.get(), parentWidth.getAsInt(), parentHeight.getAsInt());
                    } else {
                        deleteAction.accept(true);
                    }
                }),
                Triple.of(1, ScreenshotViewer.translatable("screen", "button.open_file"), btn -> Util.getPlatform().openFile(screenshotFile)),
                Triple.of(3, ScreenshotViewer.translatable("screen", "button.rename_file"), btn -> {
                    childScreen = new RenameScreen(fileName.substring(0, fileName.lastIndexOf('.')), s -> {
                        try {
                            Path moved = Files.move(screenshotFile.toPath(), screenshotFile.toPath().resolveSibling(s));
                            fileUpdater.accept(moved.toFile());
                        } catch (IOException e) {
                            LOGGER.error("Failed to rename 'screenshot' file at '" + screenshotFile.toPath().toAbsolutePath() + "' from '" + screenshotFile.getName() + "' to '" + s + "'", e);
                        }
                    }, this::hide);
                    childScreen.init(this.mcSupplier.get(), parentWidth.getAsInt(), parentHeight.getAsInt());
                }),
                Triple.of(2, ScreenshotViewer.translatable("screen", "button.close_properties_menu"), btn -> hide())
        );

        FontRenderer font = mcSupplier.get().font;
        final int largestTextWidth = buttonInfo.stream().map(Triple::getMiddle).mapToInt(font::width).max().orElse(0);
        this.width = spacing * 2 + Math.max(font.width(this.fileName), BUTTON_SIZE + largestTextWidth + spacing);
        this.height = spacing * 3 + font.lineHeight + BUTTON_SIZE * buttonInfo.size();

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
            Triple<Integer, ITextComponent, IPressable> pair = buttonInfo.get(i);
            buttons.add(new ScreenButton(this.x + spacing, this.y + spacing * 2 + font.lineHeight + BUTTON_SIZE * i, BUTTON_SIZE * pair.getLeft(), BUTTON_SIZE, pair.getMiddle(), pair.getRight()));
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

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (shouldRender) {
            matrices.pushPose();
            matrices.translate(0, 0, 1);
            if (childScreen == null) {
                final int spacing = 2;
                fill(matrices, x, y, x + width, y + height, 0xFF424242);
                mcSupplier.get().font.drawShadow(matrices, fileName, x + spacing, y + spacing, 0xFFFFFFFF);
                for (Widget widget : buttons) {
                    widget.render(matrices, mouseX, mouseY, delta);
                    mcSupplier.get().font.drawShadow(matrices, widget.getMessage(), widget.x + widget.getWidth() + spacing, widget.y + (widget.getHeight() - 9) / 2.f + spacing, 0xFFFFFFFF);
                }
            } else {
                childScreen.render(matrices, mouseX, mouseY, delta);
            }
            matrices.popPose();
        }
    }

    @Override
    public List<? extends IGuiEventListener> children() {
        return buttons;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (childScreen != null) {
            return childScreen.mouseClicked(mouseX, mouseY, button);
        }
        if (mouseX < x || mouseY < y || mouseX > x + width || mouseY > y + height) {
            hide();
            return false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            hide();
            return true;
        }
        if (childScreen instanceof RenameScreen) {
            return childScreen.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (childScreen instanceof RenameScreen) {
            return childScreen.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    private static final class ScreenButton extends ImageButton {
        private static final ResourceLocation TEXTURE = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/screenshot_viewer_icons.png");
        private final int imgU, imgV;

        public ScreenButton(int x, int y, int imgU, int imgV, ITextComponent title, IPressable pressAction) {
            super(x, y, BUTTON_SIZE, BUTTON_SIZE, 0, 0, 0, ScreenButton.WIDGETS_LOCATION, 128, 128, pressAction, title);
            this.imgU = imgU;
            this.imgV = imgV;
        }

        @Override
        public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            Minecraft.getInstance().getTextureManager().bind(TEXTURE);
            RenderSystem.enableDepthTest();
            ImageButton.blit(matrices, this.x, this.y, isHovered() ? BUTTON_SIZE : BUTTON_SIZE * 2, 0, this.width, this.height, 128, 128);
            ImageButton.blit(matrices, this.x, this.y, this.imgU, this.imgV, this.width, this.height, 128, 128);
        }
    }

    private static final class ConfirmationScreen extends ConfirmScreen {
        public ConfirmationScreen(BooleanConsumer callback, String fileName) {
            super(callback, new TranslationTextComponent("screen." + ScreenshotViewer.MODID + ".delete_prompt", fileName), ScreenshotViewer.translatable("screen", "delete_prompt.message"));
        }

        @Override
        public void renderBackground(MatrixStack matrices, int vOffset) {
            this.fillGradient(matrices, 0, 0, this.width, this.height, -1072689136, -804253680);
        }
    }

    private static final class RenameScreen extends Screen {
        private final String previousName;
        private final Consumer<String> newNameConsumer;
        private final Runnable closeAction;
        private TextFieldWidget textField;
        private Button doneBtn;

        private RenameScreen(String previousName, Consumer<String> newNameConsumer, Runnable closeAction) {
            super(ScreenshotViewer.translatable("screen", "rename_screenshot"));
            this.previousName = previousName;
            this.newNameConsumer = newNameConsumer;
            this.closeAction = closeAction;
        }

        @Override
        public void tick() {
            if (textField != null) {
                textField.tick();
            }
        }

        @Override
        protected void init() {
            super.init();
            this.textField = new TextFieldWidget(this.font, (this.width - 150) / 2, (this.height - 20) / 2, 150, 20, ScreenshotViewer.translatable("screen", "field.screenshot_name"));
            textField.setMaxLength(128);
            textField.setFilter(RenameScreen::checkInvalidCharacters);
            this.doneBtn = new Button(this.width / 2 - 4 - 150, this.height / 2 + 50, 150, 20, DialogTexts.GUI_DONE, btn -> {
                this.newNameConsumer.accept(textField.getValue().trim().concat(".png"));
                this.closeAction.run();
            });
            doneBtn.active = false;
            textField.setResponder(s -> doneBtn.active = !(StringUtils.isBlank(s) || s.trim().equals(previousName) || s.endsWith(".")));
            textField.setValue(previousName);
            this.addButton(textField);
            this.addButton(doneBtn);
            this.addButton(new Button(this.width / 2 + 4, this.height / 2 + 50, 150, 20, DialogTexts.GUI_BACK, btn -> closeAction.run()));
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            this.fillGradient(matrices, 0, 0, this.width, this.height, -1072689136, -804253680);
            drawCenteredString(matrices, this.font, this.title, this.width / 2, this.height / 2 - 70, 0xFFFFFF);
            super.render(matrices, mouseX, mouseY, delta);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ENTER && doneBtn != null && doneBtn.active) {
                doneBtn.onPress();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void onClose() {
            this.closeAction.run();
        }

        private static boolean checkInvalidCharacters(String s) {
            return s.chars().noneMatch(c -> c == '\\' || c == '/' || c == ':' || c == '*' || c == '?' || c == '"' || c == '<' || c == '>' || c == '|');
        }
    }
}

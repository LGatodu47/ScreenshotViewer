package io.github.lgatodu47.screenshot_viewer.screens.manage_screenshots;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import io.github.lgatodu47.screenshot_viewer.screens.ScreenshotViewerTexts;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

final class RenameScreenshotScreen extends Screen {
    private final String previousName;
    private final Consumer<String> newNameConsumer;
    private final Runnable closeAction;
    private Button doneBtn;

    RenameScreenshotScreen(String previousName, Consumer<String> newNameConsumer, Runnable closeAction) {
        super(ScreenshotViewerTexts.RENAME_PROMPT);
        this.previousName = previousName;
        this.newNameConsumer = newNameConsumer;
        this.closeAction = closeAction;
    }

    @Override
    protected void init() {
        super.init();
        EditBox textField = new EditBox(this.font, (this.width - 150) / 2, (this.height - 20) / 2, 150, 20, ScreenshotViewerTexts.SCREENSHOT_NAME_INPUT);
        textField.setMaxLength(128);
        textField.setFilter(RenameScreenshotScreen::checkInvalidCharacters);
        this.doneBtn = Button.builder(CommonComponents.GUI_DONE, btn -> {
            this.newNameConsumer.accept(textField.getValue().trim().concat(".png"));
            this.closeAction.run();
        }).pos(this.width / 2 - 4 - 150, this.height / 2 + 50).build();
        doneBtn.active = false;
        textField.setResponder(s -> doneBtn.active = !(s.isBlank() || s.trim().equals(previousName) || s.endsWith(".")));
        textField.setValue(previousName);
        this.addRenderableWidget(textField);
        this.addRenderableWidget(doneBtn);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, btn -> closeAction.run()).pos(this.width / 2 + 4, this.height / 2 + 50).build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        context.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 70, 0xFFFFFF);
        ScreenshotViewerUtils.forEachDrawable(this, drawable -> drawable.render(context, mouseX, mouseY, delta));
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

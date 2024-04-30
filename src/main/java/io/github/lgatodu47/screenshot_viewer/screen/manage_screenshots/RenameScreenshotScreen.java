package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerTexts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.ScreenTexts;

import java.util.function.Consumer;

final class RenameScreenshotScreen extends Screen {
    private final String previousName;
    private final Consumer<String> newNameConsumer;
    private final Runnable closeAction;
    private ButtonWidget doneBtn;

    RenameScreenshotScreen(String previousName, Consumer<String> newNameConsumer, Runnable closeAction) {
        super(ScreenshotViewerTexts.RENAME_PROMPT);
        this.previousName = previousName;
        this.newNameConsumer = newNameConsumer;
        this.closeAction = closeAction;
    }

    @Override
    protected void init() {
        super.init();
        TextFieldWidget textField = new TextFieldWidget(this.textRenderer, (this.width - 150) / 2, (this.height - 20) / 2, 150, 20, ScreenshotViewerTexts.SCREENSHOT_NAME_INPUT);
        textField.setMaxLength(128);
        textField.setTextPredicate(RenameScreenshotScreen::checkInvalidCharacters);
        this.doneBtn = ButtonWidget.builder(ScreenTexts.DONE, btn -> {
            this.newNameConsumer.accept(textField.getText().trim().concat(".png"));
            this.closeAction.run();
        }).position(this.width / 2 - 4 - 150, this.height / 2 + 50).build();
        doneBtn.active = false;
        textField.setChangedListener(s -> doneBtn.active = !(s.isBlank() || s.trim().equals(previousName) || s.endsWith(".")));
        textField.setText(previousName);
        this.addDrawableChild(textField);
        this.addDrawableChild(doneBtn);
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.BACK, btn -> closeAction.run()).position(this.width / 2 + 4, this.height / 2 + 50).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 70, 0xFFFFFF);
        ScreenshotViewerUtils.forEachDrawable(this, drawable -> drawable.render(context, mouseX, mouseY, delta));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputUtil.GLFW_KEY_ENTER && doneBtn != null && doneBtn.active) {
            doneBtn.onPress();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        this.closeAction.run();
    }

    private static boolean checkInvalidCharacters(String s) {
        return s.chars().noneMatch(c -> c == '\\' || c == '/' || c == ':' || c == '*' || c == '?' || c == '"' || c == '<' || c == '>' || c == '|');
    }
}

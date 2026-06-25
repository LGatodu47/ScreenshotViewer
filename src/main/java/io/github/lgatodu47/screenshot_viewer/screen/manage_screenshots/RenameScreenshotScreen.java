package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.lgatodu47.catconfigmc.OldEditBox;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewerUtils;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerTexts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;

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
        OldEditBox textField = new OldEditBox(this.font, (this.width - 150) / 2, (this.height - 20) / 2, 150, 20, ScreenshotViewerTexts.SCREENSHOT_NAME_INPUT);
        textField.setMaxLength(128);
        textField.setTextPredicate(RenameScreenshotScreen::checkInvalidCharacters);
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
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        context.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 70, 0xFFFFFFFF);
        ScreenshotViewerUtils.forEachDrawable(this, drawable -> drawable.extractRenderState(context, mouseX, mouseY, delta));
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == InputConstants.KEY_RETURN && doneBtn != null && doneBtn.active) {
            doneBtn.onPress(input);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void onClose() {
        this.closeAction.run();
    }

    private static boolean checkInvalidCharacters(String s) {
        return s.chars().noneMatch(c -> c == '\\' || c == '/' || c == ':' || c == '*' || c == '?' || c == '"' || c == '<' || c == '>' || c == '|');
    }
}

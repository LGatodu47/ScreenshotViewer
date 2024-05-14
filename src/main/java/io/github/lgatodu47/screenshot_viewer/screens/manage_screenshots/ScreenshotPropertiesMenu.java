package io.github.lgatodu47.screenshot_viewer.screens.manage_screenshots;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.screens.IconButtonWidget;
import io.github.lgatodu47.screenshot_viewer.screens.ScreenshotViewerTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

class ScreenshotPropertiesMenu extends AbstractContainerEventHandler implements Renderable {
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/screenshot_properties_background.png");
    static final ResourceLocation OPEN_ICON = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/sprites/widget/icons/open_folder.png");
    static final ResourceLocation COPY_ICON = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/sprites/widget/icons/copy.png");
    static final ResourceLocation DELETE_ICON = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/sprites/widget/icons/delete.png");
    static final ResourceLocation RENAME_ICON = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/sprites/widget/icons/rename.png");
    private static final ResourceLocation CLOSE_ICON = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/sprites/widget/icons/close.png");
    private static final int BUTTON_SIZE = 19;

    private final Supplier<Minecraft> mcSupplier;
    private final List<Button> buttons = new ArrayList<>();

    private int x, y, width, height;
    private ScreenshotImageHolder targetScreenshot;
    private boolean shouldRender;

    ScreenshotPropertiesMenu(Supplier<Minecraft> mcSupplier) {
        this.mcSupplier = mcSupplier;
        addButton(OPEN_ICON, ScreenshotViewerTexts.OPEN_FILE, ScreenshotImageHolder::openFile);
        addButton(COPY_ICON, ScreenshotViewerTexts.COPY, ScreenshotImageHolder::copyScreenshot);
        addButton(DELETE_ICON, ScreenshotViewerTexts.DELETE, ScreenshotImageHolder::requestFileDeletion);
        addButton(RENAME_ICON, ScreenshotViewerTexts.RENAME_FILE, ScreenshotImageHolder::renameFile);
        addButton(CLOSE_ICON, ScreenshotViewerTexts.CLOSE_PROPERTIES, null);
    }

    private void addButton(ResourceLocation texture, Component description, @Nullable Consumer<ScreenshotImageHolder> action) {
        this.buttons.add(new Button(texture, description, btn -> {
            if(action != null && targetScreenshot != null) {
                action.accept(targetScreenshot);
            }
            hide();
        }));
    }

    void show(int x, int y, int parentWidth, int parentHeight, ScreenshotImageHolder targetScreenshot) {
        this.targetScreenshot = targetScreenshot;
        final int spacing = 2;

        Font font = mcSupplier.get().font;
        final int largestTextWidth = buttons.stream().map(AbstractWidget::getMessage).mapToInt(font::width).max().orElse(0);
        this.width = spacing * 2 + Math.max(font.width(targetScreenshot.getScreenshotFile().getName()), BUTTON_SIZE + largestTextWidth + spacing * 2);
        this.height = spacing * 3 + font.lineHeight + BUTTON_SIZE * buttons.size();

        // Offset the widget if it goes out of the screen
        if (x + width > parentWidth) {
            this.x = x - width;
        } else {
            this.x = x;
        }
        if (y + height > parentHeight) {
            this.y = y - height;
        } else {
            this.y = y;
        }

        for (int i = 0; i < this.buttons.size(); i++) {
            this.buttons.get(i).setDimensionsAndPosition(width - 2 * spacing, this.x + spacing, this.y + spacing * 2 + font.lineHeight + BUTTON_SIZE * i);
        }

        shouldRender = true;
    }

    void hide() {
        shouldRender = false;
        x = y = width = height = 0;
    }

    boolean renders() {
        return shouldRender;
    }

    @Override
    public void render(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (shouldRender) {
            PoseStack matrices = context.pose();
            matrices.pushPose();
            matrices.translate(0, 0, 1);
            final int spacing = 2;

            //corners
            context.blit(BACKGROUND_TEXTURE, x, y, 0, 0,
                    2, 2, 8, 8);
            context.blit(BACKGROUND_TEXTURE, x+width-2, y, 6, 0,
                    2, 2, 8, 8);
            context.blit(BACKGROUND_TEXTURE, x, y+height-2, 0, 6,
                    2, 2, 8, 8);
            context.blit(BACKGROUND_TEXTURE, x+width-2, y+height-2, 6, 6,
                    2, 2, 8, 8);
            //sides
            context.blit(BACKGROUND_TEXTURE, x+2, y, (float) (width * 3) /2, 0,
                    width-4, 2, width*4, 8);
            context.blit(BACKGROUND_TEXTURE, x, y+2, 0, (float) (height * 3) /2,
                    2, height-4, 8, height*4);
            context.blit(BACKGROUND_TEXTURE, x+2, y+height-2, (float) (width * 3) /2, 6,
                    width-4, 2, width*4, 8);
            context.blit(BACKGROUND_TEXTURE, x+width-2, y+2, 6, (float) (height * 3) /2,
                    2, height-4, 8, height*4);
            //center
            context.blit(BACKGROUND_TEXTURE, x+2, y+2,
                    (float) (width * 3) /2, (float) (height * 3) /2,
                    width-4, height-4,
                    width*4, height*4);

            context.drawString(mcSupplier.get().font, targetScreenshot.getScreenshotFile().getName(), x + spacing, y + spacing, 0xFFFFFFFF);
            for (AbstractWidget widget : buttons) {
                widget.render(context, mouseX, mouseY, delta);
                matrices.pushPose();
                matrices.translate(0, 0, 1);
                context.drawString(mcSupplier.get().font, widget.getMessage(), widget.getX() + BUTTON_SIZE + spacing, (int) (widget.getY() + (widget.getHeight() - 9) / 2.f + spacing), 0xFFFFFFFF);
                matrices.popPose();
            }
            matrices.popPose();
        }
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return List.copyOf(this.buttons);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < x || mouseY < y || mouseX > x + width || mouseY > y + height) {
            hide();
            return false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            hide();
            return true;
        }
        return false;
    }

    private static final class Button extends IconButtonWidget {
        private static final ResourceLocation BUTTON_ENABLED = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/sprites/widget/properties_button_enabled.png");
        private static final ResourceLocation BUTTON_DISABLED = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/sprites/widget/properties_button.png");
        private static final ResourceLocation BUTTON_HOVERED = new ResourceLocation(ScreenshotViewer.MODID, "textures/gui/sprites/widget/properties_button_hovered.png");

        private boolean renderWide = ManageScreenshotsScreen.CONFIG.renderWidePropertiesButton.get();

        public Button(ResourceLocation texture, Component title, OnPress pressAction) {
            super(0, 0, BUTTON_SIZE, BUTTON_SIZE, title, texture, pressAction);
        }

        public void setDimensionsAndPosition(int width, int x, int y) {
            this.renderWide = ManageScreenshotsScreen.CONFIG.renderWidePropertiesButton.get();
            // provided width is for wide while current width is for squared.
            if(renderWide) {
                setWidth(width);
            }
            setPosition(x, y);
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
            context.setColor(1, 1, 1, this.alpha);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            ResourceLocation backgroundTexture = getBackgroundTexture(this.active, isHoveredOrFocused());
            if (renderWide) {
                context.blit(backgroundTexture, getX(), getY(), 0, 0, 1, getHeight(), BUTTON_SIZE, BUTTON_SIZE);
                context.blit(backgroundTexture, getX() + 1, getY(), getWidth() - 2, getHeight(), 1, 0, BUTTON_SIZE - 2, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);
                context.blit(backgroundTexture, getX() + getWidth() - 1, getY(), 18, 0, 1, getHeight(), BUTTON_SIZE, BUTTON_SIZE);
            } else {
                context.blit(backgroundTexture, getX(), getY(), 0, 0, getWidth(), getHeight(), BUTTON_SIZE, BUTTON_SIZE);
            }
            ResourceLocation icon = getIconTexture();
            if(icon != null) {
                context.blit(icon, getX(), getY(), 0, 0, BUTTON_SIZE, getHeight(), BUTTON_SIZE, BUTTON_SIZE);
            }
            context.setColor(1, 1, 1, 1);
        }

        @Override
        public boolean isHoveredOrFocused() {
            return isHovered;
        }

        public ResourceLocation getBackgroundTexture(boolean enabled, boolean focused) {
            return !enabled ? BUTTON_DISABLED : focused ? BUTTON_HOVERED : BUTTON_ENABLED;
        }
    }
}

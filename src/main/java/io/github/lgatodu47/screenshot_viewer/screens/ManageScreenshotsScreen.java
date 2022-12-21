package io.github.lgatodu47.screenshot_viewer.screens;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfig;
import io.github.lgatodu47.screenshot_viewer.config.ScreenshotViewerConfigListener;
import io.github.lgatodu47.screenshot_viewer.screens.widgets.ButtonAction;
import io.github.lgatodu47.screenshot_viewer.screens.widgets.ButtonTooltip;
import io.github.lgatodu47.screenshot_viewer.screens.widgets.ScreenshotViewerButton;
import io.github.lgatodu47.screenshot_viewer.screens.widgets.ScreenshotViewerImageButton;
import io.github.lgatodu47.screenshot_viewer.util.DynamicButtonTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.PrivilegedExceptionAction;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class ManageScreenshotsScreen extends GuiScreen implements ScreenshotViewerConfigListener {
    // Package-private config instance accessible in all the package classes
    static final ScreenshotViewerConfig CONFIG = ScreenshotViewer.getInstance().getConfig();
    static final Logger LOGGER = LogManager.getLogger();

    static final String GUI_DONE = I18n.format("gui.done");
    static final String GUI_BACK = I18n.format("gui.back");

    private static final ResourceLocation CONFIG_BUTTON_TEXTURE = new DynamicButtonTexture("config_button");
    private static final ResourceLocation REFRESH_BUTTON_TEXTURE = new DynamicButtonTexture("refresh_button");
    private static final ResourceLocation ASCENDING_ORDER_BUTTON_TEXTURE = new DynamicButtonTexture("ascending_order_button");
    private static final ResourceLocation DESCENDING_ORDER_BUTTON_TEXTURE = new DynamicButtonTexture("descending_order_button");
    private static final ResourceLocation OPEN_FOLDER_BUTTON_TEXTURE = new DynamicButtonTexture("open_folder_button");

    private final GuiScreen parent;
    private final EnlargedScreenshotScreen enlargedScreenshot;
    private final ScreenshotPropertiesMenu screenshotProperties;
    private ScreenshotList list;

    public ManageScreenshotsScreen(GuiScreen parent) {
        this.parent = parent;
        this.enlargedScreenshot = new EnlargedScreenshotScreen();
        this.screenshotProperties = new ScreenshotPropertiesMenu(this::client, () -> width, () -> height);
        ScreenshotViewer.getInstance().registerConfigListener(this);
    }

    Minecraft client() {
        return mc;
    }

    /// Basic Screen implementations ///

    @Override
    public void updateScreen() {
        if(screenshotProperties != null) {
            screenshotProperties.tick();
        }
    }

    @Override
    public void initGui() {
        if(mc == null) {
            return;
        }

        final int spacing = 8;
        final int btnHeight = 20;

        this.enlargedScreenshot.setWorldAndResolution(mc, width, height);

        //Main content
        int contentWidth = width - 24;
        int contentHeight = height - spacing * 5 - btnHeight;
        // We avoid creating the list every time we refresh the screen, so we don't have to load the screenshots again (which takes time)
        if(list == null) {
            list = new ScreenshotList(this, 12, spacing * 3, width - 24, height - spacing * 5 - btnHeight);
            list.init();
        }
        else {
            list.updateSize(contentWidth, contentHeight);
            list.updateChildren();
        }

        // Button stuff
        final int btnY = height - spacing - btnHeight;
        final int btnSize = 20;
        final int bigBtnWidth = 200;

        // Config Button
        Optional<UnaryOperator<GuiScreen>> configScreenFactory = ScreenshotViewer.getInstance().getConfigScreenFactory();
        GuiButton configButton = new ExtendedTexturedButtonWidget(2, 2, btnSize, btnSize, 0, 0, btnSize, CONFIG_BUTTON_TEXTURE, 32, 64, button -> {
            configScreenFactory.ifPresent(f -> mc.displayGuiScreen(f.apply(this)));
        }, (button, x, y) -> {
            drawHoveringText(fontRenderer.trimStringToWidth(ScreenshotViewer.translated("screen", configScreenFactory.isPresent() ? "button.config" : "no_config"), Math.max(width / 2 - 43, 170)), x, y + btnSize);
        }, ScreenshotViewer.translated("screen", configScreenFactory.isPresent() ? "button.config" : "no_config"));
        configButton.enabled = configScreenFactory.isPresent();
        addButton(configButton);
        // Order Button
        addButton(new ExtendedTexturedButtonWidget(spacing, btnY, btnSize, btnSize, 0, 0, btnSize, null, 32, 64, button -> {
            if(list != null) {
                list.invertOrder();
            }
        }, (button, x, y) -> {
            if(list != null) {
                drawHoveringText(fontRenderer.trimStringToWidth(ScreenshotViewer.translated("screen", list.isInvertedOrder() ? "button.order.descending" : "button.order.ascending"), Math.max(width / 2 - 43, 170)), x, y);
            }
        }, ScreenshotViewer.translated("screen", "button.order")) {
            @Override
            public @Nullable ResourceLocation getTexture() {
                return list == null ? null : list.isInvertedOrder() ? DESCENDING_ORDER_BUTTON_TEXTURE : ASCENDING_ORDER_BUTTON_TEXTURE;
            }
        });
        // Screenshot Folder Button
        addButton(new ExtendedTexturedButtonWidget(spacing * 2 + btnSize, btnY, btnSize, btnSize, 0, 0, btnSize, OPEN_FOLDER_BUTTON_TEXTURE, 32, 64, btn -> {
            openFile(new File(mc.mcDataDir, "screenshots"));
        }, (button, x, y) -> {
            drawHoveringText(fontRenderer.trimStringToWidth(ScreenshotViewer.translated("screen", "button.screenshot_folder"), Math.max(width / 2 - 43, 170)), x, y);
        }, ScreenshotViewer.translated("screen", "button.screenshot_folder")));
        // Done Button
        addButton(new ExtendedButtonWidget((width - bigBtnWidth) / 2, btnY, bigBtnWidth, btnHeight, GUI_DONE, button -> close()));
        // Refresh Button
        addButton(new ExtendedTexturedButtonWidget(width - spacing - btnSize, btnY, btnSize, btnSize, 0, 0, btnSize, REFRESH_BUTTON_TEXTURE, 32, 64, button -> {
            list.init();
        }, (btn, x, y) -> {
            drawHoveringText(fontRenderer.trimStringToWidth(ScreenshotViewer.translated("screen", "button.refresh"), Math.max(width / 2 - 43, 170)), x, y);
        }, ScreenshotViewer.translated("screen", "button.refresh")));
    }

    @Override
    protected void actionPerformed(@Nonnull GuiButton button) {
        if(button instanceof ButtonAction) {
            ((ButtonAction) button).onPress(button);
        }
    }

    @Override
    public void onResize(@Nonnull Minecraft client, int width, int height) {
        super.onResize(client, width, height);
        // Adapts the size of the enlarged screenshot when resized
        this.enlargedScreenshot.onResize(client, width, height);
        // Hides the screenshot properties menu
        this.screenshotProperties.hide();
    }

    static void openFile(File file) {
        try {
            URL fileUrl = file.toURI().toURL();
            @SuppressWarnings("removal") // I'm using JDK 17
            Process process = java.security.AccessController.doPrivileged((PrivilegedExceptionAction<? extends Process>) () -> Runtime.getRuntime().exec(getCommand(fileUrl)));
            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();
        }
        catch (Exception e) {
            LOGGER.error("Couldn't open file at '{}'", file.toPath().toAbsolutePath(), e);
        }
    }

    private static String[] getCommand(URL url) {
        switch (Util.getOSType()) {
            case WINDOWS:
                return new String[] { "rundll32", "url.dll,FileProtocolHandler", url.toString() };
            case OSX:
                return new String[] { "open", url.toString() };
            default:
                return new String[] { "xdg-open", url.toString().replace("file:", "file://") };
        }
    }

    private float screenshotScaleAnimation;

    @Override
    public void drawScreen(int mouseX, int mouseY, float delta) {
        drawDefaultBackground();
        if(list != null) {
            list.render(mouseX, mouseY, !(enlargedScreenshot.renders() || screenshotProperties.renders()));
        }
        drawCenteredString(fontRenderer, ScreenshotViewer.translated("screen", "manage_screenshots"),width / 2, 8, 0xFFFFFF);
        String text = ScreenshotViewer.translated("screen", "screenshot_manager.zoom");
        drawString(fontRenderer, text, width - fontRenderer.getStringWidth(text) - 8, 8, isCtrlKeyDown() ? 0x18DE39 : 0xF0CA22);
        super.drawScreen(mouseX, mouseY, delta);
        screenshotProperties.render(mouseX, mouseY, delta);
        if(enlargedScreenshot.renders()) {
            float animationTime = 1;

            if(CONFIG.enableScreenshotEnlargementAnimation.getAsBoolean()) {
                if(screenshotScaleAnimation < 1f) {
                    animationTime = (float) (1 - Math.pow(1 - (screenshotScaleAnimation += 0.03), 3));
                }
            }

            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0, 1);
            enlargedScreenshot.drawDefaultBackground();
            GlStateManager.translate((enlargedScreenshot.width / 2f) * (1 - animationTime), (enlargedScreenshot.height / 2f) * (1 - animationTime), 0);
            GlStateManager.scale(animationTime, animationTime, animationTime);
            enlargedScreenshot.drawScreen(mouseX, mouseY, delta);
            GlStateManager.popMatrix();
        } else {
            if(screenshotScaleAnimation > 0) {
                screenshotScaleAnimation = 0;
            }

            if(!screenshotProperties.renders()) {
                for (GuiButton btn : this.buttonList) {
                    if (btn instanceof CustomHoverState) {
                        ((CustomHoverState) btn).updateHoveredState(mouseX, mouseY);
                    }
                }

                this.handleHoveredChildren(mouseX, mouseY);
            }
        }
    }

    private void handleHoveredChildren(double mouseX, double mouseY) {
        this.buttonList.stream()
                .filter(GuiButton::isMouseOver)
                .filter(ButtonTooltip.class::isInstance)
                .forEach(btn -> ((ButtonTooltip) btn).renderTooltip(btn, (int) mouseX, (int) mouseY));
    }

    /// Methods shared between the classes of the package ///

    void enlargeScreenshot(ScreenshotImageHolder showing) {
        this.enlargedScreenshot.show(showing, list);
    }

    void showScreenshotProperties(double mouseX, double mouseY, ScreenshotWidget widget) {
        this.screenshotProperties.show((int) mouseX, (int) mouseY, () -> list.removeEntry(widget), widget.getScreenshotFile(), widget::updateScreenshotFile);
    }

    /// Input handling methods below ///

    @Override
    public void handleKeyboardInput() throws IOException {
        if(screenshotProperties.renders()) {
            screenshotProperties.handleKeyboardInput();
            return;
        }
        if(enlargedScreenshot.renders()) {
            enlargedScreenshot.handleKeyboardInput();
            return;
        }
        super.handleKeyboardInput();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if(keyCode == Keyboard.KEY_ESCAPE) {
            close();
        }
    }


    @Override
    public void handleMouseInput() throws IOException {
        if(screenshotProperties.renders()) {
            screenshotProperties.handleMouseInput();
            return;
        }
        if(enlargedScreenshot.renders()) {
            enlargedScreenshot.handleMouseInput();
            return;
        }
        if(list != null) {
            if(isCtrlKeyDown()) {
                list.updateScreenshotsPerRow(Mouse.getEventDWheel());
            } else {
                list.handleMouseInput();
            }
        }
        super.handleMouseInput();
    }

    /// Other Methods ///

    private void close() {
        if(mc != null) {
            this.mc.displayGuiScreen(parent);
        }
        ScreenshotViewer.getInstance().unregisterConfigListener(this);
    }

    @Override
    public void onGuiClosed() {
        list.close();
    }

    @Override
    public void onConfigReloaded() {
        list.configUpdated();
    }

    private static final class ExtendedButtonWidget extends ScreenshotViewerButton implements CustomHoverState {
        ExtendedButtonWidget(int x, int y, int width, int height, String message, ButtonAction action) {
            super(x, y, width, height, message, action);
        }

        @Override
        public void drawButton(@Nonnull Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                FontRenderer fontRenderer = mc.fontRenderer;
                mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                int hoverState = this.getHoverState(this.hovered);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                this.drawTexturedModalRect(this.x, this.y, 0, 46 + hoverState * 20, this.width / 2, this.height);
                this.drawTexturedModalRect(this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + hoverState * 20, this.width / 2, this.height);
                this.mouseDragged(mc, mouseX, mouseY);
                int j = 14737632;

                if (packedFGColour != 0) {
                    j = packedFGColour;
                } else if (!this.enabled) {
                    j = 10526880;
                } else if (this.hovered) {
                    j = 16777120;
                }

                this.drawCenteredString(fontRenderer, this.displayString, this.x + this.width / 2, this.y + (this.height - 8) / 2, j);
            }
        }

        @Override
        public void updateHoveredState(int mouseX, int mouseY) {
            this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        }
    }

    private static class ExtendedTexturedButtonWidget extends ScreenshotViewerImageButton implements CustomHoverState {
        @Nullable
        private final ResourceLocation texture;

        ExtendedTexturedButtonWidget(int x, int y, int width, int height,
                                     int u, int v, int hoveredVOffset, @Nullable ResourceLocation texture,
                                     int textureWidth, int textureHeight, ButtonAction action, ButtonTooltip tooltip,
                                     String text) {
            super(x, y, width, height, u, v, hoveredVOffset, GuiButton.BUTTON_TEXTURES, textureWidth, textureHeight, text, action, tooltip);
            this.texture = texture;
            this.displayString = text;
        }

        @Nullable
        public ResourceLocation getTexture() {
            return this.texture;
        }

        @Override
        public void drawButton(@Nonnull Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                ResourceLocation texture = getTexture();
                if(texture == null) {
                    drawRect(x, y, x + width, y + height, 0xFFFFFF);
                } else {
                    mc.getTextureManager().bindTexture(texture);
                    GlStateManager.color(1, 1, 1, 1);
                    GlStateManager.disableDepth();
                    int vOffset = this.v;
                    if (!enabled) {
                        vOffset += this.hoveredVOffset * 2;
                    } else if (hovered) {
                        vOffset += this.hoveredVOffset;
                    }

                    drawModalRectWithCustomSizedTexture(this.x, this.y, this.u, vOffset, this.width, this.height, this.textureWidth, this.textureHeight);
                    GlStateManager.enableDepth();
                }
            }
        }

        @Override
        public void updateHoveredState(int mouseX, int mouseY) {
            this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        }
    }

    private interface CustomHoverState {
        void updateHoveredState(int mouseX, int mouseY);
    }
}

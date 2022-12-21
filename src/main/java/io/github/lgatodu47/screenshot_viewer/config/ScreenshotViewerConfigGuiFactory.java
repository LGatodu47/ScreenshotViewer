package io.github.lgatodu47.screenshot_viewer.config;

import io.github.lgatodu47.screenshot_viewer.ScreenshotViewer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ScreenshotViewerConfigGuiFactory implements IModGuiFactory {
    @Override
    public void initialize(Minecraft mc) {
    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new GuiConfig(parentScreen, gatherConfigElements(), ScreenshotViewer.MODID, ScreenshotViewer.MODID.concat("_config"), false, false, ScreenshotViewer.translated("screen", "config"));
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return Collections.emptySet();
    }

    private static List<IConfigElement> gatherConfigElements() {
        try {
            return ScreenshotViewer.getInstance().getConfig()
                    .getConfiguration().getCategory(ScreenshotViewerConfig.CATEGORY)
                    .getOrderedValues()
                    .stream()
                    .map(ConfigElement::new)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

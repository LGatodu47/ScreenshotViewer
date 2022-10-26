package io.github.lgatodu47.screenshot_viewer;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.lgatodu47.screenshot_viewer.screen.ScreenshotViewerConfigScreen;

public class ScreenshotViewerModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ScreenshotViewerConfigScreen::new;
    }
}

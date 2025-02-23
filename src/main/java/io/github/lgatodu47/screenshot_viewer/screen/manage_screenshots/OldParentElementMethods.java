package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;

import java.util.Iterator;

public interface OldParentElementMethods extends ParentElement {
    @Override
    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        Iterator<? extends Element> iter = this.children().iterator();

        Element element;
        do {
            if (!iter.hasNext()) {
                return false;
            }

            element = iter.next();
        } while(!element.mouseClicked(mouseX, mouseY, button));

        setFocused(element);
        if (button == 0) {
            setDragging(true);
        }

        return true;
    }
}

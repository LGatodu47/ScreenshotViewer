package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;

import java.util.Iterator;

public interface OldParentElementMethods extends ParentElement {
    @Override
    default boolean mouseClicked(Click click, boolean doubled) {
        Iterator<? extends Element> iter = this.children().iterator();

        Element element;
        do {
            if (!iter.hasNext()) {
                return false;
            }

            element = iter.next();
        } while(!element.mouseClicked(click, doubled));

        setFocused(element);
        if (click.button() == 0) {
            setDragging(true);
        }

        return true;
    }
}

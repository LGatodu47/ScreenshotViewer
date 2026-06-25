package io.github.lgatodu47.screenshot_viewer.screen.manage_screenshots;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.events.ContainerEventHandler;

import java.util.Iterator;

public interface OldParentElementMethods extends ContainerEventHandler {
    @Override
    default boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        Iterator<? extends GuiEventListener> iter = this.children().iterator();

        GuiEventListener element;
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

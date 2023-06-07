package io.github.lgatodu47.screenshot_viewer;

import ca.weblite.objc.Client;
import ca.weblite.objc.Proxy;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;

public class ScreenshotViewerMacOsUtils {
    // Code taken from ScreenshotToClipboard: https://github.com/comp500/ScreenshotToClipboard
    /*
    MIT License
    Copyright (c) 2018 comp500
    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:
    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
    */
    public static void doCopyMacOS(String path) {
        if (!Minecraft.ON_OSX) {
            return;
        }

        Client client = Client.getInstance();
        Proxy url = client.sendProxy("NSURL", "fileURLWithPath:", path);

        Proxy image = client.sendProxy("NSImage", "alloc");
        image.send("initWithContentsOfURL:", url);

        Proxy array = client.sendProxy("NSArray", "array");
        array = array.sendProxy("arrayByAddingObject:", image);

        Proxy pasteboard = client.sendProxy("NSPasteboard", "generalPasteboard");
        pasteboard.send("clearContents");
        boolean wasSuccessful = pasteboard.sendBoolean("writeObjects:", array);
        if (!wasSuccessful) {
            LogUtils.getLogger().error("Failed to write image to pasteboard!");
        }
    }
}

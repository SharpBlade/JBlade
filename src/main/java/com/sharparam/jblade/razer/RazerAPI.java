/*
 * Copyright (c) 2014 by Adam Hellberg and Brandon Scott.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Disclaimer: SwitchBladeSteam is in no way affiliated
 * with Razer and/or any of its employees and/or licensors.
 * Adam Hellberg does not take responsibility for any harm caused, direct
 * or indirect, to any Razer peripherals via the use of SharpBlade.
 *
 * "Razer" is a trademark of Razer USA Ltd.
 */

package com.sharparam.jblade.razer;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;

/**
 * Created on 2014-01-24.
 * @author Sharparam
 */
public interface RazerAPI extends Library {
    static final String DLL_NAME = "RzSwitchbladeSDK2.dll";

    static final RazerAPI INSTANCE = (RazerAPI) Native.loadLibrary(DLL_NAME, RazerAPI.class);

    static final int DYNAMIC_KEYS_PER_ROW = 5;
    static final int DYNAMIC_KEY_ROWS = 2;
    static final int DYNAMIC_KEYS_COUNT = DYNAMIC_KEYS_PER_ROW * DYNAMIC_KEY_ROWS;
    static final int DYNAMIC_KEY_WIDTH = 115;
    static final int DYNAMIC_KEY_HEIGHT = 115;
    static final int DYNAMIC_KEY_IMAGE_DATA_SIZE = DYNAMIC_KEY_WIDTH * DYNAMIC_KEY_HEIGHT * 2;
    static final int TOUCHPAD_WIDTH = 800;
    static final int TOUCHPAD_HEIGHT = 480;
    static final int TOUCHPAD_IMAGE_DATA_SIZE = TOUCHPAD_WIDTH * TOUCHPAD_HEIGHT * 2;
    static final int DISPLAY_COLOR_DEPTH = 16;
    static final int MAX_STRING_LENGTH = 260;
    static final int MAX_SUPPORTED_SURFACES = 2;
    static final int PIXEL_FORMAT_INVALID = 0;
    static final int PIXEL_FORMAT_RGB565 = 1;

    interface DynamicKeyCallbackFunction extends StdCallLibrary.StdCallCallback {
        // TODO: Add invoke method
    }

    interface AppEventCallbackFunction extends StdCallLibrary.StdCallCallback {
        // TODO: Add invoke method
    }

    interface TouchpadGestureCallbackFunction extends StdCallLibrary.StdCallCallback {
        // TODO: Add invoke method
    }

    interface KeyboardCallbackFunction extends StdCallLibrary.StdCallCallback {
        // TODO: Make HRESULT class for return type? Is this feasible in Java?
        int invoke(WinDef.UINT uMsg, WinDef.UINT_PTR wParam, WinDef.INT_PTR lParam);
    }

    int RzSBStart();

    void RzSBStop();

    int RzSBAppEventSetCallback(AppEventCallbackFunction callback);

    int RzSBDynamicKeySetCallback(DynamicKeyCallbackFunction callback);

    int RzSBCaptureKeyboard(boolean enable);

    int RzSBKeyboardCaptureSetCallback(KeyboardCallbackFunction callback);

    int RzSBGestureSetCallback(TouchpadGestureCallbackFunction callback);
}

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

import com.sharparam.jblade.razer.exceptions.RazerInvalidAppEventModeException;
import com.sharparam.jblade.razer.exceptions.RazerInvalidTargetDisplayException;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * Created on 2014-01-24.
 * @author Sharparam
 */
public interface RazerAPI extends Library {
    // TODO: Make HRESULT class for return type? Is this feasible in Java?

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
        int invoke(DynamicKeyType dynamicKeyType, DynamicKeyState dynamicKeyState);
    }

    interface AppEventCallbackFunction extends StdCallLibrary.StdCallCallback {
        int invoke(AppEventType appEventType, WinDef.UINT dwAppMode, WinDef.UINT dwProcessID);
    }

    interface TouchpadGestureCallbackFunction extends StdCallLibrary.StdCallCallback {
        int invoke(GestureType gestureType, WinDef.UINT dwParameters,
                   WinDef.USHORT wXPos, WinDef.USHORT wYPos, WinDef.USHORT wZPos);
    }

    interface KeyboardCallbackFunction extends StdCallLibrary.StdCallCallback {
        int invoke(WinDef.UINT uMsg, WinDef.UINT_PTR wParam, WinDef.INT_PTR lParam);
    }

    enum DynamicKeyState {
        NONE,
        UP,
        DOWN,
        HOLD,
        INVALID
    }

    enum Direction {
        NONE,
        LEFT,
        RIGHT,
        UP,
        DOWN,
        INVALID
    }

    enum DynamicKeyType {
        NONE,
        DK1,
        DK2,
        DK3,
        DK4,
        DK5,
        DK6,
        DK7,
        DK8,
        DK9,
        DK10,
        INVALID;

        public static final int COUNT = 10;
    }

    enum TargetDisplay {
        WIDGET (0x10000),
        DK1    (0x10001),
        DK2    (0x10002),
        DK3    (0x10003),
        DK4    (0x10004),
        DK5    (0x10005),
        DK6    (0x10006),
        DK7    (0x10007),
        DK8    (0x10008),
        DK9    (0x10009),
        DK10   (0x1000A);

        private final int val;

        private TargetDisplay(int val) {
            this.val = val;
        }

        public TargetDisplay getTargetDisplayFromApiValue(int val) throws RazerInvalidTargetDisplayException {
            for (TargetDisplay disp : TargetDisplay.values()) {
                if (disp.getVal() == val)
                    return disp;
            }

            throw new RazerInvalidTargetDisplayException(val);
        }

        public int getVal() {
            return val;
        }
    }

    enum PixelType {
        RGB565
    }

    enum AppEventType {
        NONE,
        ACTIVATED,
        DEACTIVATED,
        CLOSE,
        EXIT,
        INVALID
    }

    enum AppEventMode {
        APPLET (0x02),
        NORMAL (0x04);

        private final int val;

        private AppEventMode(int val) {
            this.val = val;
        }

        public AppEventMode getAppEventModeFromApiValue(int val) throws RazerInvalidAppEventModeException {
            for (AppEventMode mode : AppEventMode.values()) {
                if (mode.getVal() == val)
                    return mode;
            }

            throw new RazerInvalidAppEventModeException(val);
        }

        public int getVal() {
            return val;
        }
    }

    enum GestureType {
        NONE    (0x0000),
        PRESS   (0x0001),
        TAP     (0x0002),
        FLICK   (0x0004),
        ZOOM    (0x0008),
        ROTATE  (0x0010),
        MOVE    (0x0020),
        HOLD    (0x0040),
        RELEASE (0x0080),
        SCROLL  (0x0100),
        ALL     (0xFFFF);

        private final int flagValue;

        private GestureType(int val) {
            flagValue = val;
        }

        public static EnumSet<GestureType> combine(GestureType arg1, GestureType arg2) {
            return EnumSet.of(arg1, arg2);
        }

        public static EnumSet<GestureType> getFromApiValue(int value) {
            EnumSet<GestureType> result = EnumSet.noneOf(GestureType.class);

            if ((value & PRESS.getFlagValue()) != 0)
                result.add(PRESS);

            if ((value & TAP.getFlagValue()) != 0)
                result.add(TAP);

            if ((value & FLICK.getFlagValue()) != 0)
                result.add(FLICK);

            if ((value & ZOOM.getFlagValue()) != 0)
                result.add(ZOOM);

            if ((value & ROTATE.getFlagValue()) != 0)
                result.add(ROTATE);

            if ((value & MOVE.getFlagValue()) != 0)
                result.add(MOVE);

            if ((value & HOLD.getFlagValue()) != 0)
                result.add(HOLD);

            if ((value & RELEASE.getFlagValue()) != 0)
                result.add(RELEASE);

            if ((value & SCROLL.getFlagValue()) != 0)
                result.add(SCROLL);

            if (result.isEmpty())
                result.add(NONE); // This is probably useless?
            else if (result.equals(EnumSet.range(PRESS, SCROLL)))
                result.add(ALL);

            return result;
        }

        public static int convertToInteger(EnumSet<GestureType> set) {
            int result = 0;

            for (GestureType type : set) {
                result |= type.getFlagValue();
            }

            return result;
        }

        public int getFlagValue() {
            return flagValue;
        }
    }

    enum HardwareType {
        INVALID,
        SWITCHBLADE,
        UNDEFINED
    }

    int RzSBStart();

    void RzSBStop();

    int RzSBQueryCapabilities(Capabilities.ByReference capabilities);

    int RzSBRenderBuffer(TargetDisplay target, BufferParams.ByValue bufferParams);

    int RzSBSetImageDynamicKey(DynamicKeyType dk, DynamicKeyState state, String filename);

    int RzSBSetImageTouchpad(String filename);

    int RzSBAppEventSetCallback(AppEventCallbackFunction callback);

    int RzSBDynamicKeySetCallback(DynamicKeyCallbackFunction callback);

    int RzSBCaptureKeyboard(boolean enable);

    int RzSBKeyboardCaptureSetCallback(KeyboardCallbackFunction callback);

    int RzSBGestureSetCallback(TouchpadGestureCallbackFunction callback);

    int RzSBEnableGesture(int gestureType, boolean enable);

    int RzSBEnableOSGesture(int gestureType, boolean enable);

    class Point extends Structure {
        public int x;
        public int y;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("x", "y");
        }
    }

    class Capabilities extends Structure {
        public static class ByReference extends Capabilities implements Structure.ByReference { }

        public WinDef.ULONG version;
        public WinDef.ULONG beVersion;
        public HardwareType hardwareType;
        public WinDef.ULONG numSurfaces;
        public Point[] surfaceGeometry;
        public WinDef.UINT[] pixelFormat;
        public WinDef.BYTE numDynamicKeys;
        public Point dynamicKeyArrangement;
        public Point dynamicKeySize;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("version", "beVersion", "hardwareType", "numSurfaces", "surfaceGeometry",
                                 "pixelFormat", "numDynamicKeys", "dynamicKeyArrangement", "dynamicKeySize");
        }
    }

    class BufferParams extends Structure {
        public static class ByValue extends BufferParams implements Structure.ByValue { }

        public PixelType pixelType;
        public WinDef.UINT dataSize;
        public WinDef.INT_PTR ptrData;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("pixelType", "dataSize", "ptrData");
        }
    }
}

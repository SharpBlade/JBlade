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
 * Disclaimer: JBlade is in no way affiliated
 * with Razer and/or any of its employees and/or licensors.
 * Adam Hellberg does not take responsibility for any harm caused, direct
 * or indirect, to any Razer peripherals via the use of SharpBlade.
 *
 * "Razer" is a trademark of Razer USA Ltd.
 */

package com.sharparam.jblade.razer;

import com.sharparam.jblade.annotations.APIComponent;
import com.sharparam.jblade.annotations.JNAComponent;
import com.sharparam.jblade.razer.exceptions.RazerInvalidAppEventModeException;
import com.sharparam.jblade.razer.exceptions.RazerInvalidTargetDisplayException;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * Created on 2014-01-25.
 *
 * @author Sharparam
 */
public class RazerAPI {
    /**
     * The DLL file containing the SDK functions.
     * Must be located in the system PATH.
     */
    public static final String DLL_NAME = "RzSwitchbladeSDK2.dll";

    /**
     * RazerLibrary instance object used in code.
     */
    public static final RazerAPI INSTANCE = new RazerAPI();

    private final RazerLibrary lib;

    private final Logger log;

    /*
     * Definitions for the Dynamic Key display region of the Switchblade.
     */

    /**
     * Number of dynamic keys per row on the device.
     */
    @APIComponent
    public static final int DYNAMIC_KEYS_PER_ROW = 5;

    /**
     * Number of rows on the dynamic keys.
     */
    @APIComponent
    public static final int DYNAMIC_KEY_ROWS = 2;

    /**
     * Total number of dynamic keys that exist on the device.
     */
    @APIComponent
    public static final int DYNAMIC_KEYS_COUNT = DYNAMIC_KEYS_PER_ROW * DYNAMIC_KEY_ROWS;

    /**
     * The width of one dynamic key, in pixels.
     * Note that this refers to the width of the display area on a dynamic key, not physical size.
     */
    @APIComponent
    public static final int DYNAMIC_KEY_WIDTH = 115;

    /**
     * The height of one dynamic key, in pixels.
     * Note that this refers to the height of the display area on a dynamic key, not physical size.
     */
    @APIComponent
    public static final int DYNAMIC_KEY_HEIGHT = 115;

    /**
     * Size of image data for one dynamic key.
     */
    @APIComponent
    public static final int DYNAMIC_KEY_IMAGE_DATA_SIZE = DYNAMIC_KEY_WIDTH * DYNAMIC_KEY_HEIGHT * 2;

    /*
     * Definitions for the Touchpad display region of the Switchblade.
     */

    /**
     * Width of the touchpad on standard devices.
     */
    @APIComponent
    public static final int TOUCHPAD_WIDTH = 800;

    /**
     * Height of the touchpad on standard devices.
     */
    @APIComponent
    public static final int TOUCHPAD_HEIGHT = 480;

    /**
     * Size of image data to cover the touchpad.
     */
    @APIComponent
    public static final int TOUCHPAD_IMAGE_DATA_SIZE = TOUCHPAD_WIDTH * TOUCHPAD_HEIGHT * 2;

    /**
     * Color depth of the device's display areas.
     */
    @APIComponent
    public static final int DISPLAY_COLOR_DEPTH = 16;

    /**
     * Max string length.
     */
    @APIComponent
    public static final int MAX_STRING_LENGTH = 260;

    /**
     * Maximum supported surfaces.
     */
    @APIComponent
    public static final int MAX_SUPPORTED_SURFACES = 2;

    /**
     * Invalid pixel format.
     */
    @APIComponent
    public static final int PIXEL_FORMAT_INVALID = 0;

    /**
     * RGB565 pixel format, used by standard SwitchBlade devices.
     */
    @APIComponent
    public static final int PIXEL_FORMAT_RGB565 = 1;

    /**
     * Interface implementing callback function for the dynamic key callback.
     */
    interface DynamicKeyCallbackInterface extends StdCallLibrary.StdCallCallback {
        /**
         * Method handling the dynamic key callback.
         * @param dynamicKeyType The key type that was changed.
         * @param dynamicKeyState The new state of the key.
         * @return HRESULT code indicating success or failure.
         */
        @JNAComponent
        int callback(int dynamicKeyType, int dynamicKeyState);
    }

    /**
     * Interface implementing callback function for the app event callback.
     */
    interface AppEventCallbackInterface extends StdCallLibrary.StdCallCallback {
        /**
         * Method handling the app event callback.
         * @param appEventType The type of app event.
         * @param dwAppMode The app mode.
         * @param dwProcessID THe process ID.
         * @return HRESULT code indicating success or failure.
         */
        @JNAComponent
        int callback(int appEventType, WinDef.UINT dwAppMode, WinDef.UINT dwProcessID);
    }

    /**
     * Interface implementing callback function for the touchpad gesture callback.
     */
    interface TouchpadGestureCallbackInterface extends StdCallLibrary.StdCallCallback {
        /**
         * Method handling the touchpad gesture callback.
         * @param gestureType The type of gesture.
         * @param dwParameters Parameters specific to gesture type.
         * @param wXPos X position where gesture happened.
         * @param wYPos Y position where gesture happened.
         * @param wZPos Z position where gesture happened.
         * @return HRESULT code indicating success or failure.
         */
        @JNAComponent
        int callback(int gestureType, WinDef.UINT dwParameters,
                     WinDef.USHORT wXPos, WinDef.USHORT wYPos, WinDef.USHORT wZPos);
    }

    /**
     * Interface implementing callback function for the keyboard callback.
     */
    interface KeyboardCallbackInterface extends StdCallLibrary.StdCallCallback {
        /**
         * Method handling the keyboard callback.
         * @param uMsg Indicates the keyboard event (WM_KEYDOWN, WM_KEYUP, WM_CHAR).
         * @param wParam Indicates the key that was pressed (Virtual Key value).
         * @param lParam Indicates key modifiers (CTRL, ALT, SHIFT).
         * @return HRESULT code indicating success or failure.
         */
        @JNAComponent
        int callback(WinDef.UINT uMsg, WinDef.UINT_PTR wParam, WinDef.INT_PTR lParam);
    }

    /**
     * Possible states of a dynamic key.
     */
    public enum DynamicKeyState {
        /**
         * No active state.
         */
        NONE,

        /**
         * Depressed state.
         */
        UP,

        /**
         * Pressed state.
         */
        DOWN,

        /**
         * Being held.
         */
        HOLD,

        /**
         * Invalid key state.
         */
        INVALID
    }

    /**
     * Direction of motion/gesture.
     */
    public enum Direction {
        /**
         * No direction.
         */
        NONE,

        /**
         * To the left.
         */
        LEFT,

        /**
         * To the right.
         */
        RIGHT,

        /**
         * Upwards (smaller Y).
         */
        UP,

        /**
         * Downwards (larger Y).
         */
        DOWN,

        /**
         * Invalid direction.
         */
        INVALID
    }

    /**
     * Dynamic keys available on the SwitchBlade device.
     */
    public enum DynamicKeyType {
        /**
         * None of the keys.
         */
        NONE,

        /**
         * Key #1.
         */
        DK1 (TargetDisplay.DK1),

        /**
         * Key #2.
         */
        DK2 (TargetDisplay.DK2),

        /**
         * Key #3.
         */
        DK3 (TargetDisplay.DK3),

        /**
         * Key #4.
         */
        DK4 (TargetDisplay.DK4),

        /**
         * Key #5.
         */
        DK5 (TargetDisplay.DK5),

        /**
         * Key #6.
         */
        DK6 (TargetDisplay.DK6),

        /**
         * Key #7.
         */
        DK7 (TargetDisplay.DK7),

        /**
         * Key #8.
         */
        DK8 (TargetDisplay.DK8),

        /**
         * Key #9.
         */
        DK9 (TargetDisplay.DK9),

        /**
         * Key #10.
         */
        DK10 (TargetDisplay.DK10),

        /**
         * Invalid dynamic key.
         */
        INVALID;

        /**
         * Number of keys available.
         */
        public static final int COUNT = 10;

        private final TargetDisplay targetDisplay;

        private DynamicKeyType() {
            targetDisplay = null;
        }

        private DynamicKeyType(TargetDisplay targetDisplay) {
            this.targetDisplay = targetDisplay;
        }

        TargetDisplay getTargetDisplay() {
            return targetDisplay;
        }
    }

    /**
     * Target displays available on SwitchBlade device.
     * NOTE: When passed to API functions, use {@link #getVal()}.
     */
    public enum TargetDisplay {
        /**
         * The touchpad screen.
         */
        WIDGET (0x10000),

        /**
         * Dynamic key #1.
         */
        DK1    (0x10001),

        /**
         * Dynamic key #2.
         */
        DK2    (0x10002),

        /**
         * Dynamic key #3.
         */
        DK3    (0x10003),

        /**
         * Dynamic key #4.
         */
        DK4    (0x10004),

        /**
         * Dynamic key #5.
         */
        DK5    (0x10005),

        /**
         * Dynamic key #6.
         */
        DK6    (0x10006),

        /**
         * Dynamic key #7.
         */
        DK7    (0x10007),

        /**
         * Dynamic key #8.
         */
        DK8    (0x10008),

        /**
         * Dynamic key #9.
         */
        DK9    (0x10009),

        /**
         * Dynamic key #10.
         */
        DK10   (0x1000A);

        private final int val;

        private TargetDisplay(final int val) {
            this.val = val;
        }

        /**
         * Converts integer value returned from API functions to a TargetDisplay value.
         * @param val Integer value returned from API functions.
         * @return TargetDisplay value.
         * @throws com.sharparam.jblade.razer.exceptions.RazerInvalidTargetDisplayException If the passed in integer value is invalid,
         *                                            a RazerInvalidTargetDisplayException is thrown.
         */
        public TargetDisplay getTargetDisplayFromApiValue(final int val) throws RazerInvalidTargetDisplayException {
            for (final TargetDisplay disp : TargetDisplay.values()) {
                if (disp.getVal() == val)
                    return disp;
            }

            throw new RazerInvalidTargetDisplayException(val);
        }

        /**
         * Gets a RazerLibrary compatible representation of this TargetDisplay value.
         * @return A value usable with RazerLibrary.
         */
        public int getVal() {
            return val;
        }
    }

    /**
     * Supported pixel formats.
     */
    public enum PixelType {
        /**
         * RGB565 pixel format.
         */
        @APIComponent
        RGB565
    }

    /**
     * App event types used by Razer's AppEvent callback system.
     */
    public enum AppEventType {
        /**
         * No/empty app event.
         */
        NONE,

        /**
         * The Switchblade framework has activated the SDK application.
         * The application can resume its operations and update the Switchblade UI display.
         */
        ACTIVATED,

        /**
         * The application has been deactivated to make way for another application.
         * In this state, the SDK application will not receive any Dynamic Key or Gesture events,
         * nor will it be able to update the Switchblade displays.
         */
        DEACTIVATED,

        /**
         * The Switchblade framework has initiated a request to close the application.
         * The application should perform cleanup and can terminate on its own when done.
         */
        CLOSE,

        /**
         * The Switchblade framework will forcibly close the application.
         * This event is always preceded by the {@link #CLOSE} event.
         * Cleanup should be done there.
         */
        EXIT,

        /**
         * Invalid app event.
         */
        INVALID
    }

    /**
     * Mode that app is running in.
     * NOTE: When passed to API functions, use {@link #getVal()}.
     */
    public enum AppEventMode {
        /**
         * Running in applet mode.
         */
        APPLET (0x02),

        /**
         * Running normally.
         */
        NORMAL (0x04);

        private final int val;

        private AppEventMode(final int val) {
            this.val = val;
        }

        /**
         * Converts integer value returned from API functions to an
         * AppEventMode value.
         * @param val Integer value returned from API functions.
         * @return AppEventMode value.
         * @throws RazerInvalidAppEventModeException If the passed in integer value is invalid,
         *                                           a RazerInvalidTargetDisplayException is thrown.
         */
        public static AppEventMode getAppEventModeFromApiValue(final int val) throws RazerInvalidAppEventModeException {
            for (final AppEventMode mode : AppEventMode.values()) {
                if (mode.getVal() == val)
                    return mode;
            }

            throw new RazerInvalidAppEventModeException(val);
        }

        /**
         * Gets a RazerAPI compatible representation of this
         * AppEventMode value.
         * @return A value usable with RazerLibrary.
         */
        public int getVal() {
            return val;
        }
    }

    /**
     * Gesture types supported by the device.
     * NOTE: When passing a GestureType value to the Switchblade API, use the {@link #getFlagValue()} method.
     */
    public enum GestureType {
        /**
         * Invalid or no gesture.
         */
        NONE    (0x0000),

        /**
         * A press on the touchpad.
         */
        PRESS   (0x0001),

        /**
         * A tap on the touchpad.
         */
        TAP     (0x0002),

        /**
         * Flick with finger(s?) on the touchpad.
         */
        FLICK   (0x0004),

        /**
         * Two fingers pinching out on touchpad.
         */
        ZOOM    (0x0008),

        /**
         * Two fingers rotating on touchpad.
         */
        ROTATE  (0x0010),

        /**
         * Finger is moving around on touchpad.
         */
        MOVE    (0x0020),

        /**
         * Finger is being held on touchpad.
         */
        HOLD    (0x0040),

        /**
         * Finger was released from touchpad.
         */
        RELEASE (0x0080),

        /**
         * Scroll gesture.
         */
        SCROLL  (0x0100),

        /**
         * Every gesture.
         */
        ALL     (0xFFFF);

        private final int flagValue;

        private GestureType(final int val) {
            flagValue = val;
        }

        /**
         * Converts an integer value returned from RazerAPI to an EnumSet containing all relevant
         * GestureType flags.
         * @param value The API integer value to convert.
         * @return An EnumSet with the GestureType values contained in the integer.
         */
        public static EnumSet<GestureType> getFromApiValue(final int value) {
            final EnumSet<GestureType> result = EnumSet.noneOf(GestureType.class);

            final EnumSet<GestureType> normalGestures = EnumSet.range(PRESS, SCROLL);

            for (final GestureType type : normalGestures)
                if ((value & type.getFlagValue()) != 0)
                    result.add(type);

            if (result.isEmpty())
                result.add(NONE); // This is probably useless?
            else if (result.equals(normalGestures))
                result.add(ALL);

            return result;
        }

        /**
         * Converts an EnumSet of GestureType values to an integer representation.
         * @param set The EnumSet to convert.
         * @return An integer value representing the gestures in the EnumSet.
         */
        public static int convertToInteger(final EnumSet<GestureType> set) {
            int result = 0;

            for (final GestureType type : set)
                result |= type.getFlagValue();

            return result;
        }

        /**
         * Gets the flag value of this gesture, compatible with RazerLibrary functions.
         * @return A value compatible with RazerLibrary for this gesture.
         */
        public int getFlagValue() {
            return flagValue;
        }
    }

    /**
     * Different hardware types returned by
     * {@link #RzSBQueryCapabilities(Capabilities.ByReference)}.
     */
    public enum HardwareType {
        /**
         * Invalid hardware.
         */
        INVALID,

        /**
         * SwitchBlade device.
         */
        SWITCHBLADE,

        /**
         * Unknown device type.
         */
        UNDEFINED
    }

    public enum Hresult {
        /**
         * Unknown error.
         */
        UNKNOWN (-1),

        /**
         * Generic success return value. Defined as S_OK.
         */
        RZSB_OK (0x00000000),

        /**
         * The application failed to establish a connection with the SwitchBlade device.
         * Call GetLastError() for additional error information. Defined as E_FAIL.
         */
        RZSB_UNSUCCESSFUL (0x80004005),

        /**
         * One or more of the parameters is invalid.
         * Call GetLastError() for additional error information. Defined as E_INVALIDARG.
         */
        RZSB_INVALID_PARAMETER (0x80070057),

        /**
         * One or more of the pointers point to data that is either not fully readable or writable.
         * Defined as E_POINTER.
         */
        RZSB_INVALID_POINTER (0x80004003),

        RZSB_ABORTED (0x80004004),
        RZSB_NO_INTERFACE (0x80004002),
        RZSB_NOT_IMPLEMENTED (0x80004001),

        /**
         * The referenced file could not be found. Defined as ERROR_FILE_NOT_FOUND.
         */
        RZSB_FILE_NOT_FOUND (0x00000002),

        RZSB_GENERIC_BASE (0x20000000),

        /**
         * Zero-length files are not allowed.
         */
        RZSB_FILE_ZERO_SIZE (0x20000001),

        /**
         * The filepath points to a file that does not exist.
         */
        RZSB_FILE_INVALID_NAME (0x20000002),

        /**
         * Zero-sized images are not allowed.
         */
        RZSB_FILE_INVALID_TYPE (0x20000003),

        /**
         * The number of bytes read was different from the number of bytes expected.
         */
        RZSB_FILE_READ_ERROR (0x20000004),

        /**
         * The image file is not a supported file format.
         */
        RZSB_FILE_INVALID_FORMAT (0x20000005),

        /**
         * The file length does not match the expected length.
         */
        RZSB_FILE_INVALID_LENGTH (0x20000006),

        /**
         * The length of the filepath is greater than 260 characters ({@link #MAX_STRING_LENGTH}.
         */
        RZSB_FILE_NAMEPATH_TOO_LONG (0x20000007),

        /**
         * The dimensions of the image do not match the requirements for the display.
         */
        RZSB_IMAGE_INVALID_SIZE (0x20000008),

        /**
         * The image could not be verified as valid.
         * Call GetLastError() for additional error information.
         */
        RZSB_IMAGE_INVALID_DATA (0x20000009),

        /**
         * Must be Win7 or greater workstation.
         */
        RZSB_WIN_VERSION_INVALID (0x2000000A),

        /**
         * Generic callback errors, but specific to the SDK.
         */
        RZSB_CALLBACK_BASE (0x20010000),

        /**
         * The application tried to call or clear a callback that was never set.
         */
        RZSB_CALLBACK_NOT_SET (0x20010001),

        /**
         * Tried to set a previously set callback without clearing it first.
         */
        RZSB_CALLBACK_ALREADY_SET (0x20010002),

        /**
         * Setting the callback failed on the server.
         */
        RZSB_CALLBACK_REMOTE_FAIL (0x20010003),

        /**
         * Control.
         */
        RZSB_CONTROL_BASE_ERROR (0x20020000),

        /**
         * Unlock when we didn't lock? -- careless
         */
        RZSB_CONTROL_NOT_LOCKED (0x20020001),

        /**
         * Someone else has the lock.
         */
        RZSB_CONTROL_LOCKED (0x20020002),

        /**
         * We already locked it? -- careless
         */
        RZSB_CONTROL_ALREADY_LOCKED (0x20020003),

        /**
         * Preemption took place!
         */
        RZSB_CONTROL_PREEMPTED (0x20020004),

        /**
         * Dynamic keys.
         */
        RZSB_DK_BASE_ERROR (0x20040000),

        /**
         * The dynamic key referenced is not a valid dynamic key value.
         * Refer to SwitchBladeSDK_types.h for valid values.
         */
        RZSB_DK_INVALID_KEY (0x20040001),

        /**
         * The dynamic key state referenced is not a valid dynamic key state.
         * Refer to SwitchBladeSDK_types.h for valid states.
         */
        RZSB_DK_INVALID_KEY_STATE (0x20040002),

        /**
         * Touchpad (buttons and gestures).
         */
        RZSB_TOUCHPAD_BASE_ERROR (0x20080000),

        /**
         * Invalid gesture detected by the SwitchBlade touchpad.
         */
        RZSB_TOUCHPAD_INVALID_GESTURE (0x20080001),

        /**
         * Interface-specific errors.
         */
        RZSB_INTERNAL_BASE_ERROR (0x20100000),

        /**
         * Callback structures already initialized.
         */
        RZSB_ALREADY_STARTED (0x20100001),

        /**
         * The internal callstack is in disorder.
         * This is sometimes due to not having previously called {@link #RzSBStart()}.
         */
        RZSB_NOT_STARTED (0x20100002),

        /**
         * Connection to application services failed.
         */
        RZSB_CONNECTION_ERROR (0x20100003),

        /**
         * Unknown error -- catch-all for now.
         */
        RZSB_INTERNAL_ERROR (0x20100004);

        private final int val;

        private Hresult(final int val) {
            this.val = val;
        }

        // TODO: Fix poor wording on following docs

        // Useless?
        /**
         * Checks if a given HRESULT value means success.
         * @param value Value to check.
         * @return True if successful, false otherwise.
         */
        @APIComponent
        public static boolean isSuccessful(final Hresult value) {
            return value == RZSB_OK;
        }

        /**
         * Checks if a given integer value represents a successful Hresult.
         * @param value Value to check.
         * @return True if successful, false otherwise.
         */
        @APIComponent
        public static boolean isSuccessful(final int value) {
            return value == RZSB_OK.getVal();
        }

        /**
         * Checks if this value means success.
         * @return True if successful, false otherwise.
         */
        @APIComponent
        public boolean isSuccessful() {
            return this == RZSB_OK;
        }

        // Useless?
        /**
         * Checks if a given HRESULT value is erroneous.
         * @param value Value to check.
         * @return True if erroneous, false otherwise.
         */
        @APIComponent
        public static boolean isError(final Hresult value) {
            return value != RZSB_OK;
        }

        /**
         * Checks if a given integer value represents an erroneous Hresult value.
         * @param value Value to check.
         * @return True if erroneous, false otherwise.
         */
        @APIComponent
        public static boolean isError(final int value) {
            return value != RZSB_OK.getVal();
        }

        /**
         * Checks if this value is erroneous.
         * @return True if erroneous, false otherwise.
         */
        @APIComponent
        public boolean isError() {
            return this != RZSB_OK;
        }

        /**
         * Converts the given integer value to an Hresult value.
         * @param value Value to convert.
         * @return The Hresult value of the integer.
         */
        public static Hresult getFromApiValue(final int value) {
            for (final Hresult err : Hresult.values()) {
                if (err.getVal() == value)
                    return err;
            }

            return UNKNOWN;
        }

        /**
         * Gets the error code of this Hresult value.
         * @return The error code.
         */
        @APIComponent
        public int getVal() {
            return val;
        }
    }

    private RazerAPI() {
        log = LogManager.getLogger();
        log.info("RazerAPI is initializing");
        log.debug("Loading RazerLibrary: {}", DLL_NAME);
        lib = (RazerLibrary) Native.loadLibrary(DLL_NAME, RazerLibrary.class);
        log.debug("RazerLibrary loaded!");
    }

    /**
     * Grants access to the Switchblade device, establishing application connections.
     * This method sets up the connections that allow an application to access the Switchblade hardware device.
     * This routine returns RZSB_OK on success, granting the calling application control of the device.
     * Subsequent calls to this routine prior to a matching {@link #RzSBStop()} call are ignored.
     * This method must be called before other Switchblade SDK routines will succeed.
     * This method must always be accompanied by an {@link #RzSBStop()}.
     * COM initialization should be called prior to calling this method.
     * If the application developer intends to use Single-Threaded Apartment model (STA) and call the SDK
     * functions within the same thread where the COM was initialized, then {@code CoInitialize()} should be called
     * before this method. Note that some MFC controls automatically initializes to STA.
     * If the application developer intends to call the SDK functions on different threads,
     * then the {@code CoInitializeEx()} should be called before this method.
     * Note: When this method is called without the COM being initialized
     * (e.g. thru calling {@code CoInitializeEx})
     * the SDK initializes the COM to Multi-Threaded Apartment (MTA) model.
     * As such, callers must invoke SDK functions from an MTA thread.
     * Future SDK versions will move these calls into an isolated STA, giving application developers additional
     * freedom to use COM in any fashion.
     * However, application developers may already implement their own processing to isolate the SDK
     * initialization and calls to avoid the issues for COM in different threading models.
     * @return HRESULT code indicating success or failure.
     */
    @SuppressWarnings("InstanceMethodNamingConvention")
    @APIComponent
    public Hresult RzSBStart() {
        return Hresult.getFromApiValue(lib.RzSBStart());
    }

    /**
     * Cleans up the Switchblade device connections and releases it for other applications.
     * This method cleans up the connections made by {@link #RzSBStart()}.
     * This routine releases an application’s control of the Switchblade hardware device,
     * allowing other applications to take control.
     * Subsequent calls to this routine prior to a matching {@link #RzSBStart()} are ignored.
     * If an application terminates after calling {@link #RzSBStart()}
     * without a matching call to this method,
     * other applications may fail to acquire control of the Switchblade device.
     * In this case, manually kill the framework processes.
     */
    @SuppressWarnings("InstanceMethodNamingConvention")
    @APIComponent
    public void RzSBStop() {
        lib.RzSBStop();
    }

    /**
     * Collects information about the SDK and the hardware supported.
     * @param capabilities A pointer to a previously allocated structure of type Capabilities.
     *                     On successful execution, this routine fills the parameters in capabilities with the
     *                     proper information about the SDK and supported hardware.
     * @return HRESULT code indicating success or failure.
     */
    @SuppressWarnings("InstanceMethodNamingConvention")
    @APIComponent
    public Hresult RzSBQueryCapabilities(final Capabilities.ByReference capabilities) {
        return Hresult.getFromApiValue(lib.RzSBQueryCapabilities(capabilities));
    }

    /**
     * Controls output to the Switchblade display.
     * The application can send bitmap data buffer directly to the Switchblade track pad
     * display thru this function providing are more direct and faster way of updating the display.
     * Since the function accepts the buffer for bottom-up bitmap,
     * the application should invert the original image along its vertical axis prior to calling the function.
     * This can be done easily with {@code BitBlit} and {@code StretchBlt} APIs.
     * @param target Specifies the target location on the Switchblade display – the main display or one of the dynamic key areas.
     *               Please refer to the definition for TargetDisplay for accepted values.
     * @param bufferParams A pointer to a buffer parameter structure of type BufferParams that
     *                     must be filled with the appropriate information for the image being sent to the render buffer.
     *                     This input parameter is an RGB565 bitmap image buffer with a bottom-up orientation.
     *                     Please refer to the definition for BufferParams for further detail.
     * @return HRESULT code indicating success or failure.
     */
    @SuppressWarnings("InstanceMethodNamingConvention")
    @APIComponent
    public Hresult RzSBRenderBuffer(final TargetDisplay target, final BufferParams.ByValue bufferParams) {
        return Hresult.getFromApiValue(lib.RzSBRenderBuffer(target.getVal(), bufferParams));
    }

    /**
     * Set images on the Switchblade UI’s Dynamic Keys.
     * Animation in GIF files are not supported.
     * @param dk DynamicKeyType indicating which key to set the image on.
     * @param state The desired dynamic key state (up, down) for the specified image. See <see cref="DynamicKeyState" /> for accepted values.
     * @param filename The image file path for the given state. This image should be 115 x 115 pixels in dimension.
     *                 Accepted file formats are BMP, GIF, JPG, and PNG.
     * @return HRESULT code indicating success or failure.
     */
    @SuppressWarnings("InstanceMethodNamingConvention")
    @APIComponent
    public Hresult RzSBSetImageDynamicKey(final DynamicKeyType dk, final DynamicKeyState state, final String filename) {
        return Hresult.getFromApiValue(lib.RzSBSetImageDynamicKey(dk, state, filename));
    }

    /**
     * Places an image on the main Switchblade display.
     * Animation in GIF files are not supported.
     * @param filename File path to the image to be placed on the main Switchblade display.
     *                 This image should be 800 x 480 pixels in dimension. Accepted file formats are BMP, GIF, JPG, and PNG.
     * @return HRESULT code indicating success or failure.
     */
    @SuppressWarnings("InstanceMethodNamingConvention")
    @APIComponent
    public Hresult RzSBSetImageTouchpad(final String filename) {
        final WString wStr = new WString(filename);
        return Hresult.getFromApiValue(lib.RzSBSetImageTouchpad(wStr));
    }

    /**
     * Sets the callback function for application event callbacks.
     * @param callback Pointer to a callback function. If this argument is set to NULL, the routine clears the previously set callback function.
     * @return HRESULT code indicating success or failure.
     */
    @SuppressWarnings("InstanceMethodNamingConvention")
    @APIComponent
    public Hresult RzSBAppEventSetCallback(final AppEventCallbackInterface callback) {
        return Hresult.getFromApiValue(lib.RzSBAppEventSetCallback(callback));
    }

    /**
     * Sets the callback function for dynamic key events.
     * @param callback Pointer to a callback function. If this argument is set to NULL, the routine clears the previously set callback function.
     * @return HRESULT code indicating success or failure.
     */
    @SuppressWarnings("InstanceMethodNamingConvention")
    @APIComponent
    public Hresult RzSBDynamicKeySetCallback(final DynamicKeyCallbackInterface callback) {
        return Hresult.getFromApiValue(lib.RzSBDynamicKeySetCallback(callback));
    }

    /**
     * Enables or disables the keyboard capture functionality.
     * When the capture is enabled, the SDK application can receive keyboard
     * input events through the callback assigned using <see cref="RzSBKeyboardCaptureSetCallback" />.
     * The OS will not receive any keyboard input from the Switchblade device as long as the capture is active.
     * Hence, applications must release the capture when no longer in use (call <see cref="RzSBEnableGesture" /> with false as parameter).
     * The function only affects the keyboard device where the application is running. Other keyboard devices will work normally.
     * @param enable The enable state. true enables the capture while false disables it.
     * @return HRESULT code indicating success or failure.
     */
    @SuppressWarnings("InstanceMethodNamingConvention")
    @APIComponent
    public Hresult RzSBCaptureKeyboard(final boolean enable) {
        return Hresult.getFromApiValue(lib.RzSBCaptureKeyboard(enable));
    }

    /**
     * Sets the callback function for keyboard events.
     * @param callback Pointer to a callback function. If this argument is set to NULL, the routine clears the previously set callback function.
     * @return HRESULT code indicating success or failure.
     */
    @SuppressWarnings("InstanceMethodNamingConvention")
    @APIComponent
    public Hresult RzSBKeyboardCaptureSetCallback(final KeyboardCallbackInterface callback) {
        return Hresult.getFromApiValue(lib.RzSBKeyboardCaptureSetCallback(callback));
    }

    /**
     * Sets the callback function for gesture events.
     * @param callback Pointer to a callback function. If this argument is set to NULL, the routine clears the previously set callback function.
     * @return HRESULT code indicating success or failure.
     */
    @SuppressWarnings("InstanceMethodNamingConvention")
    @APIComponent
    public Hresult RzSBGestureSetCallback(final TouchpadGestureCallbackInterface callback) {
        return Hresult.getFromApiValue(lib.RzSBGestureSetCallback(callback));
    }

    /**
     * Enables or disables gesture events.
     * In nearly all cases, gestural events are preceded by a
     * {@link GestureType#PRESS} event.
     * With multiple finger gestures, the first finger contact registers as a press,
     * and the touchpad reports subsequent contacts as the appropriate compound gesture (tap, flick, zoom or rotate).
     * @param gestureType GestureType to be enabled or disabled.
     * @param enable The enable state. true enables the gesture while false disables it.
     * @return HRESULT object indicating success or failure.
     */
    @SuppressWarnings("InstanceMethodNamingConvention")
    @APIComponent
    public Hresult RzSBEnableGesture(final GestureType gestureType, final boolean enable) {
        return RzSBEnableGesture(EnumSet.of(gestureType), enable);
    }

    /**
     * Enables or disables gesture events.
     * In nearly all cases, gestural events are preceded by a
     * {@link GestureType#PRESS} event.
     * With multiple finger gestures, the first finger contact registers as a press,
     * and the touchpad reports subsequent contacts as the appropriate compound gesture (tap, flick, zoom or rotate).
     * @param gestureType Set of GestureType to be enabled or disabled.
     * @param enable The enable state. true enables the gesture while false disables it.
     * @return HRESULT object indicating success or failure.
     */
    @SuppressWarnings("InstanceMethodNamingConvention")
    @APIComponent
    public Hresult RzSBEnableGesture(final EnumSet<GestureType> gestureType, final boolean enable) {
        final int intVal = GestureType.convertToInteger(gestureType);
        log.debug(String.format("Calling RzSBEnableGesture %s with gesture 0x%X", enable, intVal));
        return Hresult.getFromApiValue(lib.RzSBEnableGesture(intVal, enable));
    }

    /**
     * Enables or disables gesture event forwarding to the OS.
     * Setting the {@link GestureType#PRESS} for OS gesture is equivalent to
     * {@link GestureType#PRESS},
     * {@link GestureType#MOVE} and
     * {@link GestureType#RELEASE}.
     * @param gestureType GestureType to be enabled or disabled.
     * @param enable The enable state. true enables the gesture while false disables it.
     * @return HRESULT object indicating success or failure.
     */
    @SuppressWarnings("InstanceMethodNamingConvention")
    @APIComponent
    public Hresult RzSBEnableOSGesture(final GestureType gestureType, final boolean enable) {
        return RzSBEnableOSGesture(EnumSet.of(gestureType), enable);
    }

    /**
     * Enables or disables gesture event forwarding to the OS.
     * Setting the {@link GestureType#PRESS} for OS gesture is equivalent to
     * {@link GestureType#PRESS},
     * {@link GestureType#MOVE} and
     * {@link GestureType#RELEASE}.
     * @param gestureType Set of GestureType to be enabled or disabled.
     * @param enable The enable state. true enables the gesture while false disables it.
     * @return HRESULT object indicating success or failure.
     */
    @SuppressWarnings("InstanceMethodNamingConvention")
    @APIComponent
    public Hresult RzSBEnableOSGesture(final EnumSet<GestureType> gestureType, final boolean enable) {
        final int intVal = GestureType.convertToInteger(gestureType);
        log.debug(String.format("Calling RzSBEnableOSGesture %s with gesture 0x%X", enable, intVal));
        return Hresult.getFromApiValue(lib.RzSBEnableOSGesture(intVal, enable));
    }

    /**
     * Specifies a specific point on the touchpad.
     */
    public static class Point extends Structure {
        /**
         * X position on the touchpad.
         */
        public int x;

        /**
         * Y position on the touchpad.
         */
        public int y;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("x", "y");
        }
    }

    /**
     * Specifies the capabilities of this SwitchBlade device.
     */
    public static class Capabilities extends Structure {
        @JNAComponent
        public static class ByReference extends Capabilities implements Structure.ByReference { }

        /**
         * Version of SDK/hardware.
         */
        public WinDef.ULONG version;

        /**
         * BEVersion returned from capabilities function.
         */
        public WinDef.ULONG beVersion;

        /**
         * Type of device.
         */
        public HardwareType hardwareType;

        /**
         * Number of surfaces available.
         */
        public WinDef.ULONG numSurfaces;

        // NOTE: surfaceGeometry is UNTESTED
        // TODO: TEST surfaceGeometry
        /**
         * Surface geomtery for each surface.
         * Contains {@link #numSurfaces} entries.
         */
        public Point[] surfaceGeometry;

        // NOTE: pixelFormat is UNTESTED
        // TODO: TEST pixelFormat
        /**
         * Pixel format of each surface.
         * Contains {@link #numSurfaces} entries.
         */
        public WinDef.UINT[] pixelFormat;

        // TODO: Possible to use Java byte?
        /**
         * Number of dynamic keys available on device.
         */
        public WinDef.BYTE numDynamicKeys;

        /**
         * Arrangement of dynamic keys.
         */
        public Point dynamicKeyArrangement;

        /**
         * Size of each dynamic key.
         */
        public Point dynamicKeySize;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("version", "beVersion", "hardwareType", "numSurfaces", "surfaceGeometry",
                                 "pixelFormat", "numDynamicKeys", "dynamicKeyArrangement", "dynamicKeySize");
        }
    }

    // NOTE: BufferParams needs THOROUGH TESTING
    // TODO: TEST BufferParams
    /**
     * Buffer data sent to display when rendering image data.
     */
    public static class BufferParams extends Structure {
        @JNAComponent
        public static class ByValue extends BufferParams implements Structure.ByValue { }

        /**
         * Pixel format of the image data.
         */
        public PixelType pixelType;

        /**
         * Total size of the data.
         */
        public WinDef.UINT dataSize;

        /**
         * Pointer to image data.
         */
        public Pointer ptrData;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("pixelType", "dataSize", "ptrData");
        }
    }
}

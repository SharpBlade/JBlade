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

import com.sharparam.jblade.razer.exceptions.RazerNativeException;
import com.sharparam.jblade.razer.exceptions.RazerUnstableShutdownException;
import com.sun.jna.platform.win32.WinDef;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Created on 2014-01-24.
 * @author Sharparam
 */
public class RazerManager implements RazerAPI.AppEventCallbackFunction,
        RazerAPI.DynamicKeyCallbackFunction, RazerAPI.KeyboardCallbackFunction {
    /**
     * The file name to use when creating the control file.
     */
    public static final String RAZER_CONTROL_FILE = "DO_NOT_TOUCH__RAZER_CONTROL_FILE";

    private static final Logger STATIC_LOG = LogManager.getLogger();
    private final Logger log;

    private final RazerAPI razerAPI;

    private DynamicKey[] dynamicKeys;

    private Touchpad touchpad;

    private boolean keyboardCapture;

    /**
     * Initializes a new instance of the RazerManager class with default settings.
     * Equivalent to calling RazerManager(true, false);
     * @throws RazerUnstableShutdownException Thrown if the application was not shut down properly on last run.
     * @throws RazerNativeException Thrown if any native call fails during initialization.
     */
    public RazerManager() throws RazerUnstableShutdownException, RazerNativeException {
        this(true);
    }

    /**
     * Initializes a new instance of the RazerManager class with default setting for useControlFile.
     * Equivalent to calling RazerManager(..., false);
     * @param disableOSGestures If true, all OS gestures will by default be disabled on the touchpad,
     *                          making it do nothing until gestures are enabled manually.
     * @throws RazerUnstableShutdownException Thrown if the application was not shut down properly on last run.
     * @throws RazerNativeException Thrown if any native call fails during initialization.
     */
    public RazerManager(boolean disableOSGestures) throws RazerUnstableShutdownException, RazerNativeException {
        this(disableOSGestures, false);
    }

    /**
     * Initializes a new instance of the RazerManager class.
     * @param disableOSGestures If true, all OS gestures will by default be disabled on the touchpad,
     *                          making it do nothing until gestures are enabled manually.
     * @param useControlFile If true, creates a control file that is checked on subsequent creations of RazerManager,
     *                       initialization will fail if a control file is found and useControlFile is true.
     * @throws RazerUnstableShutdownException Thrown if the application was not shut down properly on last run.
     * @throws RazerNativeException Thrown if any native call fails during initialization.
     */
    public RazerManager(boolean disableOSGestures, boolean useControlFile) throws RazerUnstableShutdownException, RazerNativeException {
        log = LogManager.getLogger();

        log.info("RazerManager is initializing");

        if (useControlFile && isControlFilePresent()) {
            log.error("Detected control file presence, throwing exception.");
            throw new RazerUnstableShutdownException();
        }

        if (useControlFile)
            createControlFile();

        log.debug("Getting RazerLibrary instance");

        razerAPI = RazerAPI.INSTANCE;

        log.debug("Calling RzSBStart()");

        RazerAPI.Hresult result = razerAPI.RzSBStart();
        if (result.failed()) {
            // Try one more time
            result = razerAPI.RzSBStart();
            if (result.failed())
                throw new RazerNativeException("RzSBStart", result);
        }

        log.debug("Registering app event callback");

        result = razerAPI.RzSBAppEventSetCallback(this);
        if (result.failed())
            throw new RazerNativeException("RzSBAppEventSetCallback", result);

        log.info("Setting up touchpad");

        touchpad = new Touchpad();

        if (disableOSGestures)
            touchpad.disableOSGesture(RazerAPI.GestureType.ALL);

        log.debug("Registering dynamic key callback");

        result = razerAPI.RzSBDynamicKeySetCallback(this);
        if (result.failed())
            throw new RazerNativeException("RzSBDynamicKeySetCallback", result);

        log.debug("Registering keyboard callback");
        result = razerAPI.RzSBKeyboardCaptureSetCallback(this);
        if (result.failed())
            throw new RazerNativeException("RzSBKeyboardCaptureSetCallback", result);

        log.debug("Initializing dynamic key array");
        dynamicKeys = new DynamicKey[RazerAPI.DYNAMIC_KEYS_COUNT];
    }

    /**
     * Gets the Touchpad instance.
     * @return The Touchpad instance associated with this manager.
     */
    public Touchpad getTouchpad() {
        return touchpad;
    }

    /**
     * Gets a boolean value indicating whether keyboard capture is currently enabled.
     * @return A boolean value indicating whether keyboard capture is enabled.
     */
    public boolean getKeyboardCapture() {
        return keyboardCapture;
    }

    /**
     * Checks if the Razer control file exists.
     * @return True if the control file exists, false otherwise.
     */
    public static boolean isControlFilePresent() {
        return new File(RAZER_CONTROL_FILE).exists();
    }

    /**
     * Creates the Razer control file.
     */
    public static void createControlFile() {
        try {
            File controlFile = new File(RAZER_CONTROL_FILE);
            boolean result = controlFile.createNewFile();
            if (result) {
                STATIC_LOG.info("createControlFile: Success!");
            } else {
                STATIC_LOG.warn("createControlFile: File already exists");
            }
        } catch (IOException ex) {
            STATIC_LOG.error("createControlFile: Failed to create control file due to IOException.", ex);
        }
    }

    /**
     * Deletes the Razer control file.
     */
    public static void deleteControlFile() {
        File controlFile = new File(RAZER_CONTROL_FILE);
        boolean result = controlFile.delete();
        if (result) {
            STATIC_LOG.info("deleteControlFile: Success!");
        } else {
            STATIC_LOG.warn("deleteControlFile: Failed to delete control files, does it exist?");
        }
    }

    public void stop() {
        stop(true);
    }

    public void stop(boolean cleanup) {
        log.info("RazerManager is stopping! Calling RzSBStop...");
        razerAPI.RzSBStop();
        if (cleanup)
            deleteControlFile();
        log.info("RazerManager has stopped.");
    }

    public DynamicKey getDynamicKey(RazerAPI.DynamicKeyType keyType) {
        return dynamicKeys[keyType.ordinal() - 1];
    }

    // TODO: Implement EnableDynamicKey and DisableDynamicKey

    public void setKeyboardCapture(boolean enabled) throws RazerNativeException {
        if (enabled == keyboardCapture)
            return;

        RazerAPI.Hresult result = razerAPI.RzSBCaptureKeyboard(enabled);
        if (result.failed())
            throw new RazerNativeException("RzSBCaptureKeyboard", result);

        keyboardCapture = enabled;
    }

    // App event handler
    @Override
    public int invoke(RazerAPI.AppEventType appEventType, WinDef.UINT dwAppMode, WinDef.UINT dwProcessID) {
        RazerAPI.Hresult result = RazerAPI.Hresult.RZSB_OK;

        if (appEventType == RazerAPI.AppEventType.INVALID || appEventType == RazerAPI.AppEventType.NONE) {
            log.debug("Unsupported AppEventType: {}", appEventType);
            return result.getVal();
        }

        // TODO: Implement OnAppEvent

        return result.getVal();
    }

    // Dynamic key event handler
    @Override
    public int invoke(RazerAPI.DynamicKeyType dynamicKeyType, RazerAPI.DynamicKeyState dynamicKeyState) {
        RazerAPI.Hresult result = RazerAPI.Hresult.RZSB_OK;

        // TODO: Implement OnDynamicKeyEvent

        int index = dynamicKeyType.ordinal() - 1;
        DynamicKey dk = dynamicKeys[index];
        if (dk == null) {
            log.debug("Key has not been registered by app");
            return result.getVal();
        }

        log.debug("Updating key state");

        // UpdateState will check if it's a valid press and call any event listeners
        dk.updateState(dynamicKeyState);

        return result.getVal();
    }

    // Keyboard event handler
    @Override
    public int invoke(WinDef.UINT type, WinDef.UINT_PTR data, WinDef.INT_PTR modifiers) {
        RazerAPI.Hresult result = RazerAPI.Hresult.RZSB_OK;

        char asChar = (char) data.intValue();

        // TODO: Implement OnKeyboardRawEvent

        // TODO: Finish implementing keyboard handler

        return result.getVal();
    }
}

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

import com.sharparam.jblade.ModifierKeys;
import com.sharparam.jblade.razer.events.*;
import com.sharparam.jblade.razer.exceptions.RazerInvalidAppEventModeException;
import com.sharparam.jblade.razer.exceptions.RazerNativeException;
import com.sharparam.jblade.razer.exceptions.RazerUnstableShutdownException;
import com.sharparam.jblade.razer.listeners.*;
import com.sharparam.jblade.windows.WinAPI;
import com.sun.jna.platform.win32.WinDef;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

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

    private static RazerManager instance;

    private final RazerAPI razerAPI;

    private static RazerAPI.AppEventCallbackFunction appEventCallback;
    private static RazerAPI.DynamicKeyCallbackFunction dkCallback;
    private static RazerAPI.KeyboardCallbackFunction keyboardCallback;

    private final List<AppEventListener> appEventListeners;
    private final List<DynamicKeyListener> dynamicKeyListeners;
    private final List<KeyboardRawListener> keyboardRawListeners;
    private final List<KeyboardKeyListener> keyboardKeyListeners;
    private final List<KeyboardCharListener> keyboardCharListeners;

    private DynamicKey[] dynamicKeys;

    private Touchpad touchpad;

    private boolean keyboardCapture;

    /**
     * Initializes a new instance of the RazerManager class with default settings.
     * Equivalent to calling RazerManager(true, false);
     * @throws RazerUnstableShutdownException Thrown if the application was not shut down properly on last run.
     * @throws RazerNativeException Thrown if any native call fails during initialization.
     */
    private RazerManager() throws RazerUnstableShutdownException, RazerNativeException {
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
    private RazerManager(boolean disableOSGestures) throws RazerUnstableShutdownException, RazerNativeException {
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
    private RazerManager(boolean disableOSGestures, boolean useControlFile) throws RazerUnstableShutdownException, RazerNativeException {
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

        appEventCallback = this;

        result = razerAPI.RzSBAppEventSetCallback(appEventCallback);
        if (result.failed())
            throw new RazerNativeException("RzSBAppEventSetCallback", result);

        log.info("Setting up touchpad");

        touchpad = Touchpad.getInstance();

        if (disableOSGestures) {
            log.debug("disableOSGestures == true; Calling touchpad.disableOSGesture(ALL)");
            touchpad.disableOSGesture(RazerAPI.GestureType.ALL);
        }

        log.debug("Registering dynamic key callback");

        dkCallback = this;

        result = razerAPI.RzSBDynamicKeySetCallback(dkCallback);
        if (result.failed())
            throw new RazerNativeException("RzSBDynamicKeySetCallback", result);

        log.debug("Registering keyboard callback");
        keyboardCallback = this;
        result = razerAPI.RzSBKeyboardCaptureSetCallback(keyboardCallback);
        if (result.failed())
            throw new RazerNativeException("RzSBKeyboardCaptureSetCallback", result);

        log.debug("Initializing dynamic key array");
        dynamicKeys = new DynamicKey[RazerAPI.DYNAMIC_KEYS_COUNT];

        log.debug("Initializing app event listener array");
        appEventListeners = new ArrayList<AppEventListener>();

        log.debug("Initializing dynamic key listener array");
        dynamicKeyListeners = new ArrayList<DynamicKeyListener>();

        log.debug("Initializing keyboard raw listener array");
        keyboardRawListeners = new ArrayList<KeyboardRawListener>();

        log.debug("Initializing keyboard key listener array");
        keyboardKeyListeners = new ArrayList<KeyboardKeyListener>();

        log.debug("Initializing keyboard char listener array");
        keyboardCharListeners = new ArrayList<KeyboardCharListener>();
    }

    public static RazerManager getInstance() throws RazerUnstableShutdownException, RazerNativeException {
        if (instance == null)
            instance = new RazerManager();

        return instance;
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

    public void addAppEventListener(AppEventListener listener) {
        appEventListeners.add(listener);
    }

    public void removeAppEventListener(AppEventListener listener) {
        if (appEventListeners.contains(listener))
            appEventListeners.remove(listener);
    }

    private void onAppEvent(RazerAPI.AppEventType type, RazerAPI.AppEventMode mode, int processId) {
        if (appEventListeners.isEmpty())
            return;

        AppEventEvent event = new AppEventEvent(type, mode, processId);
        for (AppEventListener listener : appEventListeners)
            listener.appEventRaised(event);
    }

    public void addDynamicKeyListener(DynamicKeyListener listener) {
        dynamicKeyListeners.add(listener);
    }

    public void removeDynamicKeyListener(DynamicKeyListener listener) {
        if (dynamicKeyListeners.contains(listener))
            dynamicKeyListeners.remove(listener);
    }

    private void onDynamicKeyStateChanged(DynamicKey dk) {
        if (dynamicKeyListeners.isEmpty())
            return;

        DynamicKeyEvent event = new DynamicKeyEvent(dk.getKeyType(), dk.getState());
        for (DynamicKeyListener listener : dynamicKeyListeners)
            listener.dynamicKeyStateChanged(event);
    }

    private void onDynamicKeyPressed(DynamicKey dk) {
        if (dynamicKeyListeners.isEmpty())
            return;

        DynamicKeyEvent event = new DynamicKeyEvent(dk.getKeyType(), dk.getState());
        for (DynamicKeyListener listener : dynamicKeyListeners)
            listener.dynamicKeyPressed(event);
    }

    private void onDynamicKeyReleased(DynamicKey dk) {
        if (dynamicKeyListeners.isEmpty())
            return;

        DynamicKeyEvent event = new DynamicKeyEvent(dk.getKeyType(), dk.getState());
        for (DynamicKeyListener listener : dynamicKeyListeners)
            listener.dynamicKeyReleased(event);
    }

    public void addKeyboardRawListener(KeyboardRawListener listener) {
        keyboardRawListeners.add(listener);
    }

    public void removeKeyboardRawListener(KeyboardRawListener listener) {
        if (keyboardRawListeners.contains(listener))
            keyboardRawListeners.remove(listener);
    }

    private void onKeyboardRawEvent(int type, int data, int modifiers) {
        if (keyboardRawListeners.isEmpty())
            return;

        KeyboardRawEvent event = new KeyboardRawEvent(type, data, modifiers);
        for (KeyboardRawListener listener : keyboardRawListeners)
            listener.keyboardRawInput(event);
    }

    public void addKeyboardKeyListener(KeyboardKeyListener listener) {
        keyboardKeyListeners.add(listener);
    }

    public void removeKeyboardKeyListener(KeyboardKeyListener listener) {
        if (keyboardKeyListeners.contains(listener))
            keyboardKeyListeners.remove(listener);
    }

    private void onKeyboardKeyPressed(WinAPI.VirtualKey key, EnumSet<ModifierKeys> modifiers) {
        if (keyboardKeyListeners.isEmpty())
            return;

        KeyboardKeyEvent event = new KeyboardKeyEvent(key, modifiers);
        for (KeyboardKeyListener listener : keyboardKeyListeners)
            listener.keyboardKeyPressed(event);
    }

    private void onKeyboardKeyReleased(WinAPI.VirtualKey key, EnumSet<ModifierKeys> modifiers) {
        if (keyboardKeyListeners.isEmpty())
            return;

        KeyboardKeyEvent event = new KeyboardKeyEvent(key, modifiers);
        for (KeyboardKeyListener listener : keyboardKeyListeners)
            listener.keyboardKeyReleased(event);
    }

    public void addKeyboardCharListener(KeyboardCharListener listener) {
        keyboardCharListeners.add(listener);
    }

    public void removeKeyboardCharListener(KeyboardCharListener listener) {
        if (keyboardCharListeners.contains(listener))
            keyboardCharListeners.remove(listener);
    }

    private void onKeyboardCharTyped(char c) {
        if (keyboardCharListeners.isEmpty())
            return;

        KeyboardCharEvent event = new KeyboardCharEvent(c);
        for (KeyboardCharListener listener : keyboardCharListeners)
            listener.keyboardCharTyped(event);
    }

    // App event handler
    @Override
    public int callback(int appEventType, WinDef.UINT dwAppMode, WinDef.UINT dwProcessID) {
        RazerAPI.Hresult result = RazerAPI.Hresult.RZSB_OK;

        RazerAPI.AppEventType eventType = RazerAPI.AppEventType.values()[appEventType];

        if (eventType == RazerAPI.AppEventType.INVALID || eventType == RazerAPI.AppEventType.NONE) {
            log.debug("Unsupported AppEventType: {}", appEventType);
            return result.getVal();
        }

        RazerAPI.AppEventMode appEventMode;

        try {
            appEventMode = RazerAPI.AppEventMode.getAppEventModeFromApiValue(dwAppMode.intValue());
        } catch (RazerInvalidAppEventModeException ex) {
            log.error("Problem parsing app event mode", ex);
            return result.getVal(); // Should we return an error value?
        }

        int processId = dwProcessID.intValue();

        onAppEvent(eventType, appEventMode, processId);

        return result.getVal();
    }

    // Dynamic key event handler
    @Override
    public int callback(int rawDynamicKeyType, int rawDynamicKeyState) {
        RazerAPI.Hresult result = RazerAPI.Hresult.RZSB_OK;

        RazerAPI.DynamicKeyType dkType = RazerAPI.DynamicKeyType.values()[rawDynamicKeyType];
        RazerAPI.DynamicKeyState state = RazerAPI.DynamicKeyState.values()[rawDynamicKeyState];

        int index = dkType.ordinal() - 1;
        DynamicKey dk = dynamicKeys[index];
        if (dk == null) {
            log.debug("Key has not been registered by app");
            return result.getVal();
        }

        log.debug("Updating key state");

        // UpdateState will check if it's a valid press and call any event listeners
        dk.updateState(state);

        onDynamicKeyStateChanged(dk);

        if (dk.getState() == RazerAPI.DynamicKeyState.DOWN &&
                (dk.getPreviousState() == RazerAPI.DynamicKeyState.UP ||
                 dk.getPreviousState() == RazerAPI.DynamicKeyState.NONE))
            onDynamicKeyPressed(dk);
        else if (dk.getState() == RazerAPI.DynamicKeyState.UP &&
                (dk.getPreviousState() == RazerAPI.DynamicKeyState.DOWN ||
                 dk.getPreviousState() == RazerAPI.DynamicKeyState.NONE))
            onDynamicKeyReleased(dk);

        return result.getVal();
    }

    // Keyboard event handler
    @Override
    public int invoke(WinDef.UINT type, WinDef.UINT_PTR data, WinDef.INT_PTR modifiers) {
        RazerAPI.Hresult result = RazerAPI.Hresult.RZSB_OK;

        char asChar = (char) data.intValue();

        int typeVal = type.intValue();
        int dataVal = data.intValue();
        int modVal = modifiers.intValue();

        onKeyboardRawEvent(typeVal, dataVal, modVal);

        WinAPI.MessageType msgType = WinAPI.MessageType.getFromIntegerValue(typeVal);

        if (msgType == WinAPI.MessageType.CHAR && !Character.isISOControl(asChar)) {
            onKeyboardCharTyped(asChar);
        } else if (msgType == WinAPI.MessageType.KEYDOWN || msgType == WinAPI.MessageType.KEYUP) {
            WinAPI.VirtualKey key = WinAPI.VirtualKey.getKeyFromInteger(dataVal);
            EnumSet<ModifierKeys> modKeys = EnumSet.noneOf(ModifierKeys.class);

            WinAPI winAPI = WinAPI.INSTANCE;

            if ((winAPI.GetAsyncKeyState(WinAPI.VirtualKey.SHIFT.getVal()) & WinAPI.KEY_PRESSED) != 0)
                modKeys.add(ModifierKeys.SHIFT);

            if ((winAPI.GetAsyncKeyState(WinAPI.VirtualKey.CONTROL.getVal()) & WinAPI.KEY_PRESSED) != 0)
                modKeys.add(ModifierKeys.CONTROL);

            // MENU == ALT
            if ((winAPI.GetAsyncKeyState(WinAPI.VirtualKey.MENU.getVal()) & WinAPI.KEY_PRESSED) != 0)
                modKeys.add(ModifierKeys.ALT);

            if ((winAPI.GetAsyncKeyState(WinAPI.VirtualKey.CAPITAL.getVal()) & WinAPI.KEY_TOGGLED) != 0)
                modKeys.add(ModifierKeys.CAPS_LOCK);

            if (msgType == WinAPI.MessageType.KEYDOWN)
                onKeyboardKeyPressed(key, modKeys);
            else
                onKeyboardKeyReleased(key, modKeys);
        }

        return result.getVal();
    }
}

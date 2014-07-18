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
import com.sharparam.jblade.annotations.NativeCodeBinding;
import com.sharparam.jblade.razer.events.*;
import com.sharparam.jblade.razer.exceptions.RazerDynamicKeyException;
import com.sharparam.jblade.razer.exceptions.RazerInvalidAppEventModeException;
import com.sharparam.jblade.razer.exceptions.RazerNativeException;
import com.sharparam.jblade.razer.listeners.*;
import com.sharparam.jblade.windows.WinAPI;
import com.sun.jna.platform.win32.WinDef;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Created on 2014-01-24.
 * @author Sharparam
 */
public class RazerManager {
    private final Logger log;

    private static RazerManager instance;

    private final RazerAPI razerAPI;

    @NativeCodeBinding
    private static RazerAPI.AppEventCallbackInterface appEventCallback;

    @NativeCodeBinding
    private static RazerAPI.DynamicKeyCallbackInterface dkCallback;

    @NativeCodeBinding
    private static RazerAPI.KeyboardCallbackInterface keyboardCallback;

    private final List<AppEventListener> appEventListeners;
    private final List<DynamicKeyListener> dynamicKeyListeners;
    private final List<KeyboardRawListener> keyboardRawListeners;
    private final List<KeyboardKeyListener> keyboardKeyListeners;
    private final List<KeyboardCharListener> keyboardCharListeners;

    private final DynamicKey[] dynamicKeys;

    private final Touchpad touchpad;

    private boolean keyboardCapture;

    /**
     * Initializes a new instance of the RazerManager class.
     * @throws RazerNativeException Thrown if any native call fails during initialization.
     */
    private RazerManager() throws RazerNativeException {
        log = LogManager.getLogger();

        log.info("RazerManager is initializing");

        log.debug("Getting RazerLibrary instance");

        razerAPI = RazerAPI.INSTANCE;

        log.debug("Calling RzSBStart()");

        RazerAPI.Hresult result = razerAPI.RzSBStart();
        if (result.isError()) {
            // Try one more time
            result = razerAPI.RzSBStart();
            if (result.isError())
                throw new RazerNativeException("RzSBStart", result);
        }

        log.debug("Registering app event callback");

        appEventCallback = new RazerAPI.AppEventCallbackInterface() {
            @Override
            public int callback(final int appEventType, final WinDef.UINT dwAppMode, final WinDef.UINT dwProcessID) {
                return appEventCallbackFunction(appEventType, dwAppMode, dwProcessID);
            }
        };

        result = razerAPI.RzSBAppEventSetCallback(appEventCallback);
        if (result.isError())
            throw new RazerNativeException("RzSBAppEventSetCallback", result);

        log.info("Setting up touchpad");

        touchpad = Touchpad.getInstance();

        log.debug("Calling touchpad.disableOSGesture(ALL)");
        touchpad.disableOSGesture(RazerAPI.GestureType.ALL);

        log.debug("Registering dynamic key callback");

        dkCallback = new RazerAPI.DynamicKeyCallbackInterface() {
            @Override
            public int callback(final int dynamicKeyType, final int dynamicKeyState) {
                return dynamicKeyCallbackFunction(dynamicKeyType, dynamicKeyState);
            }
        };

        result = razerAPI.RzSBDynamicKeySetCallback(dkCallback);
        if (result.isError())
            throw new RazerNativeException("RzSBDynamicKeySetCallback", result);

        log.debug("Registering keyboard callback");

        keyboardCallback = new RazerAPI.KeyboardCallbackInterface() {
            @Override
            public int callback(final WinDef.UINT uMsg, final WinDef.UINT_PTR wParam, final WinDef.INT_PTR lParam) {
                return keyboardCallbackFunction(uMsg, wParam, lParam);
            }
        };

        result = razerAPI.RzSBKeyboardCaptureSetCallback(keyboardCallback);
        if (result.isError())
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

    public static RazerManager getInstance() throws RazerNativeException {
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
    public boolean isKeyboardCaptureEnabled() {
        return keyboardCapture;
    }

    public void stop() {
        log.info("RazerManager is stopping! Calling RzSBStop...");
        razerAPI.RzSBStop();
        log.info("RazerManager has stopped.");
    }

    public DynamicKey getDynamicKey(final RazerAPI.DynamicKeyType keyType) {
        return dynamicKeys[keyType.ordinal() - 1];
    }

    public DynamicKey enableDynamicKey(final RazerAPI.DynamicKeyType type, final DynamicKeyListener listener,
                                       final String image, final String pressedImage, final boolean replace) throws RazerDynamicKeyException {
        final int index = type.ordinal() - 1;

        if (dynamicKeys[index] != null && !replace) {
            log.info("Dynamic key {} already enabled and replace is false", type);
            return dynamicKeys[index];
        }

        log.debug("Resetting dynamic key {}", type);
        disableDynamicKey(type);

        try {
            log.debug("Creating new DynamicKey object");
            final DynamicKey dk = new DynamicKey(type, image, pressedImage, listener);
            dynamicKeys[index] = dk;
        } catch (final RazerNativeException ex) {
            log.error("Failed to enable dynamic key {}: {}", type, ex.getHresult().name());
            throw new RazerDynamicKeyException(String.format("Failed to enable dynamic key %s due to a native call exception.", type), ex);
        }

        return dynamicKeys[index];
    }

    public void disableDynamicKey(final RazerAPI.DynamicKeyType type) {
        final int index = type.ordinal() - 1;
        final DynamicKey dk = dynamicKeys[index];
        if (dk != null)
            dk.disable();
        dynamicKeys[index] = null;
    }

    public void setKeyboardCapture(final boolean enabled) throws RazerNativeException {
        if (enabled == keyboardCapture)
            return;

        final RazerAPI.Hresult result = razerAPI.RzSBCaptureKeyboard(enabled);
        if (result.isError())
            throw new RazerNativeException("RzSBCaptureKeyboard", result);

        keyboardCapture = enabled;
    }

    public void addAppEventListener(final AppEventListener listener) {
        appEventListeners.add(listener);
    }

    public void removeAppEventListener(final AppEventListener listener) {
        if (appEventListeners.contains(listener))
            appEventListeners.remove(listener);
    }

    private void onAppEvent(final RazerAPI.AppEventType type, final RazerAPI.AppEventMode mode, final int processId) {
        if (appEventListeners.isEmpty())
            return;

        final AppEventEvent event = new AppEventEvent(type, mode, processId);
        for (final AppEventListener listener : appEventListeners)
            listener.appEventRaised(event);
    }

    public void addDynamicKeyListener(final DynamicKeyListener listener) {
        dynamicKeyListeners.add(listener);
    }

    public void removeDynamicKeyListener(final DynamicKeyListener listener) {
        if (dynamicKeyListeners.contains(listener))
            dynamicKeyListeners.remove(listener);
    }

    private void onDynamicKeyStateChanged(final DynamicKey dk) {
        if (dynamicKeyListeners.isEmpty())
            return;

        final DynamicKeyEvent event = new DynamicKeyEvent(dk.getKeyType(), dk.getState());
        for (final DynamicKeyListener listener : dynamicKeyListeners)
            listener.dynamicKeyStateChanged(event);
    }

    private void onDynamicKeyPressed(final DynamicKey dk) {
        if (dynamicKeyListeners.isEmpty())
            return;

        final DynamicKeyEvent event = new DynamicKeyEvent(dk.getKeyType(), dk.getState());
        for (final DynamicKeyListener listener : dynamicKeyListeners)
            listener.dynamicKeyPressed(event);
    }

    private void onDynamicKeyReleased(final DynamicKey dk) {
        if (dynamicKeyListeners.isEmpty())
            return;

        final DynamicKeyEvent event = new DynamicKeyEvent(dk.getKeyType(), dk.getState());
        for (final DynamicKeyListener listener : dynamicKeyListeners)
            listener.dynamicKeyReleased(event);
    }

    public void addKeyboardRawListener(final KeyboardRawListener listener) {
        keyboardRawListeners.add(listener);
    }

    public void removeKeyboardRawListener(final KeyboardRawListener listener) {
        if (keyboardRawListeners.contains(listener))
            keyboardRawListeners.remove(listener);
    }

    private void onKeyboardRawEvent(final int type, final int data, final int modifiers) {
        if (keyboardRawListeners.isEmpty())
            return;

        final KeyboardRawEvent event = new KeyboardRawEvent(type, data, modifiers);
        for (final KeyboardRawListener listener : keyboardRawListeners)
            listener.keyboardRawInput(event);
    }

    public void addKeyboardKeyListener(final KeyboardKeyListener listener) {
        keyboardKeyListeners.add(listener);
    }

    public void removeKeyboardKeyListener(final KeyboardKeyListener listener) {
        if (keyboardKeyListeners.contains(listener))
            keyboardKeyListeners.remove(listener);
    }

    private void onKeyboardKeyPressed(final WinAPI.VirtualKey key, final EnumSet<ModifierKeys> modifiers) {
        if (keyboardKeyListeners.isEmpty())
            return;

        final KeyboardKeyEvent event = new KeyboardKeyEvent(key, modifiers);
        for (final KeyboardKeyListener listener : keyboardKeyListeners)
            listener.keyboardKeyPressed(event);
    }

    private void onKeyboardKeyReleased(final WinAPI.VirtualKey key, final EnumSet<ModifierKeys> modifiers) {
        if (keyboardKeyListeners.isEmpty())
            return;

        final KeyboardKeyEvent event = new KeyboardKeyEvent(key, modifiers);
        for (final KeyboardKeyListener listener : keyboardKeyListeners)
            listener.keyboardKeyReleased(event);
    }

    public void addKeyboardCharListener(final KeyboardCharListener listener) {
        keyboardCharListeners.add(listener);
    }

    public void removeKeyboardCharListener(final KeyboardCharListener listener) {
        if (keyboardCharListeners.contains(listener))
            keyboardCharListeners.remove(listener);
    }

    private void onKeyboardCharTyped(final char c) {
        if (keyboardCharListeners.isEmpty())
            return;

        final KeyboardCharEvent event = new KeyboardCharEvent(c);
        for (final KeyboardCharListener listener : keyboardCharListeners)
            listener.keyboardCharTyped(event);
    }

    // App event handler
    private int appEventCallbackFunction(final int appEventType, final WinDef.UINT dwAppMode, final WinDef.UINT dwProcessID) {
        final RazerAPI.Hresult result = RazerAPI.Hresult.RZSB_OK;

        final RazerAPI.AppEventType eventType = RazerAPI.AppEventType.values()[appEventType];

        if (eventType == RazerAPI.AppEventType.INVALID || eventType == RazerAPI.AppEventType.NONE) {
            log.debug("Unsupported AppEventType: {}", appEventType);
            return result.getVal();
        }

        final RazerAPI.AppEventMode appEventMode;

        try {
            appEventMode = RazerAPI.AppEventMode.getAppEventModeFromApiValue(dwAppMode.intValue());
        } catch (final RazerInvalidAppEventModeException ex) {
            log.error("Problem parsing app event mode", ex);
            return result.getVal(); // Should we return an error value?
        }

        final int processId = dwProcessID.intValue();

        onAppEvent(eventType, appEventMode, processId);

        return result.getVal();
    }

    // Dynamic key event handler
    private int dynamicKeyCallbackFunction(final int dynamicKeyType, final int dynamicKeyState) {
        final RazerAPI.Hresult result = RazerAPI.Hresult.RZSB_OK;

        final RazerAPI.DynamicKeyType dkType = RazerAPI.DynamicKeyType.values()[dynamicKeyType];
        final RazerAPI.DynamicKeyState state = RazerAPI.DynamicKeyState.values()[dynamicKeyState];

        final int index = dkType.ordinal() - 1;
        final DynamicKey dk = dynamicKeys[index];
        if (dk == null) {
            log.debug("Key {} has not been registered by app", dkType);
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
    private int keyboardCallbackFunction(final WinDef.UINT type, final WinDef.UINT_PTR data, final WinDef.INT_PTR modifiers) {
        final RazerAPI.Hresult result = RazerAPI.Hresult.RZSB_OK;

        final char asChar = (char) data.intValue();

        final int typeVal = type.intValue();
        final int dataVal = data.intValue();
        final int modVal = modifiers.intValue();

        onKeyboardRawEvent(typeVal, dataVal, modVal);

        final WinAPI.MessageType msgType = WinAPI.MessageType.getFromIntegerValue(typeVal);

        if (msgType == WinAPI.MessageType.CHAR && !Character.isISOControl(asChar)) {
            onKeyboardCharTyped(asChar);
        } else if (msgType == WinAPI.MessageType.KEYDOWN || msgType == WinAPI.MessageType.KEYUP) {
            final WinAPI.VirtualKey key = WinAPI.VirtualKey.getKeyFromInteger(dataVal);
            final EnumSet<ModifierKeys> modKeys = EnumSet.noneOf(ModifierKeys.class);

            final WinAPI winAPI = WinAPI.INSTANCE;

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

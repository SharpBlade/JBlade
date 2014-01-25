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
import com.sun.jna.platform.win32.WinDef;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.EnumSet;

/**
 * Created on 2014-01-24.
 * @author Sharparam
 */
public class Touchpad implements RazerAPI.TouchpadGestureCallbackFunction {
    private final Logger log;
    private final RazerAPI razerAPI;

    private EnumSet<RazerAPI.GestureType> activeGestures;
    private EnumSet<RazerAPI.GestureType> activeOSGestures;

    private boolean allGesturesEnabled;
    private boolean allOSGesturesEnabled;

    private String currentImage;

    public Touchpad() throws RazerNativeException {
        log = LogManager.getLogger();

        log.debug("Initializing active gesture vars");
        activeGestures = EnumSet.noneOf(RazerAPI.GestureType.class);
        activeOSGestures = EnumSet.noneOf(RazerAPI.GestureType.class);

        log.debug("Getting Razer API instance");
        razerAPI = RazerAPI.INSTANCE;
        log.debug("Setting gesture callback");
        RazerAPI.Hresult result = razerAPI.RzSBGestureSetCallback(this);
        if (result.failed())
            throw new RazerNativeException("RzSBGestureSetCallback", result);
    }

    public enum RenderMethod {
        EVENT,
        POLLING
    }

    public String getCurrentImage() {
        return currentImage;
    }

    public void setGesture(RazerAPI.GestureType gestureType, boolean enabled) throws RazerNativeException {
        setGesture(EnumSet.of(gestureType), enabled);
    }

    public void setGesture(EnumSet<RazerAPI.GestureType> gestureTypes, boolean enabled) throws RazerNativeException {
        EnumSet<RazerAPI.GestureType> newGestures;

        if (gestureTypes.contains(RazerAPI.GestureType.ALL))
            newGestures = gestureTypes;
        else if (gestureTypes.isEmpty() || gestureTypes.equals(EnumSet.of(RazerAPI.GestureType.NONE))) {
            if (activeGestures.isEmpty() || activeGestures.equals(EnumSet.of(RazerAPI.GestureType.NONE))) {
                log.debug("Active gestures already set to none, aborting");
                return;
            }

            if (!enabled) {
                // Request to "disable no gesture"
                // Then just enable all, since that's the same
                log.debug("Requested to set none disabled, calling set all enabled instead");
                setGesture(RazerAPI.GestureType.ALL, true);
                return;
            }

            newGestures = gestureTypes;
        } else if (enabled) {
            if (activeGestures.containsAll(gestureTypes) && !(activeGestures.equals(EnumSet.of(RazerAPI.GestureType.ALL)) && !allGesturesEnabled)) {
                log.debug("Active gestures already have requested value");
                log.debug("activeGestures == {}", activeGestures);
                log.debug("allGesturesEnabled == {}", allGesturesEnabled);
                return;
            }

            newGestures = activeGestures.clone();
            newGestures.addAll(gestureTypes);
        } else {
            if (!activeGestures.containsAll(gestureTypes)) {
                log.debug("Request to disable gesture already disabled: {}", gestureTypes);
                log.debug("activeGestures == {}", activeGestures);
                return;
            }

            newGestures = activeGestures.clone();
            newGestures.removeAll(gestureTypes);
        }

        RazerAPI.Hresult result = razerAPI.RzSBEnableGesture(newGestures, enabled);
        if (result.failed())
            throw new RazerNativeException("RzSBEnableGesture", result);

        result = razerAPI.RzSBGestureSetCallback(this);
        if (result.failed())
            throw new RazerNativeException("RzSBGestureSetCallback", result);

        activeGestures = newGestures;
        allGesturesEnabled = (activeGestures.contains(RazerAPI.GestureType.ALL) ||
                              activeGestures.containsAll(EnumSet.range(RazerAPI.GestureType.PRESS, RazerAPI.GestureType.SCROLL))) &&
                             enabled;
    }

    public void enableGesture(RazerAPI.GestureType gestureType) throws RazerNativeException {
        setGesture(gestureType, true);
    }

    public void enableGestures(EnumSet<RazerAPI.GestureType> gestureTypes) throws RazerNativeException {
        setGesture(gestureTypes, true);
    }

    public void disableGesture(RazerAPI.GestureType gestureType) throws RazerNativeException {
        setGesture(gestureType, false);
    }

    public void disableGestures(EnumSet<RazerAPI.GestureType> gestureTypes) throws RazerNativeException {
        setGesture(gestureTypes, false);
    }

    public void setOSGesture(RazerAPI.GestureType gestureType, boolean enabled) throws RazerNativeException {
        setOSGesture(EnumSet.of(gestureType), enabled);
    }

    public void setOSGesture(EnumSet<RazerAPI.GestureType> gestureTypes, boolean enabled) throws RazerNativeException {
        EnumSet<RazerAPI.GestureType> newGestures;

        if (gestureTypes.contains(RazerAPI.GestureType.ALL))
            newGestures = gestureTypes;
        else if (gestureTypes.isEmpty() || gestureTypes.equals(EnumSet.of(RazerAPI.GestureType.NONE))) {
            if (activeOSGestures.isEmpty() || activeOSGestures.equals(EnumSet.of(RazerAPI.GestureType.NONE))) {
                log.debug("Active OS gestures already set to none, aborting");
                return;
            }

            if (!enabled) {
                // Request to "disable no gesture"
                // Then just enable all, since that's the same
                log.debug("Requested to set none disabled, calling set all enabled instead");
                setOSGesture(RazerAPI.GestureType.ALL, true);
                return;
            }

            newGestures = gestureTypes;
        } else if (enabled) {
            if (activeOSGestures.containsAll(gestureTypes) &&
                !(activeOSGestures.equals(EnumSet.of(RazerAPI.GestureType.ALL)) &&
                  !allOSGesturesEnabled)) {
                log.debug("Active OS gestures already have requested value");
                log.debug("activeOSGestures == {}", activeOSGestures);
                log.debug("allOSGesturesEnabled == {}", allOSGesturesEnabled);
                return;
            }

            newGestures = activeOSGestures.clone();
            newGestures.addAll(gestureTypes);
        } else {
            if (!activeOSGestures.containsAll(gestureTypes)) {
                log.debug("Request to disable gesture already disabled: {}", gestureTypes);
                log.debug("activeOSGestures == {}", activeOSGestures);
                return;
            }

            newGestures = activeOSGestures.clone();
            newGestures.removeAll(gestureTypes);
        }

        RazerAPI.Hresult result = razerAPI.RzSBEnableGesture(newGestures, enabled);
        if (result.failed())
            throw new RazerNativeException("RzSBEnableGesture", result);

        result = razerAPI.RzSBEnableOSGesture(newGestures, enabled);
        if (result.failed())
            throw new RazerNativeException("RzSBEnableOSGesture", result);

        activeOSGestures = newGestures;
        allOSGesturesEnabled = (activeOSGestures.contains(RazerAPI.GestureType.ALL) ||
                activeOSGestures.containsAll(EnumSet.range(RazerAPI.GestureType.PRESS, RazerAPI.GestureType.SCROLL))) &&
                enabled;
    }

    public void enableOSGesture(RazerAPI.GestureType gestureType) throws RazerNativeException {
        setOSGesture(gestureType, true);
    }

    public void enableOSGestures(EnumSet<RazerAPI.GestureType> gestureTypes) throws RazerNativeException {
        setOSGesture(gestureTypes, true);
    }

    public void disableOSGesture(RazerAPI.GestureType gestureType) throws RazerNativeException {
        setGesture(gestureType, false);
    }

    public void disableOSGestures(EnumSet<RazerAPI.GestureType> gestureTypes) throws RazerNativeException {
        setOSGesture(gestureTypes, false);
    }

    // TODO: Implement DrawBitmap
    public void DrawBitmap(/* TODO: Add bitmap parameter */) {
        throw new NotImplementedException();
    }

    public void setImage(String image) throws RazerNativeException {
        RazerAPI.Hresult result = razerAPI.RzSBSetImageTouchpad(image);
        if (result.failed())
            throw new RazerNativeException("RzSBSetImageTouchpad", result);

        currentImage = image;
    }

    // Touchpad gesture event handler
    @Override
    public int invoke(RazerAPI.GestureType gestureType, WinDef.UINT dwParameters,
                      WinDef.USHORT wXPos, WinDef.USHORT wYPos, WinDef.USHORT wZPos) {
        RazerAPI.Hresult result = RazerAPI.Hresult.RZSB_OK;

        // TODO: Implement OnGesture

        switch (gestureType) {
            case PRESS:
                // TODO: Implement OnPress
                break;
            case TAP:
                // TODO: Implement OnTap
                break;
            case FLICK:
                // TODO: Implement OnFlick
                break;
            case ZOOM:
                // TODO: Implement OnZoom
                break;
            case ROTATE:
                // TODO: Implement OnRotate
                break;
            case MOVE:
                // TODO: Implement OnMove
                break;
            case HOLD:
                // TODO: Implement OnHold
                break;
            case RELEASE:
                // TODO: Implement OnRelease
                break;
            case SCROLL:
                // TODO: Implement OnScroll
                break;
        }

        return result.getVal();
    }
}

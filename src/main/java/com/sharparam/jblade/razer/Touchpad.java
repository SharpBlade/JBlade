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

import com.sharparam.jblade.razer.events.*;
import com.sharparam.jblade.razer.exceptions.RazerNativeException;
import com.sharparam.jblade.razer.listeners.*;
import com.sun.jna.platform.win32.WinDef;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Created on 2014-01-24.
 * @author Sharparam
 */
public class Touchpad implements RazerAPI.TouchpadGestureCallbackFunction {
    private static Touchpad instance;

    private final Logger log;
    private final RazerAPI razerAPI;

    private static RazerAPI.TouchpadGestureCallbackFunction gestureCallback;

    private final List<GestureListener> gestureListeners;
    private final List<FlickGestureListener> flickGestureListeners;
    private final List<HoldGestureListener> holdGestureListeners;
    private final List<MoveGestureListener> moveGestureListeners;
    private final List<PressGestureListener> pressGestureListeners;
    private final List<ReleaseGestureListener> releaseGestureListeners;
    private final List<RotateGestureListener> rotateGestureListeners;
    private final List<ScrollGestureListener> scrollGestureListeners;
    private final List<TapGestureListener> tapGestureListeners;
    private final List<ZoomGestureListener> zoomGestureListeners;

    private EnumSet<RazerAPI.GestureType> activeGestures;
    private EnumSet<RazerAPI.GestureType> activeOSGestures;

    private boolean allGesturesEnabled;
    private boolean allOSGesturesEnabled;

    private String currentImage;

    private Touchpad() throws RazerNativeException {
        log = LogManager.getLogger();

        log.debug("Initializing active gesture vars");
        activeGestures = EnumSet.noneOf(RazerAPI.GestureType.class);
        activeOSGestures = EnumSet.noneOf(RazerAPI.GestureType.class);

        log.debug("Getting Razer API instance");
        razerAPI = RazerAPI.INSTANCE;
        log.debug("Setting gesture callback");
        gestureCallback = this;
        RazerAPI.Hresult result = razerAPI.RzSBGestureSetCallback(gestureCallback);
        if (result.failed())
            throw new RazerNativeException("RzSBGestureSetCallback", result);

        log.debug("Initializing gesture listener list");
        gestureListeners = new ArrayList<GestureListener>();
        log.debug("Initializing flick gesture listener list");
        flickGestureListeners = new ArrayList<FlickGestureListener>();
        log.debug("Initializing hold gesture listener list");
        holdGestureListeners = new ArrayList<HoldGestureListener>();
        log.debug("Initializing move gesture listener list");
        moveGestureListeners = new ArrayList<MoveGestureListener>();
        log.debug("Initializing press gesture listener list");
        pressGestureListeners = new ArrayList<PressGestureListener>();
        log.debug("Initializing release gesture listener list");
        releaseGestureListeners = new ArrayList<ReleaseGestureListener>();
        log.debug("Initializing rotate gesture listener list");
        rotateGestureListeners = new ArrayList<RotateGestureListener>();
        log.debug("Initializing scroll gesture listener list");
        scrollGestureListeners = new ArrayList<ScrollGestureListener>();
        log.debug("Initializing tap gesture listener list");
        tapGestureListeners = new ArrayList<TapGestureListener>();
        log.debug("Initializing zoom gesture listener list");
        zoomGestureListeners = new ArrayList<ZoomGestureListener>();
    }

    static Touchpad getInstance() throws RazerNativeException {
        if (instance == null)
            instance = new Touchpad();

        return instance;
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

        result = razerAPI.RzSBGestureSetCallback(gestureCallback);
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
        setOSGesture(gestureType, false);
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

    public void addGestureListener(GestureListener listener) {
        gestureListeners.add(listener);
    }

    public void removeGestureListener(GestureListener listener) {
        if (gestureListeners.contains(listener))
            gestureListeners.remove(listener);
    }

    private void onGesture(RazerAPI.GestureType gestureType, int parameters, short x, short y, short z) {
        if (gestureListeners.isEmpty())
            return;

        GestureEvent event = new GestureEvent(gestureType, parameters, x, y, z);
        for (GestureListener listener : gestureListeners)
            listener.gesturePerformed(event);
    }

    public void addFlickGestureListener(FlickGestureListener listener) {
        flickGestureListeners.add(listener);
    }

    public void removeFlickGestureListener(FlickGestureListener listener) {
        if (flickGestureListeners.contains(listener))
            flickGestureListeners.remove(listener);
    }

    private void onFlickGesture(int touchpointCount, RazerAPI.Direction direction) {
        if (flickGestureListeners.isEmpty())
            return;

        FlickGestureEvent event = new FlickGestureEvent(touchpointCount, direction);
        for (FlickGestureListener listener : flickGestureListeners)
            listener.flickGesturePerformed(event);
    }

    public void addHoldGestureListener(HoldGestureListener listener) {
        holdGestureListeners.add(listener);
    }

    public void removeHoldGestureListener(HoldGestureListener listener) {
        if (holdGestureListeners.contains(listener))
            holdGestureListeners.remove(listener);
    }

    private void onHoldGesture(int parameters, short x, short y, short z) {
        if (holdGestureListeners.isEmpty())
            return;

        GestureEvent event = new GestureEvent(RazerAPI.GestureType.HOLD, parameters, x, y, z);
        for (HoldGestureListener listener : holdGestureListeners)
            listener.holdGesturePerformed(event);
    }

    public void addMoveGestureListener(MoveGestureListener listener) {
        moveGestureListeners.add(listener);
    }

    public void removeMoveGestureListener(MoveGestureListener listener) {
        if (moveGestureListeners.contains(listener))
            moveGestureListeners.remove(listener);
    }

    private void onMoveGesture(short x, short y) {
        if (moveGestureListeners.isEmpty())
            return;

        MoveGestureEvent event = new MoveGestureEvent(x, y);
        for (MoveGestureListener listener : moveGestureListeners)
            listener.moveGesturePerformed(event);
    }

    public void addPressGestureListener(PressGestureListener listener) {
        pressGestureListeners.add(listener);
    }

    public void removePressGestureListener(PressGestureListener listener) {
        if (pressGestureListeners.contains(listener))
            pressGestureListeners.remove(listener);
    }

    private void onPressGesture(int touchpointCount, short x, short y) {
        if (pressGestureListeners.isEmpty())
            return;

        PressGestureEvent event = new PressGestureEvent(touchpointCount, x, y);
        for (PressGestureListener listener : pressGestureListeners)
            listener.pressGesturePerformed(event);
    }

    public void addReleaseGestureListener(ReleaseGestureListener listener) {
        releaseGestureListeners.add(listener);
    }

    public void removeReleaseGestureListener(ReleaseGestureListener listener) {
        if (releaseGestureListeners.contains(listener))
            releaseGestureListeners.remove(listener);
    }

    private void onReleaseGesture(int touchpointCount, short x, short y) {
        if (releaseGestureListeners.isEmpty())
            return;

        ReleaseGestureEvent event = new ReleaseGestureEvent(touchpointCount, x, y);
        for (ReleaseGestureListener listener : releaseGestureListeners)
            listener.releaseGesturePerformed(event);
    }

    public void addRotateGestureListener(RotateGestureListener listener) {
        rotateGestureListeners.add(listener);
    }

    public void removeRotateGestureListener(RotateGestureListener listener) {
        if (rotateGestureListeners.contains(listener))
            rotateGestureListeners.remove(listener);
    }

    private void onRotateGesture(RotateDirection direction) {
        if (rotateGestureListeners.isEmpty())
            return;

        RotateGestureEvent event = new RotateGestureEvent(direction);
        for (RotateGestureListener listener : rotateGestureListeners)
            listener.rotateGesturePerformed(event);
    }

    public void addScrollGestureListener(ScrollGestureListener listener) {
        scrollGestureListeners.add(listener);
    }

    public void removeScrollGestureListener(ScrollGestureListener listener) {
        if (scrollGestureListeners.contains(listener))
            scrollGestureListeners.remove(listener);
    }

    private void onScrollGesture(int parameters, short x, short y, short z) {
        if (scrollGestureListeners.isEmpty())
            return;

        GestureEvent event = new GestureEvent(RazerAPI.GestureType.SCROLL, parameters, x, y, z);
        for (ScrollGestureListener listener : scrollGestureListeners)
            listener.scrollGesturePerformed(event);
    }

    public void addTapGestureListener(TapGestureListener listener) {
        tapGestureListeners.add(listener);
    }

    public void removeTapGestureListener(TapGestureListener listener) {
        if (tapGestureListeners.contains(listener))
            tapGestureListeners.remove(listener);
    }

    private void onTapGesture(short x, short y) {
        if (tapGestureListeners.isEmpty())
            return;

        TapGestureEvent event = new TapGestureEvent(x, y);
        for (TapGestureListener listener : tapGestureListeners)
            listener.tapGesturePerformed(event);
    }

    public void addZoomGestureListener(ZoomGestureListener listener) {
        zoomGestureListeners.add(listener);
    }

    public void removeZoomGestureListener(ZoomGestureListener listener) {
        if (zoomGestureListeners.contains(listener))
            zoomGestureListeners.remove(listener);
    }

    private void onZoomGesture(ZoomDirection direction) {
        if (zoomGestureListeners.isEmpty())
            return;

        ZoomGestureEvent event = new ZoomGestureEvent(direction);
        for (ZoomGestureListener listener : zoomGestureListeners)
            listener.zoomGesturePerformed(event);
    }

    // Touchpad gesture event handler
    @Override
    public int callback(int gestureType, WinDef.UINT dwParameters,
                        WinDef.USHORT wXPos, WinDef.USHORT wYPos, WinDef.USHORT wZPos) {
        RazerAPI.Hresult result = RazerAPI.Hresult.RZSB_OK;

        int parameters = dwParameters.intValue();
        short x = wXPos.shortValue();
        short y = wYPos.shortValue();
        short z = wZPos.shortValue();

        RazerAPI.GestureType type = RazerAPI.GestureType.values()[gestureType];

        onGesture(type, parameters, x, y, z);

        switch (type) {
            case PRESS: // Parameter = number of touch points
                onPressGesture(parameters, x, y);
                break;
            case TAP:
                onTapGesture(x, y);
                break;
            case FLICK:
                RazerAPI.Direction flickDirection = RazerAPI.Direction.values()[z];
                onFlickGesture(parameters, flickDirection);
                break;
            case ZOOM:
                ZoomDirection zoomDirection;
                switch (parameters) {
                    case 1:
                        zoomDirection = ZoomDirection.IN;
                        break;
                    case 2:
                        zoomDirection = ZoomDirection.OUT;
                        break;
                    default:
                        zoomDirection = ZoomDirection.INVALID;
                        break;
                }
                onZoomGesture(zoomDirection);
                break;
            case ROTATE:
                RotateDirection rotateDirection;
                switch (parameters) {
                    case 1:
                        rotateDirection = RotateDirection.CLOCKWISE;
                        break;
                    case 2:
                        rotateDirection = RotateDirection.COUNTER_CLOCKWISE;
                        break;
                    default:
                        rotateDirection = RotateDirection.INVALID;
                        break;
                }
                onRotateGesture(rotateDirection);
                break;
            case MOVE:
                onMoveGesture(x, y);
                break;
            case HOLD:
                onHoldGesture(parameters, x, y, z);
                break;
            case RELEASE:
                onReleaseGesture(parameters, x, y);
                break;
            case SCROLL:
                onScrollGesture(parameters, x, y, z);
                break;
        }

        return result.getVal();
    }
}

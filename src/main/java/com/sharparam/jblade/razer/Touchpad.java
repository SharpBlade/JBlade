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
import com.sharparam.jblade.annotations.NativeCodeBinding;
import com.sharparam.jblade.razer.events.*;
import com.sharparam.jblade.razer.exceptions.RazerNativeException;
import com.sharparam.jblade.razer.listeners.*;
import com.sun.jna.platform.win32.WinDef;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Created on 2014-01-24.
 * @author Sharparam
 */
public class Touchpad {
    private static Touchpad instance;

    private final Logger log;
    private final RazerAPI razerAPI;

    private static RazerAPI.TouchpadGestureCallbackInterface gestureCallback;

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

        gestureCallback = new RazerAPI.TouchpadGestureCallbackInterface() {
            @Override
            public int callback(final int gestureType, final WinDef.UINT dwParameters, final WinDef.USHORT wXPos, final WinDef.USHORT wYPos, final WinDef.USHORT wZPos) {
                return gestureCallbackFunction(gestureType, dwParameters, wXPos, wYPos, wZPos);
            }
        };

        final RazerAPI.Hresult result = razerAPI.RzSBGestureSetCallback(gestureCallback);
        if (result.isError())
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

    @APIComponent
    public enum RenderMethod {
        EVENT,
        POLLING
    }

    @APIComponent
    public String getCurrentImage() {
        return currentImage;
    }

    @APIComponent
    public void setGesture(final RazerAPI.GestureType gestureType, final boolean enabled) throws RazerNativeException {
        setGesture(EnumSet.of(gestureType), enabled);
    }

    @APIComponent
    public void setGesture(final EnumSet<RazerAPI.GestureType> gestureTypes, final boolean enabled) throws RazerNativeException {
        final EnumSet<RazerAPI.GestureType> newGestures;

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
        if (result.isError())
            throw new RazerNativeException("RzSBEnableGesture", result);

        result = razerAPI.RzSBGestureSetCallback(gestureCallback);
        if (result.isError())
            throw new RazerNativeException("RzSBGestureSetCallback", result);

        activeGestures = newGestures;
        allGesturesEnabled = (activeGestures.contains(RazerAPI.GestureType.ALL) ||
                              activeGestures.containsAll(EnumSet.range(RazerAPI.GestureType.PRESS, RazerAPI.GestureType.SCROLL))) &&
                             enabled;
    }

    @APIComponent
    public void enableGesture(final RazerAPI.GestureType gestureType) throws RazerNativeException {
        setGesture(gestureType, true);
    }

    @APIComponent
    public void enableGestures(final EnumSet<RazerAPI.GestureType> gestureTypes) throws RazerNativeException {
        setGesture(gestureTypes, true);
    }

    @APIComponent
    public void disableGesture(final RazerAPI.GestureType gestureType) throws RazerNativeException {
        setGesture(gestureType, false);
    }

    @APIComponent
    public void disableGestures(final EnumSet<RazerAPI.GestureType> gestureTypes) throws RazerNativeException {
        setGesture(gestureTypes, false);
    }

    @APIComponent
    public void setOSGesture(final RazerAPI.GestureType gestureType, final boolean enabled) throws RazerNativeException {
        setOSGesture(EnumSet.of(gestureType), enabled);
    }

    @APIComponent
    public void setOSGesture(final EnumSet<RazerAPI.GestureType> gestureTypes, final boolean enabled) throws RazerNativeException {
        final EnumSet<RazerAPI.GestureType> newGestures;

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
        if (result.isError())
            throw new RazerNativeException("RzSBEnableGesture", result);

        result = razerAPI.RzSBEnableOSGesture(newGestures, enabled);
        if (result.isError())
            throw new RazerNativeException("RzSBEnableOSGesture", result);

        activeOSGestures = newGestures;
        allOSGesturesEnabled = (activeOSGestures.contains(RazerAPI.GestureType.ALL) ||
                activeOSGestures.containsAll(EnumSet.range(RazerAPI.GestureType.PRESS, RazerAPI.GestureType.SCROLL))) &&
                enabled;
    }

    @APIComponent
    public void enableOSGesture(final RazerAPI.GestureType gestureType) throws RazerNativeException {
        setOSGesture(gestureType, true);
    }

    @APIComponent
    public void enableOSGestures(final EnumSet<RazerAPI.GestureType> gestureTypes) throws RazerNativeException {
        setOSGesture(gestureTypes, true);
    }

    @APIComponent
    public void disableOSGesture(final RazerAPI.GestureType gestureType) throws RazerNativeException {
        setOSGesture(gestureType, false);
    }

    @APIComponent
    public void disableOSGestures(final EnumSet<RazerAPI.GestureType> gestureTypes) throws RazerNativeException {
        setOSGesture(gestureTypes, false);
    }

    @APIComponent
    public void drawImage(final BufferedImage image) {
        // TODO: Implement
        throw new NotImplementedException();
    }

    @APIComponent
    public void setImage(final String image) throws RazerNativeException {
        final RazerAPI.Hresult result = razerAPI.RzSBSetImageTouchpad(image);
        if (result.isError())
            throw new RazerNativeException("RzSBSetImageTouchpad", result);

        currentImage = image;
    }

    @APIComponent
    public void addGestureListener(final GestureListener listener) {
        gestureListeners.add(listener);
    }

    @APIComponent
    public void removeGestureListener(final GestureListener listener) {
        if (gestureListeners.contains(listener))
            gestureListeners.remove(listener);
    }

    private void onGesture(final RazerAPI.GestureType gestureType, final int parameters,
                           final short x, final short y, final short z) {
        if (gestureListeners.isEmpty())
            return;

        final GestureEvent event = new GestureEvent(gestureType, parameters, x, y, z);
        for (final GestureListener listener : gestureListeners)
            listener.gesturePerformed(event);
    }

    @APIComponent
    public void addFlickGestureListener(final FlickGestureListener listener) {
        flickGestureListeners.add(listener);
    }

    @APIComponent
    public void removeFlickGestureListener(final FlickGestureListener listener) {
        if (flickGestureListeners.contains(listener))
            flickGestureListeners.remove(listener);
    }

    private void onFlickGesture(final int touchpointCount, final RazerAPI.Direction direction) {
        if (flickGestureListeners.isEmpty())
            return;

        final FlickGestureEvent event = new FlickGestureEvent(touchpointCount, direction);
        for (final FlickGestureListener listener : flickGestureListeners)
            listener.flickGesturePerformed(event);
    }

    @APIComponent
    public void addHoldGestureListener(final HoldGestureListener listener) {
        holdGestureListeners.add(listener);
    }

    @APIComponent
    public void removeHoldGestureListener(final HoldGestureListener listener) {
        if (holdGestureListeners.contains(listener))
            holdGestureListeners.remove(listener);
    }

    private void onHoldGesture(final int parameters, final short x, final short y, final short z) {
        if (holdGestureListeners.isEmpty())
            return;

        final GestureEvent event = new GestureEvent(RazerAPI.GestureType.HOLD, parameters, x, y, z);
        for (final HoldGestureListener listener : holdGestureListeners)
            listener.holdGesturePerformed(event);
    }

    @APIComponent
    public void addMoveGestureListener(final MoveGestureListener listener) {
        moveGestureListeners.add(listener);
    }

    @APIComponent
    public void removeMoveGestureListener(final MoveGestureListener listener) {
        if (moveGestureListeners.contains(listener))
            moveGestureListeners.remove(listener);
    }

    private void onMoveGesture(final short x, final short y) {
        if (moveGestureListeners.isEmpty())
            return;

        final MoveGestureEvent event = new MoveGestureEvent(x, y);
        for (final MoveGestureListener listener : moveGestureListeners)
            listener.moveGesturePerformed(event);
    }

    @APIComponent
    public void addPressGestureListener(final PressGestureListener listener) {
        pressGestureListeners.add(listener);
    }

    @APIComponent
    public void removePressGestureListener(final PressGestureListener listener) {
        if (pressGestureListeners.contains(listener))
            pressGestureListeners.remove(listener);
    }

    private void onPressGesture(final int touchpointCount, final short x, final short y) {
        if (pressGestureListeners.isEmpty())
            return;

        final PressGestureEvent event = new PressGestureEvent(touchpointCount, x, y);
        for (final PressGestureListener listener : pressGestureListeners)
            listener.pressGesturePerformed(event);
    }

    @APIComponent
    public void addReleaseGestureListener(final ReleaseGestureListener listener) {
        releaseGestureListeners.add(listener);
    }

    @APIComponent
    public void removeReleaseGestureListener(final ReleaseGestureListener listener) {
        if (releaseGestureListeners.contains(listener))
            releaseGestureListeners.remove(listener);
    }

    private void onReleaseGesture(final int touchpointCount, final short x, final short y) {
        if (releaseGestureListeners.isEmpty())
            return;

        final ReleaseGestureEvent event = new ReleaseGestureEvent(touchpointCount, x, y);
        for (final ReleaseGestureListener listener : releaseGestureListeners)
            listener.releaseGesturePerformed(event);
    }

    @APIComponent
    public void addRotateGestureListener(final RotateGestureListener listener) {
        rotateGestureListeners.add(listener);
    }

    @APIComponent
    public void removeRotateGestureListener(final RotateGestureListener listener) {
        if (rotateGestureListeners.contains(listener))
            rotateGestureListeners.remove(listener);
    }

    private void onRotateGesture(final RotateDirection direction) {
        if (rotateGestureListeners.isEmpty())
            return;

        final RotateGestureEvent event = new RotateGestureEvent(direction);
        for (final RotateGestureListener listener : rotateGestureListeners)
            listener.rotateGesturePerformed(event);
    }

    @APIComponent
    public void addScrollGestureListener(final ScrollGestureListener listener) {
        scrollGestureListeners.add(listener);
    }

    @APIComponent
    public void removeScrollGestureListener(final ScrollGestureListener listener) {
        if (scrollGestureListeners.contains(listener))
            scrollGestureListeners.remove(listener);
    }

    private void onScrollGesture(final int parameters, final short x, final short y, final short z) {
        if (scrollGestureListeners.isEmpty())
            return;

        final GestureEvent event = new GestureEvent(RazerAPI.GestureType.SCROLL, parameters, x, y, z);
        for (final ScrollGestureListener listener : scrollGestureListeners)
            listener.scrollGesturePerformed(event);
    }

    @APIComponent
    public void addTapGestureListener(final TapGestureListener listener) {
        tapGestureListeners.add(listener);
    }

    @APIComponent
    public void removeTapGestureListener(final TapGestureListener listener) {
        if (tapGestureListeners.contains(listener))
            tapGestureListeners.remove(listener);
    }

    private void onTapGesture(final short x, final short y) {
        if (tapGestureListeners.isEmpty())
            return;

        final TapGestureEvent event = new TapGestureEvent(x, y);
        for (final TapGestureListener listener : tapGestureListeners)
            listener.tapGesturePerformed(event);
    }

    @APIComponent
    public void addZoomGestureListener(final ZoomGestureListener listener) {
        zoomGestureListeners.add(listener);
    }

    @APIComponent
    public void removeZoomGestureListener(final ZoomGestureListener listener) {
        if (zoomGestureListeners.contains(listener))
            zoomGestureListeners.remove(listener);
    }

    private void onZoomGesture(final ZoomDirection direction) {
        if (zoomGestureListeners.isEmpty())
            return;

        final ZoomGestureEvent event = new ZoomGestureEvent(direction);
        for (final ZoomGestureListener listener : zoomGestureListeners)
            listener.zoomGesturePerformed(event);
    }

    // Touchpad gesture event handler
    @NativeCodeBinding
    private int gestureCallbackFunction(final int gestureType, final WinDef.UINT dwParameters,
                        final WinDef.USHORT wXPos, final WinDef.USHORT wYPos, final WinDef.USHORT wZPos) {
        final RazerAPI.Hresult result = RazerAPI.Hresult.RZSB_OK;

        final int parameters = dwParameters.intValue();
        final short x = wXPos.shortValue();
        final short y = wYPos.shortValue();
        final short z = wZPos.shortValue();

        // TODO: Find a more efficient way to extract the gesture type

        final EnumSet<RazerAPI.GestureType> types = RazerAPI.GestureType.getFromApiValue(gestureType); //RazerAPI.GestureType.values()[gestureType];

        if (types.size() != 1) // We should ALWAYS get EXACTLY one gesture
            throw new IllegalArgumentException("gestureType did not contain exactly 1 gesture!");

        final RazerAPI.GestureType type = (RazerAPI.GestureType) types.toArray()[0];

        onGesture(type, parameters, x, y, z);

        switch (type) {
            case PRESS: // Parameter = number of touch points
                onPressGesture(parameters, x, y);
                break;
            case TAP:
                onTapGesture(x, y);
                break;
            case FLICK:
                final RazerAPI.Direction flickDirection = RazerAPI.Direction.values()[z];
                onFlickGesture(parameters, flickDirection);
                break;
            case ZOOM:
                final ZoomDirection zoomDirection;
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
                final RotateDirection rotateDirection;
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

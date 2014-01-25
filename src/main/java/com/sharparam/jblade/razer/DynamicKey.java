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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created on 2014-01-24.
 * @author Sharparam
 */
public class DynamicKey {
    private final Logger log;

    private RazerAPI.DynamicKeyState state;
    private RazerAPI.DynamicKeyState previousState;

    private String upImage;
    private String downImage;

    private RazerAPI.DynamicKeyType keyType;

    public DynamicKey(RazerAPI.DynamicKeyType keyType, String image) throws RazerNativeException {
        this(keyType, image, null);
    }

    public DynamicKey(RazerAPI.DynamicKeyType keyType, String image, String pressedImage) throws RazerNativeException {
        log = LogManager.getLogger();

        if (image == null || image.isEmpty())
            throw new IllegalArgumentException("image argument can't be null or empty");

        if (pressedImage == null || pressedImage.isEmpty()) {
            log.debug("pressedImage is null or empty, setting to value of image");
            pressedImage = image;
        }

        log.debug("Setting default states");
        state = RazerAPI.DynamicKeyState.NONE;
        previousState = RazerAPI.DynamicKeyState.NONE;
        this.upImage = image;
        this.downImage = pressedImage;
        this.keyType = keyType;

        log.debug("Setting images");
        setUpImage(upImage);
        setDownImage(downImage);
    }

    public String getUpImage() {
        return upImage;
    }

    public String getDownImage() {
        return downImage;
    }

    public boolean hasSingleImage() {
        return upImage.equals(downImage);
    }

    public RazerAPI.DynamicKeyType getKeyType() {
        return keyType;
    }

    public RazerAPI.DynamicKeyState getState() {
        return state;
    }

    public RazerAPI.DynamicKeyState getPreviousState() {
        return previousState;
    }

    public void setImage(String image) throws RazerNativeException {
        setUpImage(image);
        setDownImage(image);
    }

    public void setImages(String image, String pressedImage) throws RazerNativeException {
        setUpImage(image);
        setDownImage(pressedImage);
    }

    public void setImage(String image, RazerAPI.DynamicKeyState state) throws RazerNativeException {
        if (state != RazerAPI.DynamicKeyState.UP && state != RazerAPI.DynamicKeyState.DOWN)
            throw new IllegalArgumentException("State can only be up or down");

        log.debug("Setting {} on {} to {}", state, keyType, image);

        RazerAPI.Hresult result = RazerAPI.INSTANCE.RzSBSetImageDynamicKey(keyType, state, image);
        if (result.failed())
            throw new RazerNativeException("RzSBSetImageDynamicKey", result);

        if (state == RazerAPI.DynamicKeyState.UP)
            upImage = image;
        else
            downImage = image;
    }

    public void setUpImage(String image) throws RazerNativeException {
        setImage(image, RazerAPI.DynamicKeyState.UP);
    }

    public void setDownImage(String image) throws RazerNativeException {
        setImage(image, RazerAPI.DynamicKeyState.DOWN);
    }

    public void refresh() throws RazerNativeException {
        setImages(upImage, downImage);
    }

    public void updateState(RazerAPI.DynamicKeyState state) {
        previousState = this.state;
        this.state = state;
        if (this.state == RazerAPI.DynamicKeyState.UP &&
                (previousState == RazerAPI.DynamicKeyState.DOWN || previousState == RazerAPI.DynamicKeyState.NONE)) {
            // TODO: Implement OnPressed
        }
    }

    // TODO: Implement UpdateState and OnPressed
}

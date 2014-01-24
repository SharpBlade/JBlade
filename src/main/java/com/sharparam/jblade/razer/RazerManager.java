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

import java.io.File;
import java.io.IOException;

/**
 * Created on 2014-01-24.
 * @author Sharparam
 */
public class RazerManager {
    /**
     * The file name to use when creating the control file.
     */
    public static final String RAZER_CONTROL_FILE = "DO_NOT_TOUCH__RAZER_CONTROL_FILE";

    // TODO: Add logging

    private DynamicKey[] dynamicKeys;

    private Touchpad touchpad;

    private boolean keyboardCapture;

    /**
     * Initializes a new instance of the RazerManager class with default settings.
     * Equivalent to calling RazerManager(true, false);
     */
    public RazerManager() {
        this(true);
    }

    /**
     * Initializes a new instance of the RazerManager class with default setting for useControlFile.
     * Equivalent to calling RazerManager(..., false);
     * @param disableOSGestures If true, all OS gestures will by default be disabled on the touchpad,
     *                          making it do nothing until gestures are enabled manually.
     */
    public RazerManager(boolean disableOSGestures) {
        this(disableOSGestures, false);
    }

    /**
     * Initializes a new instance of the RazerManager class.
     * @param disableOSGestures If true, all OS gestures will by default be disabled on the touchpad,
     *                          making it do nothing until gestures are enabled manually.
     * @param useControlFile If true, creates a control file that is checked on subsequent creations of RazerManager,
     *                       initialization will fail if a control file is found and useControlFile is true.
     */
    public RazerManager(boolean disableOSGestures, boolean useControlFile) {

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
     * Creates the Razer control file.
     */
    public static void createControlFile() {
        try {
            File controlFile = new File(RAZER_CONTROL_FILE);
            boolean result = controlFile.createNewFile();
            if (result) {
                // TODO: Notify success
            } else {
                // TODO: Warn file exists
            }
        } catch (IOException ex) {
            // TODO: Throw error
        }
    }
}

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

package com.sharparam.jblade.razer.events;

import com.sharparam.jblade.annotations.APIComponent;
import com.sharparam.jblade.razer.RazerAPI;

/**
 * Created on 2014-01-26.
 *
 * @author Sharparam
 */
public class AppEventEvent {
    private final RazerAPI.AppEventType type;
    private final RazerAPI.AppEventMode mode;
    private final int processId;

    public AppEventEvent(final RazerAPI.AppEventType type, final RazerAPI.AppEventMode mode, final int processId) {
        this.type = type;
        this.mode = mode;
        this.processId = processId;
    }

    @APIComponent
    public RazerAPI.AppEventType getType() {
        return type;
    }

    @APIComponent
    public RazerAPI.AppEventMode getMode() {
        return mode;
    }

    @APIComponent
    public int getProcessId() {
        return processId;
    }
}

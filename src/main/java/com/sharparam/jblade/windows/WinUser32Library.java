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

package com.sharparam.jblade.windows;

import com.sun.jna.Library;

/**
 * Created on 2014-01-26.
 *
 * @author Sharparam
 */
public interface WinUser32Library extends Library {
    /**
     * Retrieves the status of the specified virtual key.
     * The status specifies whether the key is up, down,
     * or toggled (on, off—alternating each time the key is pressed).
     * The key status returned from this function changes as a thread reads key messages from its message queue.
     * The status does not reflect the interrupt-level state associated with the hardware.
     * Use the GetAsyncKeyState function to retrieve that information.
     * An application calls GetKeyState in response to a keyboard-input message. This function retrieves
     * the state of the key when the input message was generated.
     * To retrieve state information for all the virtual keys, use the GetKeyboardState function.
     * An application can use the virtual key code constants
     * VK_SHIFT, VK_CONTROL, and VK_MENU as values for the nVirtKey parameter.
     * This gives the status of the SHIFT, CTRL, or ALT keys without distinguishing between left and right.
     * @param keyCode A virtual key. If the desired virtual key is a letter or digit
     *                (A through Z, a through z, or 0 through 9),
     *                nVirtKey must be set to the ASCII value of that character.
     *                For other keys, it must be a virtual-key code.
     *                If a non-English keyboard layout is used,
     *                virtual keys with values in the range ASCII A through Z and 0
     *                through 9 are used to specify most of the character keys.
     *                For example, for the German keyboard layout,
     *                the virtual key of value ASCII O (0x4F) refers to the "o" key,
     *                whereas VK_OEM_1 refers to the "o with umlaut" key.
     * @return The return value specifies the status of the specified virtual key, as follows:
     *         <list type="bullet">
     *           <item>If the high-order bit is 1, the key is down; otherwise, it is up.</item>
     *           <item>
     *             If the low-order bit is 1, the key is toggled. A key, such as the CAPS LOCK key,
     *             is toggled if it is turned on. The key is off and untoggled if the low-order bit is 0.
     *             A toggle key's indicator light (if any) on the keyboard will be on when the key is toggled,
     *             and off when the key is untoggled.
     *           </item>
     *         </list>
     */
    short GetKeyState(int keyCode);
}

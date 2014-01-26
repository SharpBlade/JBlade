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

import com.sun.jna.Library;

/**
 * Interface for native RazerLibrary functions provided by the Razer SwitchBlade UI SDK.
 * Native functions from <code>RzSwitchbladeSDK2.dll</code>, all functions are <code>__cdecl</code> calls.
 * @author Sharparam
 */
public interface RazerLibrary extends Library {
    /**
     * Grants access to the Switchblade device, establishing application connections.
     * This method sets up the connections that allow an application to access the Switchblade hardware device.
     * This routine returns RZSB_OK on success, granting the calling application control of the device.
     * Subsequent calls to this routine prior to a matching {@link #RzSBStop()} call are ignored.
     * This method must be called before other Switchblade SDK routines will succeed.
     * This method must always be accompanied by an {@link #RzSBStop()}.
     * COM initialization should be called prior to calling this method.
     * If the application developer intends to use Single-Threaded Apartment model (STA) and call the SDK
     * functions within the same thread where the COM was initialized, then <code>CoInitialize()</code> should be called
     * before this method. Note that some MFC controls automatically initializes to STA.
     * If the application developer intends to call the SDK functions on different threads,
     * then the <code>CoInitializeEx()</code> should be called before this method.
     * Note: When this method is called without the COM being initialized
     * (e.g. thru calling <code>CoInitializeEx</code>)
     * the SDK initializes the COM to Multi-Threaded Apartment (MTA) model.
     * As such, callers must invoke SDK functions from an MTA thread.
     * Future SDK versions will move these calls into an isolated STA, giving application developers additional
     * freedom to use COM in any fashion.
     * However, application developers may already implement their own processing to isolate the SDK
     * initialization and calls to avoid the issues for COM in different threading models.
     * @return HRESULT code indicating success or failure.
     */
    int RzSBStart();

    /**
     * Cleans up the Switchblade device connections and releases it for other applications.
     * This method cleans up the connections made by {@link #RzSBStart()}.
     * This routine releases an application’s control of the Switchblade hardware device,
     * allowing other applications to take control.
     * Subsequent calls to this routine prior to a matching {@link #RzSBStart()} are ignored.
     * If an application terminates after calling {@link #RzSBStart()}
     * without a matching call to this method,
     * other applications may fail to acquire control of the Switchblade device.
     * In this case, manually kill the framework processes.
     */
    void RzSBStop();

    /**
     * Collects information about the SDK and the hardware supported.
     * @param capabilities A pointer to a previously allocated structure of type Capabilities.
     *                     On successful execution, this routine fills the parameters in capabilities with the
     *                     proper information about the SDK and supported hardware.
     * @return HRESULT code indicating success or failure.
     */
    int RzSBQueryCapabilities(RazerAPI.Capabilities.ByReference capabilities);

    /**
     * Controls output to the Switchblade display.
     * The application can send bitmap data buffer directly to the Switchblade track pad
     * display thru this function providing are more direct and faster way of updating the display.
     * Since the function accepts the buffer for bottom-up bitmap,
     * the application should invert the original image along its vertical axis prior to calling the function.
     * This can be done easily with <code>BitBlit</code> and <code>StretchBlt</code> APIs.
     * @param target Specifies the target location on the Switchblade display – the main display or one of the dynamic key areas.
     *               Please refer to the definition for TargetDisplay for accepted values.
     * @param bufferParams A pointer to a buffer parameter structure of type BufferParams that
     *                     must be filled with the appropriate information for the image being sent to the render buffer.
     *                     This input parameter is an RGB565 bitmap image buffer with a bottom-up orientation.
     *                     Please refer to the definition for BufferParams for further detail.
     * @return HRESULT code indicating success or failure.
     */
    int RzSBRenderBuffer(int target, RazerAPI.BufferParams.ByValue bufferParams);

    /**
     * Set images on the Switchblade UI’s Dynamic Keys.
     * Animation in GIF files are not supported.
     * @param dk DynamicKeyType indicating which key to set the image on.
     * @param state The desired dynamic key state (up, down) for the specified image. See <see cref="DynamicKeyState" /> for accepted values.
     * @param filename The image file path for the given state. This image should be 115 x 115 pixels in dimension.
     *                 Accepted file formats are BMP, GIF, JPG, and PNG.
     * @return HRESULT code indicating success or failure.
     */
    int RzSBSetImageDynamicKey(RazerAPI.DynamicKeyType dk, RazerAPI.DynamicKeyState state, String filename);

    /**
     * Places an image on the main Switchblade display.
     * Animation in GIF files are not supported.
     * @param filename File path to the image to be placed on the main Switchblade display.
     *                 This image should be 800 x 480 pixels in dimension. Accepted file formats are BMP, GIF, JPG, and PNG.
     * @return HRESULT code indicating success or failure.
     */
    int RzSBSetImageTouchpad(String filename);

    /**
     * Sets the callback function for application event callbacks.
     * @param callback Pointer to a callback function. If this argument is set to NULL, the routine clears the previously set callback function.
     * @return HRESULT code indicating success or failure.
     */
    int RzSBAppEventSetCallback(RazerAPI.AppEventCallbackFunction callback);

    /**
     * Sets the callback function for dynamic key events.
     * @param callback Pointer to a callback function. If this argument is set to NULL, the routine clears the previously set callback function.
     * @return HRESULT code indicating success or failure.
     */
    int RzSBDynamicKeySetCallback(RazerAPI.DynamicKeyCallbackFunction callback);

    /**
     * Enables or disables the keyboard capture functionality.
     * When the capture is enabled, the SDK application can receive keyboard
     * input events through the callback assigned using <see cref="RzSBKeyboardCaptureSetCallback" />.
     * The OS will not receive any keyboard input from the Switchblade device as long as the capture is active.
     * Hence, applications must release the capture when no longer in use (call <see cref="RzSBEnableGesture" /> with false as parameter).
     * The function only affects the keyboard device where the application is running. Other keyboard devices will work normally.
     * @param enable The enable state. true enables the capture while false disables it.
     * @return HRESULT code indicating success or failure.
     */
    int RzSBCaptureKeyboard(boolean enable);

    /**
     * Sets the callback function for keyboard events.
     * @param callback Pointer to a callback function. If this argument is set to NULL, the routine clears the previously set callback function.
     * @return HRESULT code indicating success or failure.
     */
    int RzSBKeyboardCaptureSetCallback(RazerAPI.KeyboardCallbackFunction callback);

    /**
     * Sets the callback function for gesture events.
     * @param callback Pointer to a callback function. If this argument is set to NULL, the routine clears the previously set callback function.
     * @return HRESULT code indicating success or failure.
     */
    int RzSBGestureSetCallback(RazerAPI.TouchpadGestureCallbackFunction callback);

    /**
     * Enables or disables gesture events.
     * In nearly all cases, gestural events are preceded by a
     * {@link RazerAPI.GestureType#PRESS} event.
     * With multiple finger gestures, the first finger contact registers as a press,
     * and the touchpad reports subsequent contacts as the appropriate compound gesture (tap, flick, zoom or rotate).
     * @param gestureType GestureType to be enabled or disabled.
     * @param enable The enable state. true enables the gesture while false disables it.
     * @return HRESULT object indicating success or failure.
     */
    int RzSBEnableGesture(int gestureType, boolean enable);

    /**
     * Enables or disables gesture event forwarding to the OS.
     * Setting the {@link RazerAPI.GestureType#PRESS} for OS gesture is equivalent to
     * {@link RazerAPI.GestureType#PRESS},
     * {@link RazerAPI.GestureType#MOVE} and
     * {@link RazerAPI.GestureType#RELEASE}.
     * @param gestureType GestureType to be enabled or disabled.
     * @param enable The enable state. true enables the gesture while false disables it.
     * @return HRESULT object indicating success or failure.
     */
    int RzSBEnableOSGesture(int gestureType, boolean enable);
}

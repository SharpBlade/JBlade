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

import com.sun.jna.Native;

/**
 * Created on 2014-01-26.
 *
 * @author Sharparam
 */
public class WinAPI {
    public static final String KERNEL32_NAME = "kernel32.dll";
    public static final String USER32_NAME = "user32.dll";

    public static final WinAPI INSTANCE = new WinAPI();

    private final WinUser32Library user32Lib;

    /**
     * Toggled key (e.g. caps lock).
     * Used with ...
     */
    public static final int KEY_TOGGLED = 0x1;

    /**
     * Pressed key.
     * Used with ...
     */
    public static final int KEY_PRESSED = 0x8000;

    /**
     * Native windows message types.
     */
    public enum MessageType
    {
        /**
         * Posted to the window with the keyboard focus when a nonsystem key is pressed.
         * A nonsystem key is a key that is pressed when the ALT key is not pressed.
         */
        KEYDOWN (0x0100),

        /**
         * Posted to the window with the keyboard focus when a nonsystem key is released.
         * A nonsystem key is a key that is pressed when the ALT key is not pressed,
         * or a keyboard key that is pressed when a window has the keyboard focus.
         */
        KEYUP (0x0101),

        /**
         * Posted to the window with the keyboard focus when a WM_KEYDOWN message is translated
         * by the TranslateMessage function.
         * The WM_CHAR message contains the character code of the key that was pressed.
         */
        CHAR (0x0102),

        /**
         * The WM_HSCROLL message is sent to a window when a scroll event occurs in the window's
         * standard horizontal scroll bar. This message is also sent to the owner of a horizontal
         * scroll bar control when a scroll event occurs in the control.
         */
        HSCROLL (0x0114),

        /**
         * The WM_VSCROLL message is sent to a window when a scroll event occurs in the window's
         * standard vertical scroll bar. This message is also sent to the owner of a vertical
         * scroll bar control when a scroll event occurs in the control.
         */
        VSCROLL (0x0115);

        private final int val;

        private MessageType(int val) {
            this.val = val;
        }

        public static MessageType getFromIntegerValue(int value) {
            for (MessageType type : MessageType.values())
                if (type.getVal() == value)
                    return type;

            throw new IllegalArgumentException("Invalid message type passed as argument");
        }

        public int getVal() {
            return val;
        }
    }

    /**
     * Virtual-key codes used by the system.
     */
    public enum VirtualKey {
        /**
         * Left mouse button.
         */
        LBUTTON (0x01),

        /**
         * Right mouse button.
         */
        RBUTTON (0x02),

        /**
         * Control-break processing.
         */
        CANCEL (0x03),

        /**
         * Middle mouse button (three-button mouse).
         */
        MBUTTON (0x04),

        /**
         * X1 mouse button.
         */
        XBUTTON1 (0x05),

        /**
         * X2 mouse button.
         */
        XBUTTON2 (0x06),

        /**
         * Undefined.
         */
        UNDEFINED1 (0x07),

        /**
         * BACKSPACE key.
         */
        BACK (0x08),

        /**
         * TAB key.
         */
        TAB (0x09),

        /**
         * Reserved.
         */
        RESERVED1 (0x0A),

        /**
         * Reserved.
         */
        RESERVED2 (0x0B),

        /**
         * CLEAR key.
         */
        CLEAR (0x0C),

        /**
         * ENTER key.
         */
        RETURN (0x0D),

        /**
         * Undefined.
         */
        UNDEFINED2 (0x0E),

        /**
         * Undefined.
         */
        UNDEFINED3 (0x0F),

        /**
         * SHIFT key.
         */
        SHIFT (0x10),

        /**
         * CTRL key.
         */
        CONTROL (0x11),

        /**
         * ALT key.
         */
        MENU (0x12),

        /**
         * PAUSE key.
         */
        PAUSE (0x13),

        /**
         * CAPS LOCK key.
         */
        CAPITAL (0x14),

        /**
         * IME Kana mode.
         */
        KANA (0x15),

        /**
         * IME Hanguel mode (maintained for compatibility; use VK_HANGUL).
         */
        HANGUEL (0x15),

        /**
         * IME Hangul mode.
         */
        HANGUL (0x15),

        /**
         * Undefined.
         */
        UNDEFINED4 (0x16),

        /**
         * IME Junja mode.
         */
        JUNJA (0x17),

        /**
         * IME final mode.
         */
        FINAL (0x18),

        /**
         * IME Hanja mode.
         */
        HANJA (0x19),

        /**
         * IME Kanji mode.
         */
        KANJI (0x19),

        /**
         * Undefined.
         */
        UNDEFINED5 (0x0A),

        /**
         * ESC key.
         */
        ESCAPE (0x1B),

        /**
         * IME convert.
         */
        CONVERT (0x1C),

        /**
         * IME nonconvert.
         */
        NONCONVERT (0x1D),

        /**
         * IME accept.
         */
        ACCEPT (0x1E),

        /**
         * IME mode change request.
         */
        MODECHANGE (0x1F),

        /**
         * SPACEBAR.
         */
        SPACE (0x20),

        /**
         * PAGE UP key.
         */
        PRIOR (0x21),

        /**
         * PAGE DOWN key.
         */
        NEXT (0x22),

        /**
         * END key.
         */
        END (0x23),

        /**
         * HOME key.
         */
        HOME (0x24),

        /**
         * LEFT ARROW key.
         */
        LEFT (0x25),

        /**
         * UP ARROW key.
         */
        UP (0x26),

        /**
         * RIGHT ARROW key.
         */
        RIGHT (0x27),

        /**
         * DOWN ARROW key.
         */
        DOWN (0x28),

        /**
         * SELECT key.
         */
        SELECT (0x29),

        /**
         * PRINT key.
         */
        PRINT (0x2A),

        /**
         * EXECUTE key.
         */
        EXECUTE (0x2B),

        /**
         * PRINT SCREEN key.
         */
        SNAPSHOT (0x2C),

        /**
         * INS key.
         */
        INSERT (0x2D),

        /**
         * DEL key.
         */
        DELETE (0x2E),

        /**
         * HELP key.
         */
        HELP (0x2F),

        /**
         * 0 key.
         */
        Zero (0x30),

        /**
         * 1 key.
         */
        One (0x31),

        /**
         * 2 key.
         */
        Two (0x32),

        /**
         * 3 key.
         */
        Three (0x33),

        /**
         * 4 key.
         */
        Four (0x34),

        /**
         * 5 key.
         */
        Five (0x35),

        /**
         * 6 key.
         */
        Six (0x36),

        /**
         * 7 key.
         */
        Seven (0x37),

        /**
         * 8 key.
         */
        Eight (0x38),

        /**
         * 9 key.
         */
        Nine (0x39),

        /**
         * Undefined.
         */
        UNDEFINED6 (0x3A),

        /**
         * Undefined.
         */
        UNDEFINED7 (0x3B),

        /**
         * Undefined.
         */
        UNDEFINED8 (0x3C),

        /**
         * Undefined.
         */
        UNDEFINED9 (0x3D),

        /**
         * Undefined.
         */
        UNDEFINED10 (0x3E),

        /**
         * Undefined.
         */
        UNDEFINED11 (0x3F),

        /**
         * Undefined.
         */
        UNDEFINED12 (0x40),

        /**
         * A key.
         */
        A (0x41),

        /**
         * B key.
         */
        B (0x42),

        /**
         * C key.
         */
        C (0x43),

        /**
         * D key.
         */
        D (0x44),

        /**
         * E key.
         */
        E (0x45),

        /**
         * F key.
         */
        F (0x46),

        /**
         * G key.
         */
        G (0x47),

        /**
         * H key.
         */
        H (0x48),

        /**
         * I key.
         */
        I (0x49),

        /**
         * J key.
         */
        J (0x4A),

        /**
         * K key.
         */
        K (0x4B),

        /**
         * L key.
         */
        L (0x4C),

        /**
         * M key.
         */
        M (0x4D),

        /**
         * N key.
         */
        N (0x4E),

        /**
         * O key.
         */
        O (0x4F),

        /**
         * P key.
         */
        P (0x50),

        /**
         * Q key.
         */
        Q (0x51),

        /**
         * R key.
         */
        R (0x52),

        /**
         * S key.
         */
        S (0x53),

        /**
         * T key.
         */
        T (0x54),

        /**
         * U key.
         */
        U (0x55),

        /**
         * V key.
         */
        V (0x56),

        /**
         * W key.
         */
        W (0x57),

        /**
         * X key.
         */
        X (0x58),

        /**
         * Y key.
         */
        Y (0x59),

        /**
         * Z key.
         */
        Z (0x5A),

        /**
         * Left Windows key (Natural keyboard) .
         */
        LWIN (0x5B),

        /**
         * Right Windows key (Natural keyboard).
         */
        RWIN (0x5C),

        /**
         * Applications key (Natural keyboard).
         */
        APPS (0x5D),

        /**
         * Reserved.
         */
        RESERVED3 (0x5E),

        /**
         * Computer Sleep key.
         */
        SLEEP (0x5F),

        /**
         * Numeric keypad 0 key.
         */
        NUMPAD0 (0x60),

        /**
         * Numeric keypad 1 key.
         */
        NUMPAD1 (0x61),

        /**
         * Numeric keypad 2 key.
         */
        NUMPAD2 (0x62),

        /**
         * Numeric keypad 3 key.
         */
        NUMPAD3 (0x63),

        /**
         * Numeric keypad 4 key.
         */
        NUMPAD4 (0x64),

        /**
         * Numeric keypad 5 key.
         */
        NUMPAD5 (0x65),

        /**
         * Numeric keypad 6 key.
         */
        NUMPAD6 (0x66),

        /**
         * Numeric keypad 7 key.
         */
        NUMPAD7 (0x67),

        /**
         * Numeric keypad 8 key.
         */
        NUMPAD8 (0x68),

        /**
         * Numeric keypad 9 key.
         */
        NUMPAD9 (0x69),

        /**
         * Multiply key.
         */
        MULTIPLY (0x6A),

        /**
         * Add key.
         */
        ADD (0x6B),

        /**
         * Separator key.
         */
        SEPARATOR (0x6C),

        /**
         * Subtract key.
         */
        SUBTRACT (0x6D),

        /**
         * Decimal key.
         */
        DECIMAL (0x6E),

        /**
         * Divide key.
         */
        DIVIDE (0x6F),

        /**
         * F1 key.
         */
        F1 (0x70),

        /**
         * F2 key.
         */
        F2 (0x71),

        /**
         * F3 key.
         */
        F3 (0x72),

        /**
         * F4 key.
         */
        F4 (0x73),

        /**
         * F5 key.
         */
        F5 (0x74),

        /**
         * F6 key.
         */
        F6 (0x75),

        /**
         * F7 key.
         */
        F7 (0x76),

        /**
         * F8 key.
         */
        F8 (0x77),

        /**
         * F9 key.
         */
        F9 (0x78),

        /**
         * F10 key.
         */
        F10 (0x79),

        /**
         * F11 key.
         */
        F11 (0x7A),

        /**
         * F12 key.
         */
        F12 (0x7B),

        /**
         * F13 key.
         */
        F13 (0x7C),

        /**
         * F14 key.
         */
        F14 (0x7D),

        /**
         * F15 key.
         */
        F15 (0x7E),

        /**
         * F16 key.
         */
        F16 (0x7F),

        /**
         * F17 key.
         */
        F17 (0x80),

        /**
         * F18 key.
         */
        F18 (0x81),

        /**
         * F19 key.
         */
        F19 (0x82),

        /**
         * F20 key.
         */
        F20 (0x83),

        /**
         * F21 key.
         */
        F21 (0x84),

        /**
         * F22 key.
         */
        F22 (0x85),

        /**
         * F23 key.
         */
        F23 (0x86),

        /**
         * F24 key.
         */
        F24 (0x87),

        /**
         * Unassigned.
         */
        UNASSIGNED1 (0x88),

        /**
         * Unassigned.
         */
        UNASSIGNED2 (0x89),

        /**
         * Unassigned.
         */
        UNASSIGNED3 (0x8A),

        /**
         * Unassigned.
         */
        UNASSIGNED4 (0x8B),

        /**
         * Unassigned.
         */
        UNASSIGNED5 (0x8C),

        /**
         * Unassigned.
         */
        UNASSIGNED6 (0x8D),

        /**
         * Unassigned.
         */
        UNASSIGNED7 (0x8E),

        /**
         * Unassigned.
         */
        UNASSIGNED8 (0x8F),

        /**
         * NUM LOCK key.
         */
        NUMLOCK (0x90),

        /**
         * SCROLL LOCK key.
         */
        SCROLL (0x91),

        /**
         * OEM Specific.
         */
        OEM1 (0x92),

        /**
         * OEM Specific.
         */
        OEM2 (0x93),

        /**
         * OEM Specific.
         */
        OEM3 (0x94),

        /**
         * OEM Specific.
         */
        OEM4 (0x95),

        /**
         * OEM Specific.
         */
        OEM5 (0x96),

        /**
         * Unassigned.
         */
        UNASSIGNED9 (0x97),

        /**
         * Unassigned.
         */
        UNASSIGNED10 (0x97),

        /**
         * Unassigned.
         */
        UNASSIGNED11 (0x97),

        /**
         * Unassigned.
         */
        UNASSIGNED12 (0x97),

        /**
         * Unassigned.
         */
        UNASSIGNED13 (0x97),

        /**
         * Unassigned.
         */
        UNASSIGNED14 (0x97),

        /**
         * Unassigned.
         */
        UNASSIGNED15 (0x97),

        /**
         * Unassigned.
         */
        UNASSIGNED16 (0x97),

        /**
         * Unassigned.
         */
        UNASSIGNED17 (0x97),

        /**
         * Left SHIFT key.
         */
        LSHIFT (0xA0),

        /**
         * Right SHIFT key.
         */
        RSHIFT (0xA1),

        /**
         * Left CONTROL key.
         */
        LCONTROL (0xA2),

        /**
         * Right CONTROL key.
         */
        RCONTROL (0xA3),

        /**
         * Left MENU key.
         */
        LMENU (0xA4),

        /**
         * Right MENU key.
         */
        RMENU (0xA5),

        /**
         * Browser Back key.
         */
        BROWSER_BACK (0xA6),

        /**
         * Browser Forward key.
         */
        BROWSER_FORWARD (0xA7),

        /**
         * Browser Refresh key.
         */
        BROWSER_REFRESH (0xA8),

        /**
         * Browser Stop key.
         */
        BROWSER_STOP (0xA9),

        /**
         * Browser Search key .
         */
        BROWSER_SEARCH (0xAA),

        /**
         * Browser Favorites key.
         */
        BROWSER_FAVORITES (0xAB),

        /**
         * Browser Start and Home key.
         */
        BROWSER_HOME (0xAC),

        /**
         * Volume Mute key.
         */
        VOLUME_MUTE (0xAD),

        /**
         * Volume Down key.
         */
        VOLUME_DOWN (0xAE),

        /**
         * Volume Up key.
         */
        VOLUME_UP (0xAF),

        /**
         * Next Track key.
         */
        MEDIA_NEXT_TRACK (0xB0),

        /**
         * Previous Track key.
         */
        MEDIA_PREV_TRACK (0xB1),

        /**
         * Stop Media key.
         */
        MEDIA_STOP (0xB2),

        /**
         * Play/Pause Media key.
         */
        MEDIA_PLAY_PAUSE (0xB3),

        /**
         * Start Mail key.
         */
        LAUNCH_MAIL (0xB4),

        /**
         * Select Media key.
         */
        LAUNCH_MEDIA_SELECT (0xB5),

        /**
         * Start Application 1 key.
         */
        LAUNCH_APP1 (0xB6),

        /**
         * Start Application 2 key.
         */
        LAUNCH_APP2 (0xB7),

        /**
         * Reserved.
         */
        RESERVED4 (0xB8),

        /**
         * Reserved.
         */
        RESERVED5 (0xB9),

        /**
         * Used for miscellaneous characters; it can vary by keyboard.
         * For the US standard keyboard, the ';:' key.
         */
        OEM_1 (0xBA),

        /**
         * For any country/region, the '+' key.
         */
        OEM_PLUS (0xBB),

        /**
         * For any country/region, the ',' key.
         */
        OEM_COMMA (0xBC),

        /**
         * For any country/region, the '-' key.
         */
        OEM_MINUS (0xBD),

        /**
         * For any country/region, the '.' key.
         */
        OEM_PERIOD (0xBE),

        /**
         * Used for miscellaneous characters; it can vary by keyboard.
         * For the US standard keyboard, the '/?' key.
         */
        OEM_2 (0xBF),

        /**
         * Used for miscellaneous characters; it can vary by keyboard.
         * For the US standard keyboard, the '`~' key.
         */
        OEM_3 (0xC0),

        /**
         * Reserved.
         */
        RESERVED6 (0xC1),

        /**
         * Reserved.
         */
        RESERVED7 (0xC2),

        /**
         * Reserved.
         */
        RESERVED8 (0xC3),

        /**
         * Reserved.
         */
        RESERVED9 (0xC4),

        /**
         * Reserved.
         */
        RESERVED12 (0xC5),

        /**
         * Reserved.
         */
        RESERVED13 (0xC6),

        /**
         * Reserved.
         */
        RESERVED14 (0xC7),

        /**
         * Reserved.
         */
        RESERVED15 (0xC8),

        /**
         * Reserved.
         */
        RESERVED16 (0xC9),

        /**
         * Reserved.
         */
        RESERVED17 (0xCA),

        /**
         * Reserved.
         */
        RESERVED18 (0xCB),

        /**
         * Reserved.
         */
        RESERVED19 (0xCC),

        /**
         * Reserved.
         */
        RESERVED20 (0xCD),

        /**
         * Reserved.
         */
        RESERVED21 (0xCE),

        /**
         * Reserved.
         */
        RESERVED22 (0xCF),

        /**
         * Reserved.
         */
        RESERVED23 (0xD1),

        /**
         * Reserved.
         */
        RESERVED24 (0xD2),

        /**
         * Reserved.
         */
        RESERVED25 (0xD3),

        /**
         * Reserved.
         */
        RESERVED26 (0xD4),

        /**
         * Reserved.
         */
        RESERVED27 (0xD5),

        /**
         * Reserved.
         */
        RESERVED28 (0xD6),

        /**
         * Reserved.
         */
        RESERVED29 (0xD7),

        /**
         * Unassigned.
         */
        UNASSIGNED18 (0xD8),

        /**
         * Unassigned.
         */
        UNASSIGNED19 (0xD9),

        /**
         * Unassigned.
         */
        UNASSIGNED20 (0xDA),

        /**
         * Used for miscellaneous characters; it can vary by keyboard.
         * For the US standard keyboard, the '[{' key.
         */
        OEM_4 (0xDB),

        /**
         * Used for miscellaneous characters; it can vary by keyboard.
         * For the US standard keyboard, the '\|' key.
         */
        OEM_5 (0xDC),

        /**
         * Used for miscellaneous characters; it can vary by keyboard.
         * For the US standard keyboard, the ']}' key.
         */
        OEM_6 (0xDD),

        /**
         * Used for miscellaneous characters; it can vary by keyboard.
         * For the US standard keyboard, the 'single-quote/double-quote' key.
         */
        OEM_7 (0xDE),

        /**
         * Used for miscellaneous characters; it can vary by keyboard.
         */
        OEM_8 (0xDF),

        /**
         * Reserved.
         */
        RESERVED30 (0xE0),

        /**
         * OEM Specific.
         */
        OEM6 (0xE1),

        /**
         * Either the angle bracket key or the backslash key on the RT 102-key keyboard.
         */
        OEM_102 (0xE2),

        /**
         * OEM Specific.
         */
        OEM7 (0xE3),

        /**
         * OEM Specific.
         */
        OEM8 (0xE4),

        /**
         * IME PROCESS key.
         */
        PROCESSKEY (0xE5),

        /**
         * OEM Specific.
         */
        OEM9 (0xE6),

        /**
         * Used to pass Unicode characters as if they were keystrokes.
         * The VK_PACKET key is the low word of a 32-bit
         * Virtual Key value used for non-keyboard input methods.
         * For more information, see Remark in KEYBDINPUT, SendInput, WM_KEYDOWN, and WM_KEYUP.
         */
        PACKET (0xE7),

        /**
         * Unassigned.
         */
        UNASSIGNED21 (0xE8),

        /**
         * OEM Specific.
         */
        OEM10 (0xE9),

        /**
         * OEM Specific.
         */
        OEM11 (0xEA),

        /**
         * OEM Specific.
         */
        OEM12 (0xEB),

        /**
         * OEM Specific.
         */
        OEM13 (0xEC),

        /**
         * OEM Specific.
         */
        OEM14 (0xED),

        /**
         * OEM Specific.
         */
        OEM15 (0xEE),

        /**
         * OEM Specific.
         */
        OEM16 (0xEF),

        /**
         * OEM Specific.
         */
        OEM17 (0xF1),

        /**
         * OEM Specific.
         */
        OEM18 (0xF2),

        /**
         * OEM Specific.
         */
        OEM19 (0xF3),

        /**
         * OEM Specific.
         */
        OEM20 (0xF4),

        /**
         * OEM Specific.
         */
        OEM21 (0xF5),

        /**
         * Attn key.
         */
        ATTN (0xF6),

        /**
         * CrSel key.
         */
        CRSEL (0xF7),

        /**
         * ExSel key.
         */
        EXSEL (0xF8),

        /**
         * Erase EOF key.
         */
        EREOF (0xF9),

        /**
         * Play key.
         */
        PLAY (0xFA),

        /**
         * Zoom key.
         */
        ZOOM (0xFB),

        /**
         * Reserved .
         */
        NONAME (0xFC),

        /**
         * PA1 key.
         */
        PA1 (0xFD),

        /**
         * Clear key.
         */
        OEM_CLEAR (0xFE);

        private final int val;

        private VirtualKey(int val) {
            this.val = val;
        }

        public static VirtualKey getKeyFromInteger(int value) {
            for (VirtualKey key : VirtualKey.values())
                if (key.getVal() == value)
                    return key;

            throw new IllegalArgumentException("Value supplied is not a valid virtual key");
        }

        public int getVal() {
            return val;
        }
    }

    private WinAPI() {
        user32Lib = (WinUser32Library) Native.loadLibrary(USER32_NAME, WinUser32Library.class);
    }

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
    public short GetKeyState(int keyCode) {
        return user32Lib.GetKeyState(keyCode);
    }
}

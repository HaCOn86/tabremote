package com.example.bluetooth

/**
 * Standard HID Report Descriptors and Key Codes for USB / Bluetooth HID.
 * Streamlined Mouse + Keyboard + Consumer Control profile (98 bytes, maximally compatible
 * across Samsung, Android, Windows, macOS, Linux hosts without tethering misdetection):
 * - Report ID 1: 5-Button Mouse (Left, Right, Middle, M4/Back, M5/Forward) + Wheel + Horizontal AC Pan
 * - Report ID 2: Full Hardware Keyboard (ANSI/ISO Turkish Q & US QWERTY, F1-F12, Numpad, Ctrl+Scroll Zoom)
 * - Report ID 3: Consumer Control (Media, Back, Home, AL Task Manager)
 */
object HidConstants {
    const val REPORT_ID_MOUSE: Byte = 1
    const val REPORT_ID_KEYBOARD: Byte = 2
    const val REPORT_ID_CONSUMER: Byte = 3

    val CLASSIC_COMBO_REPORT_DESCRIPTOR: ByteArray = byteArrayOf(
        // === 1. 5-BUTTON MOUSE + WHEEL + HORIZONTAL AC PAN (Report ID 1) ===
        0x05.toByte(), 0x01.toByte(),        // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x02.toByte(),        // USAGE (Mouse)
        0xA1.toByte(), 0x01.toByte(),        // COLLECTION (Application)
        0x85.toByte(), REPORT_ID_MOUSE,      //   REPORT_ID (1)
        0x09.toByte(), 0x01.toByte(),        //   USAGE (Pointer)
        0xA1.toByte(), 0x00.toByte(),        //   COLLECTION (Physical)
        // 5 Buttons: Left(1), Right(2), Middle(3), M4/Back(4), M5/Forward(5)
        0x05.toByte(), 0x09.toByte(),        //     USAGE_PAGE (Button)
        0x19.toByte(), 0x01.toByte(),        //     USAGE_MINIMUM (Button 1)
        0x29.toByte(), 0x05.toByte(),        //     USAGE_MAXIMUM (Button 5)
        0x15.toByte(), 0x00.toByte(),        //     LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(),        //     LOGICAL_MAXIMUM (1)
        0x95.toByte(), 0x05.toByte(),        //     REPORT_COUNT (5)
        0x75.toByte(), 0x01.toByte(),        //     REPORT_SIZE (1)
        0x81.toByte(), 0x02.toByte(),        //     INPUT (Data,Var,Abs)
        // Padding (3 bits)
        0x95.toByte(), 0x01.toByte(),        //     REPORT_COUNT (1)
        0x75.toByte(), 0x03.toByte(),        //     REPORT_SIZE (3)
        0x81.toByte(), 0x03.toByte(),        //     INPUT (Cnst,Var,Abs)
        // Movement: X, Y, Vertical Wheel (3 bytes)
        0x05.toByte(), 0x01.toByte(),        //     USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x30.toByte(),        //     USAGE (X)
        0x09.toByte(), 0x31.toByte(),        //     USAGE (Y)
        0x09.toByte(), 0x38.toByte(),        //     USAGE (Wheel)
        0x15.toByte(), 0x81.toByte(),        //     LOGICAL_MINIMUM (-127)
        0x25.toByte(), 0x7F.toByte(),        //     LOGICAL_MAXIMUM (127)
        0x75.toByte(), 0x08.toByte(),        //     REPORT_SIZE (8)
        0x95.toByte(), 0x03.toByte(),        //     REPORT_COUNT (3)
        0x81.toByte(), 0x06.toByte(),        //     INPUT (Data,Var,Rel)
        // Horizontal Scroll: AC Pan (1 byte)
        0x05.toByte(), 0x0C.toByte(),        //     USAGE_PAGE (Consumer Devices)
        0x0A.toByte(), 0x38.toByte(), 0x02.toByte(), // USAGE (AC Pan)
        0x95.toByte(), 0x01.toByte(),        //     REPORT_COUNT (1)
        0x75.toByte(), 0x08.toByte(),        //     REPORT_SIZE (8)
        0x81.toByte(), 0x06.toByte(),        //     INPUT (Data,Var,Rel)
        0xC0.toByte(),                       //   END_COLLECTION (Physical)
        0xC0.toByte(),                       // END_COLLECTION (Mouse)

        // === 2. FULL HARDWARE KEYBOARD (Report ID 2) ===
        0x05.toByte(), 0x01.toByte(),        // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x06.toByte(),        // USAGE (Keyboard)
        0xA1.toByte(), 0x01.toByte(),        // COLLECTION (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD,   //   REPORT_ID (2)
        0x05.toByte(), 0x07.toByte(),        //   USAGE_PAGE (Keyboard/Keypad)
        0x19.toByte(), 0xE0.toByte(),        //   USAGE_MINIMUM (Keyboard LeftControl)
        0x29.toByte(), 0xE7.toByte(),        //   USAGE_MAXIMUM (Keyboard Right GUI)
        0x15.toByte(), 0x00.toByte(),        //   LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(),        //   LOGICAL_MAXIMUM (1)
        0x75.toByte(), 0x01.toByte(),        //   REPORT_SIZE (1)
        0x95.toByte(), 0x08.toByte(),        //   REPORT_COUNT (8)
        0x81.toByte(), 0x02.toByte(),        //   INPUT (Data,Var,Abs)
        // Reserved byte
        0x95.toByte(), 0x01.toByte(),        //   REPORT_COUNT (1)
        0x75.toByte(), 0x08.toByte(),        //   REPORT_SIZE (8)
        0x81.toByte(), 0x03.toByte(),        //   INPUT (Cnst,Var,Abs)
        // 6-key rollover array (up to 0x73 / 115)
        0x95.toByte(), 0x06.toByte(),        //   REPORT_COUNT (6)
        0x75.toByte(), 0x08.toByte(),        //   REPORT_SIZE (8)
        0x15.toByte(), 0x00.toByte(),        //   LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x73.toByte(),        //   LOGICAL_MAXIMUM (115)
        0x05.toByte(), 0x07.toByte(),        //   USAGE_PAGE (Keyboard/Keypad)
        0x19.toByte(), 0x00.toByte(),        //   USAGE_MINIMUM (Reserved)
        0x29.toByte(), 0x73.toByte(),        //   USAGE_MAXIMUM (Keyboard F24)
        0x81.toByte(), 0x00.toByte(),        //   INPUT (Data,Ary,Abs)
        0xC0.toByte(),                       // END_COLLECTION (Keyboard)

        // === 3. CONSUMER CONTROL (Report ID 3) ===
        0x05.toByte(), 0x0C.toByte(),        // USAGE_PAGE (Consumer Devices)
        0x09.toByte(), 0x01.toByte(),        // USAGE (Consumer Control)
        0xA1.toByte(), 0x01.toByte(),        // COLLECTION (Application)
        0x85.toByte(), REPORT_ID_CONSUMER,   //   REPORT_ID (3)
        0x15.toByte(), 0x00.toByte(),        //   LOGICAL_MINIMUM (0)
        0x26.toByte(), 0xFF.toByte(), 0x03.toByte(), // LOGICAL_MAXIMUM (1023)
        0x19.toByte(), 0x00.toByte(),        //   USAGE_MINIMUM (0)
        0x2A.toByte(), 0xFF.toByte(), 0x03.toByte(), // USAGE_MAXIMUM (1023)
        0x75.toByte(), 0x10.toByte(),        //   REPORT_SIZE (16)
        0x95.toByte(), 0x01.toByte(),        //   REPORT_COUNT (1)
        0x81.toByte(), 0x00.toByte(),        //   INPUT (Data,Ary,Abs)
        0xC0.toByte()                        // END_COLLECTION (Consumer Control)
    )

    /**
     * Natural two-finger vertical scroll conversion:
     * Swiping two fingers UP (deltaY < 0) produces negative wheel/scroll ticks so the page moves DOWN.
     */
    fun twoFingerScrollDirectionSign(invertScroll: Boolean): Int =
        if (invertScroll) -1 else 1

    fun classicPinchZoomWheelTick(pinchDelta: Float): Int = when {
        pinchDelta > 0f -> 1
        pinchDelta < 0f -> -1
        else -> 0
    }

    // Keyboard Modifier bitmasks
    const val MOD_LCTRL = 1 shl 0
    const val MOD_LSHIFT = 1 shl 1
    const val MOD_LALT = 1 shl 2
    const val MOD_LGUI = 1 shl 3 // Command / Windows / Meta key
    const val MOD_RCTRL = 1 shl 4
    const val MOD_RSHIFT = 1 shl 5
    const val MOD_RALT = 1 shl 6 // AltGr
    const val MOD_RGUI = 1 shl 7

    // HID Keycodes (Usage Page 0x07)
    const val KEY_NONE = 0x00
    const val KEY_A = 0x04
    const val KEY_B = 0x05
    const val KEY_C = 0x06
    const val KEY_D = 0x07
    const val KEY_E = 0x08
    const val KEY_F = 0x09
    const val KEY_G = 0x0A
    const val KEY_H = 0x0B
    const val KEY_I = 0x0C
    const val KEY_J = 0x0D
    const val KEY_K = 0x0E
    const val KEY_L = 0x0F
    const val KEY_M = 0x10
    const val KEY_N = 0x11
    const val KEY_O = 0x12
    const val KEY_P = 0x13
    const val KEY_Q = 0x14
    const val KEY_R = 0x15
    const val KEY_S = 0x16
    const val KEY_T = 0x17
    const val KEY_U = 0x18
    const val KEY_V = 0x19
    const val KEY_W = 0x1A
    const val KEY_X = 0x1B
    const val KEY_Y = 0x1C
    const val KEY_Z = 0x1D

    const val KEY_1 = 0x1E
    const val KEY_2 = 0x1F
    const val KEY_3 = 0x20
    const val KEY_4 = 0x21
    const val KEY_5 = 0x22
    const val KEY_6 = 0x23
    const val KEY_7 = 0x24
    const val KEY_8 = 0x25
    const val KEY_9 = 0x26
    const val KEY_0 = 0x27

    const val KEY_ENTER = 0x28
    const val KEY_ESC = 0x29
    const val KEY_BACKSPACE = 0x2A
    const val KEY_TAB = 0x2B
    const val KEY_SPACE = 0x2C
    const val KEY_MINUS = 0x2D
    const val KEY_EQUAL = 0x2E
    const val KEY_LEFTBRACE = 0x2F
    const val KEY_RIGHTBRACE = 0x30
    const val KEY_BACKSLASH = 0x31
    const val KEY_SEMICOLON = 0x33
    const val KEY_APOSTROPHE = 0x34
    const val KEY_GRAVE = 0x35
    const val KEY_COMMA = 0x36
    const val KEY_DOT = 0x37
    const val KEY_SLASH = 0x38
    const val KEY_CAPSLOCK = 0x39

    const val KEY_F1 = 0x3A
    const val KEY_F2 = 0x3B
    const val KEY_F3 = 0x3C
    const val KEY_F4 = 0x3D
    const val KEY_F5 = 0x3E
    const val KEY_F6 = 0x3F
    const val KEY_F7 = 0x40
    const val KEY_F8 = 0x41
    const val KEY_F9 = 0x42
    const val KEY_F10 = 0x43
    const val KEY_F11 = 0x44
    const val KEY_F12 = 0x45

    const val KEY_SYSRQ = 0x46 // Print Screen
    const val KEY_HOME = 0x4A
    const val KEY_PAGEUP = 0x4B
    const val KEY_DELETE = 0x4C
    const val KEY_END = 0x4D
    const val KEY_PAGEDOWN = 0x4E
    const val KEY_RIGHT = 0x4F
    const val KEY_LEFT = 0x50
    const val KEY_DOWN = 0x51
    const val KEY_UP = 0x52

    // Numpad Keycodes
    const val KEY_NUMLOCK = 0x53
    const val KEY_KP_SLASH = 0x54
    const val KEY_KP_ASTERISK = 0x55
    const val KEY_KP_MINUS = 0x56
    const val KEY_KP_PLUS = 0x57
    const val KEY_KP_ENTER = 0x58
    const val KEY_KP_1 = 0x59
    const val KEY_KP_2 = 0x5A
    const val KEY_KP_3 = 0x5B
    const val KEY_KP_4 = 0x5C
    const val KEY_KP_5 = 0x5D
    const val KEY_KP_6 = 0x5E
    const val KEY_KP_7 = 0x5F
    const val KEY_KP_8 = 0x60
    const val KEY_KP_9 = 0x61
    const val KEY_KP_0 = 0x62
    const val KEY_KP_DOT = 0x63
    const val KEY_NON_US_BACKSLASH = 0x64 // ISO '< > |' next to Left Shift

    const val KEY_MENU = 0x65
    const val KEY_POWER = 0x66

    // Consumer Control Usages (16-bit)
    const val CONSUMER_POWER = 0x0030
    const val CONSUMER_MENU = 0x0040
    const val CONSUMER_MENU_PICK = 0x0041
    const val CONSUMER_MENU_UP = 0x0042
    const val CONSUMER_MENU_DOWN = 0x0043
    const val CONSUMER_MENU_LEFT = 0x0044
    const val CONSUMER_MENU_RIGHT = 0x0045
    const val CONSUMER_MEDIA_SELECT_TV = 0x0089
    const val CONSUMER_MEDIA_SELECT_GUIDE = 0x008D
    const val CONSUMER_CH_UP = 0x009C
    const val CONSUMER_CH_DOWN = 0x009D
    const val CONSUMER_FAST_FORWARD = 0x00B3
    const val CONSUMER_REWIND = 0x00B4
    const val CONSUMER_PLAY_PAUSE = 0x00CD
    const val CONSUMER_NEXT_TRACK = 0x00B5
    const val CONSUMER_PREV_TRACK = 0x00B6
    const val CONSUMER_STOP = 0x00B7
    const val CONSUMER_VOLUME_UP = 0x00E9
    const val CONSUMER_VOLUME_DOWN = 0x00EA
    const val CONSUMER_MUTE = 0x00E2
    const val CONSUMER_HOME = 0x0223
    const val CONSUMER_BACK = 0x0224
    const val CONSUMER_AC_SEARCH = 0x0221
    const val CONSUMER_AL_TASK_MANAGER = 0x01A7

    /**
     * Maps a character to (modifier, hidKeyCode) taking into account the target keyboard layout
     * (Turkish Q vs US English).
     */
    fun charToHid(c: Char, isTurkishLayout: Boolean = true): Pair<Int, Int>? {
        if (isTurkishLayout) {
            when (c) {
                ',' -> return Pair(0, KEY_BACKSLASH)
                ';' -> return Pair(MOD_LSHIFT, KEY_BACKSLASH)
                '.' -> return Pair(0, KEY_SLASH)
                ':' -> return Pair(MOD_LSHIFT, KEY_SLASH)
                'ö' -> return Pair(0, KEY_COMMA)
                'Ö' -> return Pair(MOD_LSHIFT, KEY_COMMA)
                'ç' -> return Pair(0, KEY_DOT)
                'Ç' -> return Pair(MOD_LSHIFT, KEY_DOT)
                'ş' -> return Pair(0, KEY_SEMICOLON)
                'Ş' -> return Pair(MOD_LSHIFT, KEY_SEMICOLON)
                'ğ' -> return Pair(0, KEY_LEFTBRACE)
                'Ğ' -> return Pair(MOD_LSHIFT, KEY_LEFTBRACE)
                'ü' -> return Pair(0, KEY_RIGHTBRACE)
                'Ü' -> return Pair(MOD_LSHIFT, KEY_RIGHTBRACE)
                'ı' -> return Pair(0, KEY_I)
                'I' -> return Pair(MOD_LSHIFT, KEY_I)
                'i' -> return Pair(0, KEY_APOSTROPHE)
                'İ' -> return Pair(MOD_LSHIFT, KEY_APOSTROPHE)
                '<' -> return Pair(0, KEY_NON_US_BACKSLASH)
                '>' -> return Pair(MOD_LSHIFT, KEY_NON_US_BACKSLASH)
                '/' -> return Pair(MOD_LSHIFT, KEY_7)
                '?' -> return Pair(MOD_LSHIFT, KEY_MINUS)
                else -> Unit
            }
        }

        return when (c) {
            in 'a'..'z' -> Pair(0, KEY_A + (c - 'a'))
            in 'A'..'Z' -> Pair(MOD_LSHIFT, KEY_A + (c - 'A'))
            in '1'..'9' -> Pair(0, KEY_1 + (c - '1'))
            '0' -> Pair(0, KEY_0)
            ' ' -> Pair(0, KEY_SPACE)
            '\n', '\r' -> Pair(0, KEY_ENTER)
            '\t' -> Pair(0, KEY_TAB)
            '!' -> Pair(MOD_LSHIFT, KEY_1)
            '@' -> Pair(MOD_LSHIFT, KEY_2)
            '#' -> Pair(MOD_LSHIFT, KEY_3)
            '$' -> Pair(MOD_LSHIFT, KEY_4)
            '%' -> Pair(MOD_LSHIFT, KEY_5)
            '^' -> Pair(MOD_LSHIFT, KEY_6)
            '&' -> Pair(MOD_LSHIFT, KEY_7)
            '*' -> Pair(MOD_LSHIFT, KEY_8)
            '(' -> Pair(MOD_LSHIFT, KEY_9)
            ')' -> Pair(MOD_LSHIFT, KEY_0)
            '-' -> Pair(0, KEY_MINUS)
            '_' -> Pair(MOD_LSHIFT, KEY_MINUS)
            '=' -> Pair(0, KEY_EQUAL)
            '+' -> Pair(MOD_LSHIFT, KEY_EQUAL)
            '[' -> Pair(0, KEY_LEFTBRACE)
            '{' -> Pair(MOD_LSHIFT, KEY_LEFTBRACE)
            ']' -> Pair(0, KEY_RIGHTBRACE)
            '}' -> Pair(MOD_LSHIFT, KEY_RIGHTBRACE)
            '\\' -> Pair(0, KEY_BACKSLASH)
            '|' -> Pair(MOD_LSHIFT, KEY_BACKSLASH)
            ';' -> Pair(0, KEY_SEMICOLON)
            ':' -> Pair(MOD_LSHIFT, KEY_SEMICOLON)
            '\'' -> Pair(0, KEY_APOSTROPHE)
            '"' -> Pair(MOD_LSHIFT, KEY_APOSTROPHE)
            ',' -> Pair(0, KEY_COMMA)
            '<' -> Pair(MOD_LSHIFT, KEY_COMMA)
            '.' -> Pair(0, KEY_DOT)
            '>' -> Pair(MOD_LSHIFT, KEY_DOT)
            '/' -> Pair(0, KEY_SLASH)
            '?' -> Pair(MOD_LSHIFT, KEY_SLASH)
            '`' -> Pair(0, KEY_GRAVE)
            '~' -> Pair(MOD_LSHIFT, KEY_GRAVE)
            else -> null
        }
    }
}

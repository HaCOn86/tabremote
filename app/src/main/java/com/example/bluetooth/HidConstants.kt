package com.example.bluetooth

/**
 * Standard HID Report Descriptors and Key Codes for USB / Bluetooth HID.
 * Includes:
 * - Precision Touchpad (Digitizer 0x0D / Touch Pad 0x05, 4 parallel fingers, Zoom/Scroll/3-Finger Gestures)
 * - 5-Button Mouse (Left, Right, Middle, M4/Back, M5/Forward) + Vertical Wheel + Horizontal AC Pan
 * - Full Hardware Keyboard (ANSI/ISO Turkish Q & US QWERTY, F1-F12, Numpad, Media Modifiers)
 * - Consumer Control (Media, Back, Home, AL Task Manager)
 * - Gamepad / Console Controller (16 Buttons, 8-Way Hat D-Pad, Dual Analog Sticks)
 */
object HidConstants {
    const val REPORT_ID_MOUSE: Byte = 1
    const val REPORT_ID_KEYBOARD: Byte = 2
    const val REPORT_ID_CONSUMER: Byte = 3
    const val REPORT_ID_TOUCHPAD: Byte = 4
    const val REPORT_ID_TP_CAPABILITIES: Byte = 5
    const val REPORT_ID_TP_INPUT_MODE: Byte = 6
    const val REPORT_ID_TP_SELECTIVE: Byte = 7
    const val REPORT_ID_TP_CERT: Byte = 8
    const val REPORT_ID_GAMEPAD: Byte = 9

    const val TP_LOGICAL_MAX_X = 3200
    const val TP_LOGICAL_MAX_Y = 2000
    const val TP_MAX_CONTACTS = 4

    private fun fingerCollectionBytes(): ByteArray = byteArrayOf(
        0x05.toByte(), 0x0D.toByte(),        //   USAGE_PAGE (Digitizers)
        0x09.toByte(), 0x22.toByte(),        //   USAGE (Finger)
        0xA1.toByte(), 0x02.toByte(),        //   COLLECTION (Logical)
        // Tip Switch (1 bit) + Confidence (1 bit) + Contact Identifier (6 bits) = 1 byte
        0x09.toByte(), 0x42.toByte(),        //     USAGE (Tip Switch)
        0x09.toByte(), 0x47.toByte(),        //     USAGE (Confidence)
        0x15.toByte(), 0x00.toByte(),        //     LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(),        //     LOGICAL_MAXIMUM (1)
        0x75.toByte(), 0x01.toByte(),        //     REPORT_SIZE (1)
        0x95.toByte(), 0x02.toByte(),        //     REPORT_COUNT (2)
        0x81.toByte(), 0x02.toByte(),        //     INPUT (Data,Var,Abs)
        0x09.toByte(), 0x51.toByte(),        //     USAGE (Contact Identifier)
        0x75.toByte(), 0x06.toByte(),        //     REPORT_SIZE (6)
        0x95.toByte(), 0x01.toByte(),        //     REPORT_COUNT (1)
        0x25.toByte(), 0x0F.toByte(),        //     LOGICAL_MAXIMUM (15)
        0x81.toByte(), 0x02.toByte(),        //     INPUT (Data,Var,Abs)
        // X & Y Coordinates (16-bit unsigned each, with physical size 100mm x 62.5mm)
        0xA4.toByte(),                       //     PUSH
        0x05.toByte(), 0x01.toByte(),        //     USAGE_PAGE (Generic Desktop)
        0x15.toByte(), 0x00.toByte(),        //     LOGICAL_MINIMUM (0)
        0x26.toByte(), 0x80.toByte(), 0x0C.toByte(), // LOGICAL_MAXIMUM (3200)
        0x35.toByte(), 0x00.toByte(),        //     PHYSICAL_MINIMUM (0)
        0x46.toByte(), 0xE8.toByte(), 0x03.toByte(), // PHYSICAL_MAXIMUM (1000 -> 10.00cm)
        0x55.toByte(), 0x0E.toByte(),        //     UNIT_EXPONENT (-2)
        0x65.toByte(), 0x11.toByte(),        //     UNIT (SI Lin:Distance - cm)
        0x75.toByte(), 0x10.toByte(),        //     REPORT_SIZE (16)
        0x95.toByte(), 0x01.toByte(),        //     REPORT_COUNT (1)
        0x09.toByte(), 0x30.toByte(),        //     USAGE (X)
        0x81.toByte(), 0x02.toByte(),        //     INPUT (Data,Var,Abs)
        0x26.toByte(), 0xD0.toByte(), 0x07.toByte(), // LOGICAL_MAXIMUM (2000)
        0x46.toByte(), 0x71.toByte(), 0x02.toByte(), // PHYSICAL_MAXIMUM (625 -> 6.25cm)
        0x09.toByte(), 0x31.toByte(),        //     USAGE (Y)
        0x81.toByte(), 0x02.toByte(),        //     INPUT (Data,Var,Abs)
        0xB4.toByte(),                       //     POP
        0xC0.toByte()                        //   END_COLLECTION (Logical Finger)
    )

    private val MOUSE_KEYBOARD_CONSUMER_GAMEPAD_DESCRIPTOR = byteArrayOf(
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
        0x81.toByte(), 0x01.toByte(),        //   INPUT (Cnst,Ary,Abs)
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
        0xC0.toByte(),                       // END_COLLECTION (Consumer Control)

        // === 4. GAMEPAD / CONSOLE CONTROLLER (Report ID 9) ===
        0x05.toByte(), 0x01.toByte(),        // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x05.toByte(),        // USAGE (Game Pad)
        0xA1.toByte(), 0x01.toByte(),        // COLLECTION (Application)
        0x85.toByte(), REPORT_ID_GAMEPAD,    //   REPORT_ID (9)
        0xA1.toByte(), 0x00.toByte(),        //   COLLECTION (Physical)
        // 16 Buttons (A, B, X, Y, L1, R1, L2, R2, Select, Start, Mode, L3, R3, etc.) -> 2 bytes
        0x05.toByte(), 0x09.toByte(),        //     USAGE_PAGE (Button)
        0x19.toByte(), 0x01.toByte(),        //     USAGE_MINIMUM (Button 1)
        0x29.toByte(), 0x10.toByte(),        //     USAGE_MAXIMUM (Button 16)
        0x15.toByte(), 0x00.toByte(),        //     LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(),        //     LOGICAL_MAXIMUM (1)
        0x75.toByte(), 0x01.toByte(),        //     REPORT_SIZE (1)
        0x95.toByte(), 0x10.toByte(),        //     REPORT_COUNT (16)
        0x81.toByte(), 0x02.toByte(),        //     INPUT (Data,Var,Abs)
        // Hat Switch (8-way D-Pad, 4 bits) + 4 bits padding -> 1 byte
        0x05.toByte(), 0x01.toByte(),        //     USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x39.toByte(),        //     USAGE (Hat Switch)
        0x15.toByte(), 0x00.toByte(),        //     LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x07.toByte(),        //     LOGICAL_MAXIMUM (7)
        0x35.toByte(), 0x00.toByte(),        //     PHYSICAL_MINIMUM (0)
        0x46.toByte(), 0x3B.toByte(), 0x01.toByte(), // PHYSICAL_MAXIMUM (315)
        0x65.toByte(), 0x14.toByte(),        //     UNIT (Eng Rot:Angular Pos)
        0x75.toByte(), 0x04.toByte(),        //     REPORT_SIZE (4)
        0x95.toByte(), 0x01.toByte(),        //     REPORT_COUNT (1)
        0x81.toByte(), 0x42.toByte(),        //     INPUT (Data,Var,Abs,Null)
        0x65.toByte(), 0x00.toByte(),        //     UNIT (None)
        0x75.toByte(), 0x04.toByte(),        //     REPORT_SIZE (4)
        0x95.toByte(), 0x01.toByte(),        //     REPORT_COUNT (1)
        0x81.toByte(), 0x03.toByte(),        //     INPUT (Cnst,Var,Abs)
        // Analog Axes: Left (X, Y) + Right (Z, Rz) -> 4 bytes (-127..127)
        0x09.toByte(), 0x30.toByte(),        //     USAGE (X)
        0x09.toByte(), 0x31.toByte(),        //     USAGE (Y)
        0x09.toByte(), 0x32.toByte(),        //     USAGE (Z)
        0x09.toByte(), 0x35.toByte(),        //     USAGE (Rz)
        0x15.toByte(), 0x81.toByte(),        //     LOGICAL_MINIMUM (-127)
        0x25.toByte(), 0x7F.toByte(),        //     LOGICAL_MAXIMUM (127)
        0x75.toByte(), 0x08.toByte(),        //     REPORT_SIZE (8)
        0x95.toByte(), 0x04.toByte(),        //     REPORT_COUNT (4)
        0x81.toByte(), 0x02.toByte(),        //     INPUT (Data,Var,Abs)
        0xC0.toByte(),                       //   END_COLLECTION (Physical)
        0xC0.toByte()                        // END_COLLECTION (Game Pad)
    )

    private val PRECISION_TOUCHPAD_COLLECTION = byteArrayOf(
        // === PRECISION TOUCHPAD (Report ID 4 Input, IDs 5..8 Features) ===
        0x05.toByte(), 0x0D.toByte(),        // USAGE_PAGE (Digitizers)
        0x09.toByte(), 0x05.toByte(),        // USAGE (Touch Pad)
        0xA1.toByte(), 0x01.toByte(),        // COLLECTION (Application)
        0x85.toByte(), REPORT_ID_TOUCHPAD    //   REPORT_ID (4)
    ) +
        fingerCollectionBytes() +            // Finger 0
        fingerCollectionBytes() +            // Finger 1
        fingerCollectionBytes() +            // Finger 2
        fingerCollectionBytes() +            // Finger 3
        byteArrayOf(
            // Scan Time (16-bit 100us increments) -> 2 bytes
            0x05.toByte(), 0x0D.toByte(),    //   USAGE_PAGE (Digitizers)
            0x55.toByte(), 0x0C.toByte(),    //   UNIT_EXPONENT (-4)
            0x66.toByte(), 0x01.toByte(), 0x10.toByte(), // UNIT (SI Lin:Time - Seconds)
            0x47.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), // PHYSICAL_MAX (65535)
            0x27.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), // LOGICAL_MAX (65535)
            0x75.toByte(), 0x10.toByte(),    //   REPORT_SIZE (16)
            0x95.toByte(), 0x01.toByte(),    //   REPORT_COUNT (1)
            0x09.toByte(), 0x56.toByte(),    //   USAGE (Scan Time)
            0x81.toByte(), 0x02.toByte(),    //   INPUT (Data,Var,Abs)
            // Contact Count (1 byte)
            0x55.toByte(), 0x00.toByte(),    //   UNIT_EXPONENT (0)
            0x65.toByte(), 0x00.toByte(),    //   UNIT (None)
            0x35.toByte(), 0x00.toByte(),    //   PHYSICAL_MINIMUM (0)
            0x45.toByte(), 0x00.toByte(),    //   PHYSICAL_MAXIMUM (0)
            0x15.toByte(), 0x00.toByte(),    //   LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x04.toByte(),    //   LOGICAL_MAXIMUM (4)
            0x75.toByte(), 0x08.toByte(),    //   REPORT_SIZE (8)
            0x95.toByte(), 0x01.toByte(),    //   REPORT_COUNT (1)
            0x09.toByte(), 0x54.toByte(),    //   USAGE (Contact Count)
            0x81.toByte(), 0x02.toByte(),    //   INPUT (Data,Var,Abs)
            // Clickpad Button (1 bit) + Padding (7 bits) -> 1 byte
            0x05.toByte(), 0x09.toByte(),    //   USAGE_PAGE (Button)
            0x09.toByte(), 0x01.toByte(),    //   USAGE (Button 1)
            0x15.toByte(), 0x00.toByte(),    //   LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x01.toByte(),    //   LOGICAL_MAXIMUM (1)
            0x75.toByte(), 0x01.toByte(),    //   REPORT_SIZE (1)
            0x95.toByte(), 0x01.toByte(),    //   REPORT_COUNT (1)
            0x81.toByte(), 0x02.toByte(),    //   INPUT (Data,Var,Abs)
            0x75.toByte(), 0x07.toByte(),    //   REPORT_SIZE (7)
            0x95.toByte(), 0x01.toByte(),    //   REPORT_COUNT (1)
            0x81.toByte(), 0x03.toByte(),    //   INPUT (Cnst,Var,Abs)

            // Feature Report ID 5: Capabilities (Max Contacts + Button Type)
            0x05.toByte(), 0x0D.toByte(),    //   USAGE_PAGE (Digitizers)
            0x85.toByte(), REPORT_ID_TP_CAPABILITIES, // REPORT_ID (5)
            0x09.toByte(), 0x55.toByte(),    //   USAGE (Contact Count Maximum)
            0x15.toByte(), 0x00.toByte(),    //   LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x04.toByte(),    //   LOGICAL_MAXIMUM (4)
            0x75.toByte(), 0x04.toByte(),    //   REPORT_SIZE (4)
            0x95.toByte(), 0x01.toByte(),    //   REPORT_COUNT (1)
            0xB1.toByte(), 0x02.toByte(),    //   FEATURE (Data,Var,Abs)
            0x09.toByte(), 0x59.toByte(),    //   USAGE (Button Type)
            0x15.toByte(), 0x00.toByte(),    //   LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x01.toByte(),    //   LOGICAL_MAXIMUM (1)
            0x75.toByte(), 0x04.toByte(),    //   REPORT_SIZE (4)
            0x95.toByte(), 0x01.toByte(),    //   REPORT_COUNT (1)
            0xB1.toByte(), 0x02.toByte(),    //   FEATURE (Data,Var,Abs)

            // Feature Report ID 6: Input Mode (Mouse=0, Windows PTP=3)
            0x09.toByte(), 0x0E.toByte(),    //   USAGE (Device Configuration)
            0xA1.toByte(), 0x02.toByte(),    //   COLLECTION (Logical)
            0x85.toByte(), REPORT_ID_TP_INPUT_MODE, // REPORT_ID (6)
            0x09.toByte(), 0x52.toByte(),    //     USAGE (Device Mode)
            0x15.toByte(), 0x00.toByte(),    //     LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x03.toByte(),    //     LOGICAL_MAXIMUM (3)
            0x75.toByte(), 0x08.toByte(),    //     REPORT_SIZE (8)
            0x95.toByte(), 0x01.toByte(),    //     REPORT_COUNT (1)
            0xB1.toByte(), 0x02.toByte(),    //     FEATURE (Data,Var,Abs)
            0xC0.toByte(),                   //   END_COLLECTION (Device Configuration)

            // Feature Report ID 7: Selective Reporting (Surface Switch, Button Switch)
            0x85.toByte(), REPORT_ID_TP_SELECTIVE, // REPORT_ID (7)
            0x09.toByte(), 0x57.toByte(),    //   USAGE (Surface Switch)
            0x09.toByte(), 0x58.toByte(),    //   USAGE (Button Switch)
            0x15.toByte(), 0x00.toByte(),    //   LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x01.toByte(),    //   LOGICAL_MAXIMUM (1)
            0x75.toByte(), 0x01.toByte(),    //   REPORT_SIZE (1)
            0x95.toByte(), 0x02.toByte(),    //   REPORT_COUNT (2)
            0xB1.toByte(), 0x02.toByte(),    //   FEATURE (Data,Var,Abs)
            0x75.toByte(), 0x06.toByte(),    //   REPORT_SIZE (6)
            0x95.toByte(), 0x01.toByte(),    //   REPORT_COUNT (1)
            0xB1.toByte(), 0x03.toByte(),    //   FEATURE (Cnst,Var,Abs)

            // Feature Report ID 8: PTPHQA Vendor Certification Blob (256 bytes)
            0x06.toByte(), 0x00.toByte(), 0xFF.toByte(), // USAGE_PAGE (Vendor Defined 0xFF00)
            0x09.toByte(), 0xC5.toByte(),    //   USAGE (Vendor Usage 0xC5 - PTPHQA)
            0x85.toByte(), REPORT_ID_TP_CERT,//   REPORT_ID (8)
            0x15.toByte(), 0x00.toByte(),    //   LOGICAL_MINIMUM (0)
            0x26.toByte(), 0xFF.toByte(), 0x00.toByte(), // LOGICAL_MAXIMUM (255)
            0x75.toByte(), 0x08.toByte(),    //   REPORT_SIZE (8)
            0x96.toByte(), 0x00.toByte(), 0x01.toByte(), // REPORT_COUNT (256)
            0xB1.toByte(), 0x02.toByte(),    //   FEATURE (Data,Var,Abs)

            0xC0.toByte()                    // END_COLLECTION (Touch Pad)
        )

    /** Precision Touchpad first so host OS identifies Touchpad + Mouse + Keyboard + Consumer + Gamepad */
    val PRECISION_COMBO_REPORT_DESCRIPTOR: ByteArray =
        PRECISION_TOUCHPAD_COLLECTION + MOUSE_KEYBOARD_CONSUMER_GAMEPAD_DESCRIPTOR

    /** Classic 5-Button Mouse + Keyboard + Consumer + Gamepad (No PTP digitizer collection) */
    val CLASSIC_COMBO_REPORT_DESCRIPTOR: ByteArray =
        MOUSE_KEYBOARD_CONSUMER_GAMEPAD_DESCRIPTOR

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

    // Standard Windows Precision Touchpad Certification Blob (256 bytes)
    val PTP_CERT_BLOB: ByteArray = ByteArray(256).also { arr ->
        val header = byteArrayOf(
            0xFC.toByte(), 0x28.toByte(), 0xFE.toByte(), 0x84.toByte(),
            0x40.toByte(), 0xCB.toByte(), 0x9A.toByte(), 0x87.toByte(),
            0x0D.toByte(), 0xBE.toByte(), 0x57.toByte(), 0x3C.toByte(),
            0xB6.toByte(), 0x70.toByte(), 0x09.toByte(), 0x88.toByte(),
            0x07.toByte(), 0x97.toByte(), 0x2D.toByte(), 0x2B.toByte(),
            0xE3.toByte(), 0x38.toByte(), 0x34.toByte(), 0xB6.toByte(),
            0x6C.toByte(), 0xED.toByte(), 0xB0.toByte(), 0xF7.toByte(),
            0xE5.toByte(), 0x9C.toByte(), 0xF6.toByte(), 0xC2.toByte()
        )
        header.copyInto(arr, 0)
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

    // Consumer Control Usages (16-bit)
    const val CONSUMER_PLAY_PAUSE = 0x00CD
    const val CONSUMER_NEXT_TRACK = 0x00B5
    const val CONSUMER_PREV_TRACK = 0x00B6
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

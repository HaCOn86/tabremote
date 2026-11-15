package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.bluetooth.HidConstants
import com.example.model.AppRole
import com.example.model.ButtonPosition
import com.example.model.GamepadStyle
import com.example.model.HidProfileMode
import com.example.model.MediaDockItemId
import com.example.model.MouseButton
import com.example.model.NavDockItemId
import com.example.model.RemoteSettings
import com.example.model.VolumeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TabRemote", appName)
    }

    @Test
    fun `verify v2 app version identity`() {
        assertEquals("2.0.0", BuildConfig.VERSION_NAME)
        assertEquals(2, BuildConfig.VERSION_CODE)
    }

    @Test
    fun `verify hid char mappings for Turkish Q and US English`() {
        val lowerA = HidConstants.charToHid('a', isTurkishLayout = false)
        assertNotNull(lowerA)
        assertEquals(0, lowerA?.first)
        assertEquals(HidConstants.KEY_A, lowerA?.second)

        val upperA = HidConstants.charToHid('A', isTurkishLayout = false)
        assertNotNull(upperA)
        assertEquals(HidConstants.MOD_LSHIFT, upperA?.first)
        assertEquals(HidConstants.KEY_A, upperA?.second)

        val trComma = HidConstants.charToHid(',', isTurkishLayout = true)
        assertEquals(Pair(0, HidConstants.KEY_BACKSLASH), trComma)

        val trOe = HidConstants.charToHid('ö', isTurkishLayout = true)
        assertEquals(Pair(0, HidConstants.KEY_COMMA), trOe)

        val trDot = HidConstants.charToHid('.', isTurkishLayout = true)
        assertEquals(Pair(0, HidConstants.KEY_SLASH), trDot)

        val trDotUpperI = HidConstants.charToHid('İ', isTurkishLayout = true)
        assertEquals(Pair(HidConstants.MOD_LSHIFT, HidConstants.KEY_APOSTROPHE), trDotUpperI)
    }

    @Test
    fun `verify v2 default remote settings and dock orders`() {
        val settings = RemoteSettings()
        assertEquals(AppRole.CONTROLLER, settings.appRole)
        assertTrue(settings.unifiedControlMode)
        assertEquals("TabRemote", settings.customBluetoothName)
        assertEquals(HidProfileMode.PRECISION_TOUCHPAD, settings.hidProfileMode)
        assertTrue(settings.showFunctionButtons)
        assertEquals(VolumeAction.SCROLL_DOWN, settings.touchpadVolumeDownAction)
        assertEquals(VolumeAction.SCROLL_UP, settings.touchpadVolumeUpAction)
        assertEquals(VolumeAction.MOUSE_LEFT_CLICK, settings.airMouseVolumeDownAction)
        assertEquals(VolumeAction.MOUSE_RIGHT_CLICK, settings.airMouseVolumeUpAction)
        assertEquals(ButtonPosition.BOTTOM, settings.buttonPosition)
        assertTrue(settings.showRightClick)
        assertTrue(settings.tapToClick)
        assertEquals(GamepadStyle.PLAYSTATION, settings.gamepadStyle)
        assertFalse(settings.splitPcKeyboard)
        assertEquals(1.6f, settings.pcSplitGapWeight)

        // Navigation Dock uses F1/F2 naming
        assertEquals(NavDockItemId.F1, settings.navDockOrder.first())
        assertEquals("F1", NavDockItemId.F1.defaultLabel)
        assertEquals("F2", NavDockItemId.F2.defaultLabel)

        // Media Dock defaults PLAY_PAUSE ("Oynat / Durdur") to far right
        assertEquals(MediaDockItemId.PLAY_PAUSE, settings.mediaDockOrder.last())
    }

    @Test
    fun `verify streamlined volume actions and consumer search usage`() {
        assertEquals(7, VolumeAction.entries.size)
        assertEquals(0x0221, HidConstants.CONSUMER_AC_SEARCH)
    }

    @Test
    fun `verify 5-button mouse bitmasks for M4 F1 and M5 F2`() {
        assertEquals(1, MouseButton.LEFT.mask)
        assertEquals(2, MouseButton.RIGHT.mask)
        assertEquals(4, MouseButton.MIDDLE.mask)
        assertEquals(8, MouseButton.BUTTON_4.mask)
        assertEquals(16, MouseButton.BUTTON_5.mask)
    }

    @Test
    fun `verify HID descriptor contains Precision Touchpad and Gamepad collections`() {
        assertTrue(HidConstants.PRECISION_COMBO_REPORT_DESCRIPTOR.size > HidConstants.CLASSIC_COMBO_REPORT_DESCRIPTOR.size)
        assertEquals(256, HidConstants.PTP_CERT_BLOB.size)
        assertEquals(4.toByte(), HidConstants.REPORT_ID_TOUCHPAD)
        assertEquals(9.toByte(), HidConstants.REPORT_ID_GAMEPAD)
    }
}

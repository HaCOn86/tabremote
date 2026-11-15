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
    fun `verify HID descriptor contains Precision Touchpad first and Gamepad collections`() {
        assertTrue(HidConstants.PRECISION_COMBO_REPORT_DESCRIPTOR.size > HidConstants.CLASSIC_COMBO_REPORT_DESCRIPTOR.size)
        // Must lead with Digitizers (0x05, 0x0D) / Touch Pad (0x09, 0x05) so hosts identify it as a Touchpad
        assertEquals(0x05.toByte(), HidConstants.PRECISION_COMBO_REPORT_DESCRIPTOR[0])
        assertEquals(0x0D.toByte(), HidConstants.PRECISION_COMBO_REPORT_DESCRIPTOR[1])
        assertEquals(0x09.toByte(), HidConstants.PRECISION_COMBO_REPORT_DESCRIPTOR[2])
        assertEquals(0x05.toByte(), HidConstants.PRECISION_COMBO_REPORT_DESCRIPTOR[3])
        assertEquals(256, HidConstants.PTP_CERT_BLOB.size)
        assertEquals(4.toByte(), HidConstants.REPORT_ID_TOUCHPAD)
        assertEquals(9.toByte(), HidConstants.REPORT_ID_GAMEPAD)
    }

    @Test
    fun `verify natural two-finger scroll direction and classic pinch zoom wheel ticks`() {
        assertEquals(1, HidConstants.twoFingerScrollDirectionSign(invertScroll = false))
        assertEquals(-1, HidConstants.twoFingerScrollDirectionSign(invertScroll = true))

        // Spreading 2 fingers apart zooms in (+1 wheel tick while Ctrl held)
        assertEquals(1, HidConstants.classicPinchZoomWheelTick(15f))
        // Pinching 2 fingers together zooms out (-1 wheel tick while Ctrl held)
        assertEquals(-1, HidConstants.classicPinchZoomWheelTick(-15f))
        assertEquals(0, HidConstants.classicPinchZoomWheelTick(0f))
    }

    @Test
    fun `verify 6-column QuickToolsLayoutEngine packs 4x4 D-Pad and 2x1 and 1x1 without overlap`() {
        val (placed, rowCount) = com.example.model.QuickToolsLayoutEngine.pack(
            tools = com.example.model.QuickToolId.defaultActiveGrid,
            columns = 6
        )
        assertEquals(com.example.model.QuickToolId.defaultActiveGrid.size, placed.size)
        assertEquals(6, rowCount)

        val grid = Array(rowCount) { BooleanArray(6) }
        for (item in placed) {
            assertTrue(item.col >= 0 && item.col + item.colSpan <= 6)
            assertTrue(item.row >= 0 && item.row + item.rowSpan <= rowCount)
            for (r in item.row until item.row + item.rowSpan) {
                for (c in item.col until item.col + item.colSpan) {
                    assertFalse("Overlap at row=$r col=$c for ${item.tool}", grid[r][c])
                    grid[r][c] = true
                }
            }
        }
        val dpad = placed.first { it.tool == com.example.model.QuickToolId.DPAD }
        assertEquals(4, dpad.colSpan)
        assertEquals(4, dpad.rowSpan)
    }

    @Test
    fun `verify SettingsPrefsStore persists AuthSessionMode QuickTools and liveTypingMode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = com.example.data.SettingsPrefsStore(context)

        store.setAuthSessionMode(com.example.model.AuthSessionMode.BLUETOOTH_ONLY)
        assertEquals(com.example.model.AuthSessionMode.BLUETOOTH_ONLY, store.getAuthSessionMode())

        store.setAuthSessionMode(com.example.model.AuthSessionMode.GUEST_LOCAL)
        assertEquals(com.example.model.AuthSessionMode.GUEST_LOCAL, store.getAuthSessionMode())

        val customActive = listOf(com.example.model.QuickToolId.DPAD, com.example.model.QuickToolId.ENTER)
        val customPool = listOf(com.example.model.QuickToolId.SPACE, com.example.model.QuickToolId.TR_CCEDIL)
        store.saveSettings(
            RemoteSettings(
                quickToolsActiveOrder = customActive,
                quickToolsPoolOrder = customPool,
                liveTypingMode = false
            )
        )
        val reloaded = store.loadSettings()
        assertEquals(customActive, reloaded.quickToolsActiveOrder)
        assertTrue(reloaded.quickToolsPoolOrder.containsAll(customPool))
        assertFalse(reloaded.liveTypingMode)
    }
}


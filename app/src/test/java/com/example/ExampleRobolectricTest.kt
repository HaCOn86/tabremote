package com.example

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.test.core.app.ApplicationProvider
import com.example.accessibility.RemoteAccessibilityService
import com.example.bluetooth.HidConstants
import com.example.data.SettingsPrefsStore
import com.example.model.AppRole
import com.example.model.AuthSessionMode
import com.example.model.ButtonPosition
import com.example.model.CursorContrastMode
import com.example.model.GamepadLayoutOffsets
import com.example.model.GamepadStyle
import com.example.model.HorizontalPanPosition
import com.example.model.InputSubMode
import com.example.model.MediaDockItemId
import com.example.model.MouseButton
import com.example.model.NavDockItemId
import com.example.model.QuickToolId
import com.example.model.QuickToolPlacement
import com.example.model.QuickToolsLayoutEngine
import com.example.model.RemoteSettings
import com.example.model.SparseCell
import com.example.model.SparseGridEngine
import com.example.model.ThemePreset
import com.example.model.VolumeAction
import com.example.model.WifiQuickWidgetId
import com.example.model.WifiWidgetPlacement
import com.example.viewmodel.NavTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
    fun `verify v30 app version identity`() {
        assertEquals("3.0", BuildConfig.VERSION_NAME)
        assertEquals(5, BuildConfig.VERSION_CODE)
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
    fun `verify v25 default remote settings and dock orders`() {
        val settings = RemoteSettings()
        assertEquals(AppRole.CONTROLLER, settings.appRole)
        assertEquals(GamepadLayoutOffsets(), settings.gamepadOffsets)
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
        assertEquals(ThemePreset.RUBY, settings.themePreset)
        assertFalse(settings.useDynamicColor)
        assertEquals(1.0f, settings.cursorScale)

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
    fun `verify HID descriptor contains Classic Combo Mouse Keyboard and Consumer collections`() {
        assertTrue(HidConstants.CLASSIC_COMBO_REPORT_DESCRIPTOR.size > 70)
        // Must lead with Generic Desktop (0x05, 0x01) / Mouse (0x09, 0x02) for universal HID host compatibility
        assertEquals(0x05.toByte(), HidConstants.CLASSIC_COMBO_REPORT_DESCRIPTOR[0])
        assertEquals(0x01.toByte(), HidConstants.CLASSIC_COMBO_REPORT_DESCRIPTOR[1])
        assertEquals(0x09.toByte(), HidConstants.CLASSIC_COMBO_REPORT_DESCRIPTOR[2])
        assertEquals(0x02.toByte(), HidConstants.CLASSIC_COMBO_REPORT_DESCRIPTOR[3])
        assertEquals(1.toByte(), HidConstants.REPORT_ID_MOUSE)
        assertEquals(2.toByte(), HidConstants.REPORT_ID_KEYBOARD)
        assertEquals(3.toByte(), HidConstants.REPORT_ID_CONSUMER)
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
    fun `verify 6-column QuickToolsLayoutEngine packs 3x2 D-Pad all Turkish letters and controls without overlap`() {
        val (placed, rowCount) = QuickToolsLayoutEngine.pack(
            tools = QuickToolId.defaultActiveGrid,
            columns = 6
        )
        assertEquals(QuickToolId.defaultActiveGrid.size, placed.size)
        assertEquals(5, rowCount)
        assertTrue(QuickToolId.TR_CAPITAL_I in QuickToolId.defaultActiveGrid)
        assertTrue(QuickToolId.ESC in QuickToolId.defaultActiveGrid)
        assertTrue(QuickToolId.HOME_KEY in QuickToolId.defaultPool)
        assertTrue(QuickToolId.NEW_TAB in QuickToolId.defaultPool)

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
        val dpad = placed.first { it.tool == QuickToolId.DPAD }
        assertEquals(3, dpad.colSpan)
        assertEquals(2, dpad.rowSpan)

        val space = placed.first { it.tool == QuickToolId.SPACE }
        assertEquals(2, space.colSpan)
    }

    @Test
    fun `verify SettingsPrefsStore persists AuthSessionMode QuickTools ThemePreset Cursor and GamepadOffsets`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsPrefsStore(context)

        store.setAuthSessionMode(AuthSessionMode.BLUETOOTH_ONLY)
        assertEquals(AuthSessionMode.BLUETOOTH_ONLY, store.getAuthSessionMode())

        store.setAuthSessionMode(AuthSessionMode.GUEST_LOCAL)
        assertEquals(AuthSessionMode.GUEST_LOCAL, store.getAuthSessionMode())

        val customActive = listOf(QuickToolId.DPAD, QuickToolId.ENTER)
        val customPool = listOf(QuickToolId.SPACE, QuickToolId.TR_CCEDIL, QuickToolId.ESC)
        val customColor = 0xFF4BE3B0.toInt()
        val customOffsets = GamepadLayoutOffsets(
            dpadX = 0.12f,
            dpadY = -0.08f,
            leftStickX = 0.04f,
            leftStickY = 0.16f,
            faceButtonsX = -0.20f,
            faceButtonsY = 0.05f,
            rightStickX = -0.06f,
            rightStickY = -0.12f,
            centerPadX = 0.03f,
            centerPadY = 0.09f
        )
        val customPlacements = listOf(
            QuickToolPlacement(QuickToolId.DPAD, col = 0, row = 0, colSpan = 3, rowSpan = 2),
            QuickToolPlacement(QuickToolId.ENTER, col = 4, row = 1, colSpan = 2, rowSpan = 1)
        )
        val customWifiWidgets = listOf(
            WifiWidgetPlacement(WifiQuickWidgetId.PLAY_PAUSE, col = 0, row = 0, colSpan = 2, rowSpan = 1),
            WifiWidgetPlacement(WifiQuickWidgetId.SCREENSHOT, col = 3, row = 1, colSpan = 2, rowSpan = 1)
        )
        store.saveSettings(
            RemoteSettings(
                quickToolsActiveOrder = customActive,
                quickToolsPoolOrder = customPool,
                quickToolsPlacements = customPlacements,
                wifiControlPlacements = customWifiWidgets,
                liveTypingMode = false,
                themePreset = ThemePreset.EMERALD,
                useDynamicColor = true,
                cursorScale = 1.6f,
                cursorColorArgb = customColor,
                cursorBorderColorArgb = 0xFF101218.toInt(),
                cursorContrastMode = CursorContrastMode.GRADUAL_ADAPTIVE,
                gamepadOffsets = customOffsets,
                horizontalPanPosition = HorizontalPanPosition.BELOW_BUTTONS,
                buttonPosition = ButtonPosition.TOP
            )
        )
        val reloaded = store.loadSettings()
        assertEquals(customActive, reloaded.quickToolsActiveOrder)
        assertTrue(reloaded.quickToolsPoolOrder.containsAll(customPool))
        assertEquals(customPlacements, reloaded.quickToolsPlacements)
        assertEquals(customWifiWidgets, reloaded.wifiControlPlacements)
        assertFalse(reloaded.liveTypingMode)
        assertEquals(ThemePreset.EMERALD, reloaded.themePreset)
        assertTrue(reloaded.useDynamicColor)
        assertEquals(1.6f, reloaded.cursorScale, 0.01f)
        assertEquals(customColor, reloaded.cursorColorArgb)
        assertEquals(0xFF101218.toInt(), reloaded.cursorBorderColorArgb)
        assertEquals(CursorContrastMode.GRADUAL_ADAPTIVE, reloaded.cursorContrastMode)
        assertEquals(customOffsets, reloaded.gamepadOffsets)
        assertEquals(HorizontalPanPosition.BELOW_BUTTONS, reloaded.horizontalPanPosition)
        assertEquals(ButtonPosition.BOTTOM, reloaded.buttonPosition)
    }

    @Test
    fun `verify SparseGridEngine preserves empty gaps and pushes colliding tiles to nearest open slot`() {
        val initial = listOf(
            SparseCell(QuickToolId.ESC, col = 0, row = 0, colSpan = 1, rowSpan = 1),
            SparseCell(QuickToolId.ENTER, col = 3, row = 0, colSpan = 2, rowSpan = 1)
        )
        // Drop TAB at col=3,row=0 onto ENTER -> ENTER should slide cleanly without compacting gap at col=1..2
        val updated = SparseGridEngine.placeWithPush(
            cells = initial + SparseCell(QuickToolId.TAB, col = 5, row = 0, colSpan = 1, rowSpan = 1),
            targetId = QuickToolId.TAB,
            targetCol = 3,
            targetRow = 0,
            targetColSpan = 1,
            targetRowSpan = 1
        )
        val esc = updated.first { it.id == QuickToolId.ESC }
        val tab = updated.first { it.id == QuickToolId.TAB }
        val enter = updated.first { it.id == QuickToolId.ENTER }

        assertEquals(0, esc.col)
        assertEquals(0, esc.row)
        assertEquals(3, tab.col)
        assertEquals(0, tab.row)
        // ENTER was colliding with (3,0), so it must not overlap (3,0) or (0,0)
        assertFalse(enter.row == 0 && enter.col == 3)
        assertFalse(enter.row == 0 && enter.col == 0)
    }

    @Test
    fun `verify QuickToolId nextSpan toggles 1x1 and 2x1 and keeps Turkish and punctuation fixed 1x1`() {
        assertEquals(2 to 1, QuickToolId.ESC.nextSpan(1, 1))
        assertEquals(1 to 1, QuickToolId.ESC.nextSpan(2, 1))
        assertEquals(1 to 1, QuickToolId.SPACE.nextSpan(2, 1))
        assertEquals(2 to 1, QuickToolId.SPACE.nextSpan(1, 1))
        assertEquals(3 to 2, QuickToolId.DPAD.nextSpan(3, 2))

        assertEquals(listOf(1 to 1), QuickToolId.TR_CCEDIL.allowedSpans())
        assertEquals(1 to 1, QuickToolId.TR_CCEDIL.nextSpan(1, 1))
        assertEquals(listOf(1 to 1), QuickToolId.PUNCT_COMMA.allowedSpans())
        assertEquals(1 to 1, QuickToolId.PUNCT_DOT.nextSpan(1, 1))
    }

    @Test
    fun `verify 4-tab NavTab and 4 InputSubMode entries`() {
        assertEquals(
            listOf(NavTab.TOUCHPAD, NavTab.KEYBOARD, NavTab.CONTROL_HUB, NavTab.SETTINGS),
            NavTab.entries
        )
        assertEquals(
            listOf(
                InputSubMode.HUB_HOME,
                InputSubMode.PC_KEYBOARD,
                InputSubMode.TV_REMOTE,
                InputSubMode.GAMEPAD
            ),
            InputSubMode.entries
        )
    }

    @Test
    fun `verify cursor contrast modes resolve manual, instant BW, and gradual adaptive colors`() {
        val fill = 0xFF33AAFF.toInt()
        val border = AndroidColor.BLACK

        assertEquals(
            fill to border,
            RemoteAccessibilityService.resolveEffectiveCursorColors(
                fill, border, CursorContrastMode.MANUAL, backgroundLuminance = 0.92f
            )
        )

        val instantOnWhite = RemoteAccessibilityService.resolveEffectiveCursorColors(
            fill, border, CursorContrastMode.INSTANT_BW, backgroundLuminance = 0.9f
        )
        val instantOnBlack = RemoteAccessibilityService.resolveEffectiveCursorColors(
            fill, border, CursorContrastMode.INSTANT_BW, backgroundLuminance = 0.1f
        )
        assertNotEquals(instantOnWhite.first, instantOnBlack.first)
        assertEquals(AndroidColor.WHITE, instantOnWhite.second)
        assertEquals(AndroidColor.WHITE, instantOnBlack.first)

        val midGradual = RemoteAccessibilityService.resolveEffectiveCursorColors(
            fill, border, CursorContrastMode.GRADUAL_ADAPTIVE, backgroundLuminance = 0.5f
        )
        assertNotEquals(midGradual.first, AndroidColor.WHITE)
        assertNotEquals(midGradual.first, 0xFF14161B.toInt())
    }
}

package com.example.data

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import com.example.model.AirMouseScrollMode
import com.example.model.AppRole
import com.example.model.AuthSessionMode
import com.example.model.ButtonPosition
import com.example.model.CursorContrastMode
import com.example.model.GamepadLayoutOffsets
import com.example.model.GamepadStyle
import com.example.model.HorizontalPanPosition
import com.example.model.MediaDockItemId
import com.example.model.NavDockItemId
import com.example.model.QuickToolId
import com.example.model.QuickToolPlacement
import com.example.model.QuickToolsLayoutEngine
import com.example.model.RemoteSettings
import com.example.model.ScrollSide
import com.example.model.SparseCell
import com.example.model.SparseGridEngine
import com.example.model.SparseGridNode
import com.example.model.ThemePreset
import com.example.model.VolumeAction
import com.example.model.WifiQuickWidgetId
import com.example.model.WifiWidgetPlacement
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SettingsPrefsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("tabremote_v2_prefs", Context.MODE_PRIVATE)

    fun getOrCreateDeviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val created = "dev_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        prefs.edit { putString(KEY_DEVICE_ID, created) }
        return created
    }

    fun getOrCreateSessionToken(): String {
        val existing = prefs.getString(KEY_SESSION_TOKEN, null)
        if (!existing.isNullOrBlank() && existing.length >= 12) return existing
        val created = "tr_${UUID.randomUUID().toString().replace("-", "")}"
        prefs.edit { putString(KEY_SESSION_TOKEN, created) }
        return created
    }

    fun getAuthSessionMode(): AuthSessionMode? {
        val raw = prefs.getString(KEY_AUTH_SESSION_MODE, null) ?: return null
        return try {
            AuthSessionMode.valueOf(raw)
        } catch (_: Exception) {
            null
        }
    }

    fun setAuthSessionMode(mode: AuthSessionMode?) {
        prefs.edit {
            if (mode == null) remove(KEY_AUTH_SESSION_MODE)
            else putString(KEY_AUTH_SESSION_MODE, mode.name)
        }
    }

    fun isStartupPermissionCheckDone(): Boolean =
        prefs.getBoolean(KEY_STARTUP_PERMISSION_CHECKED, false)

    fun setStartupPermissionCheckDone(done: Boolean) {
        prefs.edit { putBoolean(KEY_STARTUP_PERMISSION_CHECKED, done) }
    }

    fun loadSettings(): RemoteSettings {
        val defaultName = Build.MODEL?.takeIf { it.isNotBlank() } ?: "TabRemote Cihaz"
        val navOrder = parseEnumList(
            raw = prefs.getString(KEY_NAV_ORDER, null),
            entries = NavDockItemId.entries,
            fallback = RemoteSettings().navDockOrder
        )
        val hiddenNav = parseEnumSet(
            raw = prefs.getString(KEY_HIDDEN_NAV, null),
            entries = NavDockItemId.entries
        )
        val mediaOrder = parseEnumList(
            raw = prefs.getString(KEY_MEDIA_ORDER, null),
            entries = MediaDockItemId.entries,
            fallback = RemoteSettings().mediaDockOrder
        )
        val hiddenMedia = parseEnumSet(
            raw = prefs.getString(KEY_HIDDEN_MEDIA, null),
            entries = MediaDockItemId.entries
        )

        val (legacyActive, legacyPool) = parseQuickTools(
            rawActive = prefs.getString(KEY_QUICK_TOOLS_ACTIVE, null),
            rawPool = prefs.getString(KEY_QUICK_TOOLS_POOL, null)
        )
        val quickPlacements = parseQuickToolPlacements(
            rawJson = prefs.getString(KEY_QUICK_TOOLS_PLACEMENTS, null),
            legacyActiveOrder = legacyActive
        )
        val effectiveActiveOrder = quickPlacements.map { it.tool }
        val effectivePool = (legacyPool.filter { it !in effectiveActiveOrder } +
            QuickToolId.entries.filter { it !in effectiveActiveOrder && it !in legacyPool }).distinct()

        val wifiPlacements = parseWifiWidgetPlacements(
            rawJson = prefs.getString(KEY_WIFI_WIDGET_PLACEMENTS, null)
        )

        return RemoteSettings(
            appRole = parseEnum(prefs.getString(KEY_APP_ROLE, null), AppRole.CONTROLLER),
            localDeviceName = prefs.getString(KEY_LOCAL_DEVICE_NAME, defaultName) ?: defaultName,
            pointerSensitivity = prefs.getFloat(KEY_POINTER_SENS, 1.3f),
            scrollSensitivity = prefs.getFloat(KEY_SCROLL_SENS, 0.9f),
            invertScroll = prefs.getBoolean(KEY_INVERT_SCROLL, false),
            showHorizontalScrollGutter = prefs.getBoolean(KEY_SHOW_H_GUTTER, true),
            horizontalPanPosition = parseEnum(prefs.getString(KEY_H_PAN_POS, null), HorizontalPanPosition.ABOVE_BUTTONS),
            airMouseSensitivity = prefs.getFloat(KEY_AIR_SENS, 2.0f),
            airMouseDeadband = prefs.getFloat(KEY_AIR_DEADBAND, 0.04f),
            airMouseSmoothing = prefs.getFloat(KEY_AIR_SMOOTH, 0.65f),
            airMouseScrollMode = parseEnum(prefs.getString(KEY_AIR_SCROLL_MODE, null), AirMouseScrollMode.SCROLL_STRIP),
            scrollSide = parseEnum(prefs.getString(KEY_SCROLL_SIDE, null), ScrollSide.RIGHT),
            buttonPosition = ButtonPosition.BOTTOM,
            showRightClick = prefs.getBoolean(KEY_SHOW_RIGHT_CLICK, true),
            showMiddleClick = prefs.getBoolean(KEY_SHOW_MIDDLE_CLICK, false),
            showFunctionButtons = prefs.getBoolean(KEY_SHOW_FN_BTNS, true),
            tapToClick = prefs.getBoolean(KEY_TAP_TO_CLICK, true),
            hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true),
            touchpadVolumeDownAction = parseEnum(prefs.getString(KEY_TP_VOL_DOWN, null), VolumeAction.SCROLL_DOWN),
            touchpadVolumeUpAction = parseEnum(prefs.getString(KEY_TP_VOL_UP, null), VolumeAction.SCROLL_UP),
            airMouseVolumeDownAction = parseEnum(prefs.getString(KEY_AIR_VOL_DOWN, null), VolumeAction.MOUSE_LEFT_CLICK),
            airMouseVolumeUpAction = parseEnum(prefs.getString(KEY_AIR_VOL_UP, null), VolumeAction.MOUSE_RIGHT_CLICK),
            favoriteDeviceAddresses = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet(),
            isTurkishKeyboardLayout = prefs.getBoolean(KEY_TR_LAYOUT, true),
            showPcNumpad = prefs.getBoolean(KEY_PC_NUMPAD, false),
            showPcNumberRow = prefs.getBoolean(KEY_PC_NUMROW, true),
            splitPcKeyboard = prefs.getBoolean(KEY_PC_SPLIT, false),
            pcSplitGapWeight = prefs.getFloat(KEY_PC_SPLIT_GAP, 1.6f).coerceIn(0.4f, 3.2f),
            gamepadStyle = parseEnum(prefs.getString(KEY_GAMEPAD_STYLE, null), GamepadStyle.PLAYSTATION),
            gamepadOffsets = GamepadLayoutOffsets(
                dpadX = prefs.getFloat(KEY_GP_DPAD_X, 0f).coerceIn(-0.45f, 0.45f),
                dpadY = prefs.getFloat(KEY_GP_DPAD_Y, 0f).coerceIn(-0.45f, 0.45f),
                leftStickX = prefs.getFloat(KEY_GP_LSTICK_X, 0f).coerceIn(-0.45f, 0.45f),
                leftStickY = prefs.getFloat(KEY_GP_LSTICK_Y, 0f).coerceIn(-0.45f, 0.45f),
                faceButtonsX = prefs.getFloat(KEY_GP_FACE_X, 0f).coerceIn(-0.45f, 0.45f),
                faceButtonsY = prefs.getFloat(KEY_GP_FACE_Y, 0f).coerceIn(-0.45f, 0.45f),
                rightStickX = prefs.getFloat(KEY_GP_RSTICK_X, 0f).coerceIn(-0.45f, 0.45f),
                rightStickY = prefs.getFloat(KEY_GP_RSTICK_Y, 0f).coerceIn(-0.45f, 0.45f),
                centerPadX = prefs.getFloat(KEY_GP_PAD_X, 0f).coerceIn(-0.45f, 0.45f),
                centerPadY = prefs.getFloat(KEY_GP_PAD_Y, 0f).coerceIn(-0.45f, 0.45f)
            ),
            swapGamepadLeftWing = prefs.getBoolean(KEY_GP_SWAP_LEFT, false),
            swapGamepadRightWing = prefs.getBoolean(KEY_GP_SWAP_RIGHT, false),
            mirrorGamepadHands = prefs.getBoolean(KEY_GP_MIRROR, false),
            navDockOrder = navOrder,
            hiddenNavDockItems = hiddenNav,
            mediaDockOrder = mediaOrder,
            hiddenMediaDockItems = hiddenMedia,
            quickToolsActiveOrder = effectiveActiveOrder,
            quickToolsPoolOrder = effectivePool,
            quickToolsPlacements = quickPlacements,
            wifiControlPlacements = wifiPlacements,
            liveTypingMode = prefs.getBoolean(KEY_LIVE_TYPING_MODE, true),
            themePreset = parseEnum(prefs.getString(KEY_THEME_PRESET, null), ThemePreset.RUBY),
            useDynamicColor = prefs.getBoolean(KEY_USE_DYNAMIC_COLOR, false),
            cursorScale = prefs.getFloat(KEY_CURSOR_SCALE, 1.0f).coerceIn(0.6f, 2.2f),
            cursorColorArgb = prefs.getInt(KEY_CURSOR_COLOR_ARGB, 0xFFFF2A4D.toInt()),
            cursorBorderColorArgb = prefs.getInt(KEY_CURSOR_BORDER_ARGB, 0xFFFFFFFF.toInt()),
            cursorContrastMode = parseEnum(prefs.getString(KEY_CURSOR_CONTRAST_MODE, null), CursorContrastMode.MANUAL)
        )
    }

    fun saveSettings(settings: RemoteSettings) {
        val rawPlacements = if (settings.quickToolsPlacements.isNotEmpty() || settings.quickToolsActiveOrder.isEmpty()) {
            settings.quickToolsPlacements
        } else {
            QuickToolsLayoutEngine.pack(settings.quickToolsActiveOrder).first.map {
                QuickToolPlacement(it.tool, it.col, it.row, it.colSpan, it.rowSpan)
            }
        }
        val syncedPlacements = rawPlacements.map { p ->
            val allowed = p.tool.allowedSpans()
            val span = if ((p.colSpan to p.rowSpan) in allowed) (p.colSpan to p.rowSpan) else allowed.first()
            p.copy(
                colSpan = span.first,
                rowSpan = span.second,
                col = p.col.coerceIn(0, (SparseGridEngine.GRID_COLUMNS - span.first).coerceAtLeast(0))
            )
        }
        val syncedActiveOrder = syncedPlacements.map { it.tool }

        prefs.edit {
            putString(KEY_APP_ROLE, settings.appRole.name)
            putString(KEY_LOCAL_DEVICE_NAME, settings.localDeviceName)
            putFloat(KEY_POINTER_SENS, settings.pointerSensitivity)
            putFloat(KEY_SCROLL_SENS, settings.scrollSensitivity)
            putBoolean(KEY_INVERT_SCROLL, settings.invertScroll)
            putBoolean(KEY_SHOW_H_GUTTER, settings.showHorizontalScrollGutter)
            putString(KEY_H_PAN_POS, settings.horizontalPanPosition.name)
            putFloat(KEY_AIR_SENS, settings.airMouseSensitivity)
            putFloat(KEY_AIR_DEADBAND, settings.airMouseDeadband)
            putFloat(KEY_AIR_SMOOTH, settings.airMouseSmoothing)
            putString(KEY_AIR_SCROLL_MODE, settings.airMouseScrollMode.name)
            putString(KEY_SCROLL_SIDE, settings.scrollSide.name)
            putString(KEY_BTN_POS, ButtonPosition.BOTTOM.name)
            putBoolean(KEY_SHOW_RIGHT_CLICK, settings.showRightClick)
            putBoolean(KEY_SHOW_MIDDLE_CLICK, settings.showMiddleClick)
            putBoolean(KEY_SHOW_FN_BTNS, settings.showFunctionButtons)
            putBoolean(KEY_TAP_TO_CLICK, settings.tapToClick)
            putBoolean(KEY_HAPTICS, settings.hapticsEnabled)
            putString(KEY_TP_VOL_DOWN, settings.touchpadVolumeDownAction.name)
            putString(KEY_TP_VOL_UP, settings.touchpadVolumeUpAction.name)
            putString(KEY_AIR_VOL_DOWN, settings.airMouseVolumeDownAction.name)
            putString(KEY_AIR_VOL_UP, settings.airMouseVolumeUpAction.name)
            putStringSet(KEY_FAVORITES, settings.favoriteDeviceAddresses)
            putBoolean(KEY_TR_LAYOUT, settings.isTurkishKeyboardLayout)
            putBoolean(KEY_PC_NUMPAD, settings.showPcNumpad)
            putBoolean(KEY_PC_NUMROW, settings.showPcNumberRow)
            putBoolean(KEY_PC_SPLIT, settings.splitPcKeyboard)
            putFloat(KEY_PC_SPLIT_GAP, settings.pcSplitGapWeight)
            putString(KEY_GAMEPAD_STYLE, settings.gamepadStyle.name)
            putFloat(KEY_GP_DPAD_X, settings.gamepadOffsets.dpadX)
            putFloat(KEY_GP_DPAD_Y, settings.gamepadOffsets.dpadY)
            putFloat(KEY_GP_LSTICK_X, settings.gamepadOffsets.leftStickX)
            putFloat(KEY_GP_LSTICK_Y, settings.gamepadOffsets.leftStickY)
            putFloat(KEY_GP_FACE_X, settings.gamepadOffsets.faceButtonsX)
            putFloat(KEY_GP_FACE_Y, settings.gamepadOffsets.faceButtonsY)
            putFloat(KEY_GP_RSTICK_X, settings.gamepadOffsets.rightStickX)
            putFloat(KEY_GP_RSTICK_Y, settings.gamepadOffsets.rightStickY)
            putFloat(KEY_GP_PAD_X, settings.gamepadOffsets.centerPadX)
            putFloat(KEY_GP_PAD_Y, settings.gamepadOffsets.centerPadY)
            putBoolean(KEY_GP_SWAP_LEFT, settings.swapGamepadLeftWing)
            putBoolean(KEY_GP_SWAP_RIGHT, settings.swapGamepadRightWing)
            putBoolean(KEY_GP_MIRROR, settings.mirrorGamepadHands)
            putString(KEY_NAV_ORDER, settings.navDockOrder.joinToString(",") { it.name })
            putString(KEY_HIDDEN_NAV, settings.hiddenNavDockItems.joinToString(",") { it.name })
            putString(KEY_MEDIA_ORDER, settings.mediaDockOrder.joinToString(",") { it.name })
            putString(KEY_HIDDEN_MEDIA, settings.hiddenMediaDockItems.joinToString(",") { it.name })
            putString(KEY_QUICK_TOOLS_ACTIVE, syncedActiveOrder.joinToString(",") { it.name })
            putString(KEY_QUICK_TOOLS_POOL, settings.quickToolsPoolOrder.joinToString(",") { it.name })
            putString(KEY_QUICK_TOOLS_PLACEMENTS, serializeQuickToolPlacements(syncedPlacements))
            putString(KEY_WIFI_WIDGET_PLACEMENTS, serializeWifiWidgetPlacements(settings.wifiControlPlacements))
            putBoolean(KEY_LIVE_TYPING_MODE, settings.liveTypingMode)
            putString(KEY_THEME_PRESET, settings.themePreset.name)
            putBoolean(KEY_USE_DYNAMIC_COLOR, settings.useDynamicColor)
            putFloat(KEY_CURSOR_SCALE, settings.cursorScale)
            putInt(KEY_CURSOR_COLOR_ARGB, settings.cursorColorArgb)
            putInt(KEY_CURSOR_BORDER_ARGB, settings.cursorBorderColorArgb)
            putString(KEY_CURSOR_CONTRAST_MODE, settings.cursorContrastMode.name)
        }
    }

    private fun serializeQuickToolPlacements(list: List<QuickToolPlacement>): String {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject()
                    .put("id", item.tool.name)
                    .put("c", item.col)
                    .put("r", item.row)
                    .put("cs", item.colSpan)
                    .put("rs", item.rowSpan)
            )
        }
        return arr.toString()
    }

    private fun parseQuickToolPlacements(
        rawJson: String?,
        legacyActiveOrder: List<QuickToolId>
    ): List<QuickToolPlacement> {
        if (rawJson.isNullOrBlank()) {
            return QuickToolsLayoutEngine.pack(legacyActiveOrder, SparseGridEngine.GRID_COLUMNS).first.map {
                QuickToolPlacement(it.tool, it.col, it.row, it.colSpan, it.rowSpan)
            }
        }
        return try {
            val byName = QuickToolId.entries.associateBy { it.name }
            val arr = JSONArray(rawJson)
            val parsed = mutableListOf<SparseCell<QuickToolId>>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val tool = byName[obj.optString("id")] ?: continue
                val cs = obj.optInt("cs", tool.colSpan).coerceIn(1, SparseGridEngine.GRID_COLUMNS)
                val rs = obj.optInt("rs", tool.rowSpan).coerceAtLeast(1)
                val c = obj.optInt("c", 0).coerceIn(0, SparseGridEngine.GRID_COLUMNS - cs)
                val r = obj.optInt("r", 0).coerceAtLeast(0)
                parsed.add(SparseCell(tool, c, r, cs, rs))
            }
            if (parsed.isEmpty() && legacyActiveOrder.isNotEmpty()) {
                QuickToolsLayoutEngine.pack(legacyActiveOrder).first.map {
                    QuickToolPlacement(it.tool, it.col, it.row, it.colSpan, it.rowSpan)
                }
            } else {
                SparseGridEngine.normalize(parsed).map {
                    QuickToolPlacement(it.id, it.col, it.row, it.colSpan, it.rowSpan)
                }
            }
        } catch (_: Exception) {
            QuickToolsLayoutEngine.pack(legacyActiveOrder).first.map {
                QuickToolPlacement(it.tool, it.col, it.row, it.colSpan, it.rowSpan)
            }
        }
    }

    private fun serializeWifiWidgetPlacements(list: List<WifiWidgetPlacement>): String {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject()
                    .put("id", item.widget.name)
                    .put("c", item.col)
                    .put("r", item.row)
                    .put("cs", item.colSpan)
                    .put("rs", item.rowSpan)
            )
        }
        return arr.toString()
    }

    private fun parseWifiWidgetPlacements(rawJson: String?): List<WifiWidgetPlacement> {
        if (rawJson.isNullOrBlank()) return WifiQuickWidgetId.defaultPlacements
        return try {
            val byName = WifiQuickWidgetId.entries.associateBy { it.name }
            val arr = JSONArray(rawJson)
            val parsed = mutableListOf<SparseCell<WifiQuickWidgetId>>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val widget = byName[obj.optString("id")] ?: continue
                val cs = obj.optInt("cs", widget.defaultColSpan).coerceIn(1, SparseGridEngine.GRID_COLUMNS)
                val rs = obj.optInt("rs", widget.defaultRowSpan).coerceAtLeast(1)
                val c = obj.optInt("c", 0).coerceIn(0, SparseGridEngine.GRID_COLUMNS - cs)
                val r = obj.optInt("r", 0).coerceAtLeast(0)
                parsed.add(SparseCell(widget, c, r, cs, rs))
            }
            if (parsed.isEmpty()) WifiQuickWidgetId.defaultPlacements
            else SparseGridEngine.normalize(parsed).map {
                WifiWidgetPlacement(it.id, it.col, it.row, it.colSpan, it.rowSpan)
            }
        } catch (_: Exception) {
            WifiQuickWidgetId.defaultPlacements
        }
    }

    private fun parseQuickTools(rawActive: String?, rawPool: String?): Pair<List<QuickToolId>, List<QuickToolId>> {
        if (rawActive == null && rawPool == null) {
            return QuickToolId.defaultActiveGrid to QuickToolId.defaultPool
        }
        val byName = QuickToolId.entries.associateBy { it.name }
        val active = rawActive.orEmpty()
            .split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { byName[it] }
            .distinct()
        val poolExplicit = rawPool.orEmpty()
            .split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { byName[it] }
            .filter { it !in active }
            .distinct()
        val missing = QuickToolId.entries.filter { it !in active && it !in poolExplicit }
        return active to (poolExplicit + missing)
    }

    private fun <T : Enum<T>> parseEnum(raw: String?, fallback: T): T {
        if (raw.isNullOrBlank()) return fallback
        return try {
            java.lang.Enum.valueOf(fallback.declaringJavaClass, raw)
        } catch (_: Exception) {
            fallback
        }
    }

    private fun <T : Enum<T>> parseEnumList(
        raw: String?,
        entries: List<T>,
        fallback: List<T>
    ): List<T> {
        if (raw.isNullOrBlank()) return fallback
        val byName = entries.associateBy { it.name }
        val parsed = raw.split(",").mapNotNull { byName[it] }.distinct()
        if (parsed.isEmpty()) return fallback
        val missing = entries.filter { it !in parsed }
        return parsed + missing
    }

    private fun <T : Enum<T>> parseEnumSet(raw: String?, entries: List<T>): Set<T> {
        if (raw.isNullOrBlank()) return emptySet()
        val byName = entries.associateBy { it.name }
        return raw.split(",").mapNotNull { byName[it] }.toSet()
    }

    private companion object {
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_SESSION_TOKEN = "session_token"
        const val KEY_AUTH_SESSION_MODE = "auth_session_mode"
        const val KEY_STARTUP_PERMISSION_CHECKED = "startup_perm_checked"
        const val KEY_APP_ROLE = "app_role"
        const val KEY_LOCAL_DEVICE_NAME = "local_device_name"
        const val KEY_POINTER_SENS = "pointer_sens"
        const val KEY_SCROLL_SENS = "scroll_sens"
        const val KEY_INVERT_SCROLL = "invert_scroll"
        const val KEY_SHOW_H_GUTTER = "show_h_gutter"
        const val KEY_H_PAN_POS = "h_pan_pos"
        const val KEY_AIR_SENS = "air_sens"
        const val KEY_AIR_DEADBAND = "air_deadband"
        const val KEY_AIR_SMOOTH = "air_smooth"
        const val KEY_AIR_SCROLL_MODE = "air_scroll_mode"
        const val KEY_SCROLL_SIDE = "scroll_side"
        const val KEY_BTN_POS = "btn_pos"
        const val KEY_SHOW_RIGHT_CLICK = "show_right_click"
        const val KEY_SHOW_MIDDLE_CLICK = "show_middle_click"
        const val KEY_SHOW_FN_BTNS = "show_fn_btns"
        const val KEY_TAP_TO_CLICK = "tap_to_click"
        const val KEY_HAPTICS = "haptics"
        const val KEY_TP_VOL_DOWN = "tp_vol_down"
        const val KEY_TP_VOL_UP = "tp_vol_up"
        const val KEY_AIR_VOL_DOWN = "air_vol_down"
        const val KEY_AIR_VOL_UP = "air_vol_up"
        const val KEY_FAVORITES = "favorites"
        const val KEY_TR_LAYOUT = "tr_layout"
        const val KEY_PC_NUMPAD = "pc_numpad"
        const val KEY_PC_NUMROW = "pc_numrow"
        const val KEY_PC_SPLIT = "pc_split"
        const val KEY_PC_SPLIT_GAP = "pc_split_gap"
        const val KEY_GAMEPAD_STYLE = "gamepad_style"
        const val KEY_GP_DPAD_X = "gp_dpad_x"
        const val KEY_GP_DPAD_Y = "gp_dpad_y"
        const val KEY_GP_LSTICK_X = "gp_lstick_x"
        const val KEY_GP_LSTICK_Y = "gp_lstick_y"
        const val KEY_GP_FACE_X = "gp_face_x"
        const val KEY_GP_FACE_Y = "gp_face_y"
        const val KEY_GP_RSTICK_X = "gp_rstick_x"
        const val KEY_GP_RSTICK_Y = "gp_rstick_y"
        const val KEY_GP_PAD_X = "gp_pad_x"
        const val KEY_GP_PAD_Y = "gp_pad_y"
        const val KEY_GP_SWAP_LEFT = "gp_swap_left"
        const val KEY_GP_SWAP_RIGHT = "gp_swap_right"
        const val KEY_GP_MIRROR = "gp_mirror"
        const val KEY_NAV_ORDER = "nav_order"
        const val KEY_HIDDEN_NAV = "hidden_nav"
        const val KEY_MEDIA_ORDER = "media_order"
        const val KEY_HIDDEN_MEDIA = "hidden_media"
        const val KEY_QUICK_TOOLS_ACTIVE = "quick_tools_active"
        const val KEY_QUICK_TOOLS_POOL = "quick_tools_pool"
        const val KEY_QUICK_TOOLS_PLACEMENTS = "quick_tools_placements_v2"
        const val KEY_WIFI_WIDGET_PLACEMENTS = "wifi_widget_placements_v1"
        const val KEY_LIVE_TYPING_MODE = "live_typing_mode"
        const val KEY_THEME_PRESET = "theme_preset"
        const val KEY_USE_DYNAMIC_COLOR = "use_dynamic_color"
        const val KEY_CURSOR_SCALE = "cursor_scale"
        const val KEY_CURSOR_COLOR_ARGB = "cursor_color_argb"
        const val KEY_CURSOR_BORDER_ARGB = "cursor_border_argb"
        const val KEY_CURSOR_CONTRAST_MODE = "cursor_contrast_mode"
    }
}

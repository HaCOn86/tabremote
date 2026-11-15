package com.example.data

import android.content.Context
import android.os.Build
import com.example.model.AirMouseScrollMode
import com.example.model.AppRole
import com.example.model.ButtonPosition
import com.example.model.GamepadStyle
import com.example.model.HidProfileMode
import com.example.model.MediaDockItemId
import com.example.model.NavDockItemId
import com.example.model.RemoteSettings
import com.example.model.ScrollSide
import com.example.model.VolumeAction
import java.util.UUID

class SettingsPrefsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("tabremote_v2_prefs", Context.MODE_PRIVATE)

    fun getOrCreateDeviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val created = "dev_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        prefs.edit().putString(KEY_DEVICE_ID, created).apply()
        return created
    }

    fun getOrCreateSessionToken(): String {
        val existing = prefs.getString(KEY_SESSION_TOKEN, null)
        if (!existing.isNullOrBlank() && existing.length >= 12) return existing
        val created = "tr_${UUID.randomUUID().toString().replace("-", "")}"
        prefs.edit().putString(KEY_SESSION_TOKEN, created).apply()
        return created
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

        return RemoteSettings(
            appRole = parseEnum(prefs.getString(KEY_APP_ROLE, null), AppRole.CONTROLLER),
            unifiedControlMode = prefs.getBoolean(KEY_UNIFIED_CONTROL, true),
            customBluetoothName = prefs.getString(KEY_BT_NAME, "TabRemote") ?: "TabRemote",
            localDeviceName = prefs.getString(KEY_LOCAL_DEVICE_NAME, defaultName) ?: defaultName,
            pointerSensitivity = prefs.getFloat(KEY_POINTER_SENS, 1.3f),
            scrollSensitivity = prefs.getFloat(KEY_SCROLL_SENS, 0.9f),
            invertScroll = prefs.getBoolean(KEY_INVERT_SCROLL, false),
            showHorizontalScrollGutter = prefs.getBoolean(KEY_SHOW_H_GUTTER, true),
            airMouseSensitivity = prefs.getFloat(KEY_AIR_SENS, 2.0f),
            airMouseDeadband = prefs.getFloat(KEY_AIR_DEADBAND, 0.04f),
            airMouseSmoothing = prefs.getFloat(KEY_AIR_SMOOTH, 0.65f),
            airMouseScrollMode = parseEnum(prefs.getString(KEY_AIR_SCROLL_MODE, null), AirMouseScrollMode.SCROLL_STRIP),
            scrollSide = parseEnum(prefs.getString(KEY_SCROLL_SIDE, null), ScrollSide.RIGHT),
            buttonPosition = parseEnum(prefs.getString(KEY_BTN_POS, null), ButtonPosition.BOTTOM),
            showRightClick = prefs.getBoolean(KEY_SHOW_RIGHT_CLICK, true),
            showMiddleClick = prefs.getBoolean(KEY_SHOW_MIDDLE_CLICK, false),
            showFunctionButtons = prefs.getBoolean(KEY_SHOW_FN_BTNS, true),
            tapToClick = prefs.getBoolean(KEY_TAP_TO_CLICK, true),
            hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true),
            hidProfileMode = parseEnum(prefs.getString(KEY_HID_PROFILE, null), HidProfileMode.PRECISION_TOUCHPAD),
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
            swapGamepadLeftWing = prefs.getBoolean(KEY_GP_SWAP_LEFT, false),
            swapGamepadRightWing = prefs.getBoolean(KEY_GP_SWAP_RIGHT, false),
            mirrorGamepadHands = prefs.getBoolean(KEY_GP_MIRROR, false),
            navDockOrder = navOrder,
            hiddenNavDockItems = hiddenNav,
            mediaDockOrder = mediaOrder,
            hiddenMediaDockItems = hiddenMedia
        )
    }

    fun saveSettings(settings: RemoteSettings) {
        prefs.edit()
            .putString(KEY_APP_ROLE, settings.appRole.name)
            .putBoolean(KEY_UNIFIED_CONTROL, settings.unifiedControlMode)
            .putString(KEY_BT_NAME, settings.customBluetoothName)
            .putString(KEY_LOCAL_DEVICE_NAME, settings.localDeviceName)
            .putFloat(KEY_POINTER_SENS, settings.pointerSensitivity)
            .putFloat(KEY_SCROLL_SENS, settings.scrollSensitivity)
            .putBoolean(KEY_INVERT_SCROLL, settings.invertScroll)
            .putBoolean(KEY_SHOW_H_GUTTER, settings.showHorizontalScrollGutter)
            .putFloat(KEY_AIR_SENS, settings.airMouseSensitivity)
            .putFloat(KEY_AIR_DEADBAND, settings.airMouseDeadband)
            .putFloat(KEY_AIR_SMOOTH, settings.airMouseSmoothing)
            .putString(KEY_AIR_SCROLL_MODE, settings.airMouseScrollMode.name)
            .putString(KEY_SCROLL_SIDE, settings.scrollSide.name)
            .putString(KEY_BTN_POS, settings.buttonPosition.name)
            .putBoolean(KEY_SHOW_RIGHT_CLICK, settings.showRightClick)
            .putBoolean(KEY_SHOW_MIDDLE_CLICK, settings.showMiddleClick)
            .putBoolean(KEY_SHOW_FN_BTNS, settings.showFunctionButtons)
            .putBoolean(KEY_TAP_TO_CLICK, settings.tapToClick)
            .putBoolean(KEY_HAPTICS, settings.hapticsEnabled)
            .putString(KEY_HID_PROFILE, settings.hidProfileMode.name)
            .putString(KEY_TP_VOL_DOWN, settings.touchpadVolumeDownAction.name)
            .putString(KEY_TP_VOL_UP, settings.touchpadVolumeUpAction.name)
            .putString(KEY_AIR_VOL_DOWN, settings.airMouseVolumeDownAction.name)
            .putString(KEY_AIR_VOL_UP, settings.airMouseVolumeUpAction.name)
            .putStringSet(KEY_FAVORITES, settings.favoriteDeviceAddresses)
            .putBoolean(KEY_TR_LAYOUT, settings.isTurkishKeyboardLayout)
            .putBoolean(KEY_PC_NUMPAD, settings.showPcNumpad)
            .putBoolean(KEY_PC_NUMROW, settings.showPcNumberRow)
            .putBoolean(KEY_PC_SPLIT, settings.splitPcKeyboard)
            .putFloat(KEY_PC_SPLIT_GAP, settings.pcSplitGapWeight)
            .putString(KEY_GAMEPAD_STYLE, settings.gamepadStyle.name)
            .putBoolean(KEY_GP_SWAP_LEFT, settings.swapGamepadLeftWing)
            .putBoolean(KEY_GP_SWAP_RIGHT, settings.swapGamepadRightWing)
            .putBoolean(KEY_GP_MIRROR, settings.mirrorGamepadHands)
            .putString(KEY_NAV_ORDER, settings.navDockOrder.joinToString(",") { it.name })
            .putString(KEY_HIDDEN_NAV, settings.hiddenNavDockItems.joinToString(",") { it.name })
            .putString(KEY_MEDIA_ORDER, settings.mediaDockOrder.joinToString(",") { it.name })
            .putString(KEY_HIDDEN_MEDIA, settings.hiddenMediaDockItems.joinToString(",") { it.name })
            .apply()
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
        const val KEY_APP_ROLE = "app_role"
        const val KEY_UNIFIED_CONTROL = "unified_control"
        const val KEY_BT_NAME = "bt_name"
        const val KEY_LOCAL_DEVICE_NAME = "local_device_name"
        const val KEY_POINTER_SENS = "pointer_sens"
        const val KEY_SCROLL_SENS = "scroll_sens"
        const val KEY_INVERT_SCROLL = "invert_scroll"
        const val KEY_SHOW_H_GUTTER = "show_h_gutter"
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
        const val KEY_HID_PROFILE = "hid_profile"
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
        const val KEY_GP_SWAP_LEFT = "gp_swap_left"
        const val KEY_GP_SWAP_RIGHT = "gp_swap_right"
        const val KEY_GP_MIRROR = "gp_mirror"
        const val KEY_NAV_ORDER = "nav_order"
        const val KEY_HIDDEN_NAV = "hidden_nav"
        const val KEY_MEDIA_ORDER = "media_order"
        const val KEY_HIDDEN_MEDIA = "hidden_media"
    }
}

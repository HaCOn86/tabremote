package com.example.model

enum class AppRole(val label: String, val subtitle: String) {
    CONTROLLER("Kumanda Modu", "Telefon üzerinden tableti veya TV/PC'yi kontrol et"),
    RECEIVER("Alıcı Modu", "Bu cihazı (tablet) Wi-Fi + özel imleçle kontrol edilebilir yap")
}

enum class ActiveTransport(val label: String) {
    BLUETOOTH_HID("Bluetooth HID"),
    WIFI_RECEIVER("Wi-Fi + Erişilebilirlik")
}

enum class NavDockItemId(val defaultLabel: String, val description: String) {
    F1("F1", "4. Fare / Yardımcı Tuş"),
    BACK("BACK", "Geri"),
    HOME("HOME", "Ana Ekran"),
    RECENT("RECENT", "Son Uygulamalar"),
    F2("F2", "5. Fare / Yardımcı Tuş")
}

enum class MediaDockItemId(val label: String) {
    PREV("Önceki"),
    NEXT("Sonraki"),
    VOL_DOWN("Ses -"),
    VOL_UP("Ses +"),
    MUTE("Sessiz"),
    PLAY_PAUSE("Oynat / Durdur")
}

enum class VolumeAction(val title: String, val description: String, val category: String) {
    MOUSE_LEFT_CLICK("Sol Tık (Seçim)", "Birincil fare sol tıklaması", "FARE & TIKLAMA"),
    MOUSE_RIGHT_CLICK("Sağ Tık (Menü)", "İkincil sağ tık / bağlam menüsü", "FARE & TIKLAMA"),
    SCROLL_DOWN("Aşağı Kaydır (Wheel Down)", "Sayfayı veya videoyu aşağı kaydırır", "KAYDIRMA"),
    SCROLL_UP("Yukarı Kaydır (Wheel Up)", "Sayfayı veya videoyu yukarı kaydırır", "KAYDIRMA"),
    MEDIA_VOLUME_DOWN("Tablet Sesi Kıs", "Bağlı tabletin ses seviyesini azaltır", "MEDYA"),
    MEDIA_VOLUME_UP("Tablet Sesi Aç", "Bağlı tabletin ses seviyesini artırır", "MEDYA"),
    NONE("Devre Dışı (Telefon Sesi)", "Tuş basımını telefona bırakır", "GENEL")
}

enum class AirMouseScrollMode(val label: String) {
    SCROLL_STRIP("Dikey Kaydırma Rayı"),
    STEP_BUTTONS("Yukarı / Aşağı Tuşlar (Basılı Tut)")
}

enum class ScrollSide(val label: String) {
    RIGHT("Sağ Taraf"),
    LEFT("Sol Taraf"),
    HIDDEN("Gizli")
}

enum class ButtonPosition(val label: String) {
    BOTTOM("Touchpad Altı"),
    TOP("Touchpad Üstü"),
    SPLIT("Ayrık (Sol Altta, Sağ Üstte)")
}

enum class HidProfileMode(val label: String, val description: String) {
    PRECISION_TOUCHPAD(
        "Precision Touchpad (Samsung Doğal Çoklu Dokunuş)",
        "2 parmak kaydırma/zoom ve 3 parmak ana ekran/son uygulamalar tabletin doğal trackpad sürücüsüyle çalışır"
    ),
    CLASSIC_MOUSE(
        "Klasik Fare + Klavye",
        "Uyumluluk modu: Tüm parmak hareketleri standart 5 tuşlu fare ve kaydırma tekerleği olarak iletilir"
    )
}

enum class InputSubMode(val label: String) {
    SMART_KEYBOARD("Akıllı Klavye"),
    PC_KEYBOARD("PC Klavye"),
    GAMEPAD("Gamepad")
}

enum class GamepadStyle(val label: String) {
    PLAYSTATION("PlayStation (PS)"),
    XBOX("Xbox")
}

enum class MouseButton(val mask: Int) {
    NONE(0),
    LEFT(1),
    RIGHT(2),
    MIDDLE(4),
    BUTTON_4(8),
    BUTTON_5(16)
}

enum class AuthSessionMode(val label: String) {
    GOOGLE("Google Hesabı"),
    BLUETOOTH_ONLY("Sadece Bluetooth"),
    GUEST_LOCAL("Misafir Modu")
}

enum class QuickToolId(
    val label: String,
    val shortLabel: String,
    val colSpan: Int,
    val rowSpan: Int,
    val isDpad: Boolean = false
) {
    DPAD("Yön Tuşları", "YÖN 4x4", 4, 4, isDpad = true),
    PREV("Önceki", "ÖNCE", 1, 1),
    NEXT("Sonraki", "SONRA", 1, 1),
    VOL_DOWN("Ses -", "SES-", 1, 1),
    VOL_UP("Ses +", "SES+", 1, 1),
    MUTE("Sessiz", "SESSİZ", 1, 1),
    PLAY_PAUSE("Oynat / Durdur", "OYNAT / DURDUR", 2, 1),
    SPACE("Space", "SPACE", 2, 1),
    BACKSPACE("Backspace", "GERİ SİL", 2, 1),
    ENTER("Enter", "ENTER", 2, 1),
    DEL("Del", "DEL", 1, 1),
    ZOOM_IN("Yakınlaştır", "ZOOM+", 1, 1),
    ZOOM_OUT("Uzaklaştır", "ZOOM-", 1, 1),
    CUT("Kes", "KES", 1, 1),
    COPY("Kopyala", "KOPYALA", 1, 1),
    PASTE("Yapıştır", "YAPIŞTIR", 1, 1),
    TR_CCEDIL("ç", "ç", 1, 1),
    TR_GBREVE("ğ", "ğ", 1, 1),
    TR_DOTLESS_I("ı", "ı", 1, 1),
    TR_OUML("ö", "ö", 1, 1),
    TR_SCEDIL("ş", "ş", 1, 1),
    TR_UUML("ü", "ü", 1, 1),
    TR_CAPITAL_I("İ", "İ", 1, 1),
    PUNCT_COMMA(",", ",", 1, 1),
    PUNCT_DOT(".", ".", 1, 1);

    companion object {
        val defaultActiveGrid: List<QuickToolId> = listOf(
            PREV, NEXT, VOL_DOWN, VOL_UP, PLAY_PAUSE,
            DPAD, SPACE, BACKSPACE, ENTER, DEL, MUTE,
            TR_CCEDIL, TR_GBREVE, TR_DOTLESS_I, TR_OUML, TR_SCEDIL, TR_UUML
        )

        val defaultPool: List<QuickToolId> = entries.filter { it !in defaultActiveGrid }
    }
}

data class PlacedQuickTool(
    val tool: QuickToolId,
    val col: Int,
    val row: Int,
    val colSpan: Int,
    val rowSpan: Int
)

object QuickToolsLayoutEngine {
    const val GRID_COLUMNS = 6

    fun pack(tools: List<QuickToolId>, columns: Int = GRID_COLUMNS): Pair<List<PlacedQuickTool>, Int> {
        if (tools.isEmpty()) return emptyList<PlacedQuickTool>() to 0
        val occupied = mutableListOf<BooleanArray>()

        fun ensureRow(r: Int) {
            while (occupied.size <= r) {
                occupied.add(BooleanArray(columns))
            }
        }

        fun canPlace(startCol: Int, startRow: Int, colSpan: Int, rowSpan: Int): Boolean {
            if (startCol + colSpan > columns) return false
            for (r in startRow until startRow + rowSpan) {
                ensureRow(r)
                for (c in startCol until startCol + colSpan) {
                    if (occupied[r][c]) return false
                }
            }
            return true
        }

        fun mark(startCol: Int, startRow: Int, colSpan: Int, rowSpan: Int) {
            for (r in startRow until startRow + rowSpan) {
                ensureRow(r)
                for (c in startCol until startCol + colSpan) {
                    occupied[r][c] = true
                }
            }
        }

        val placed = mutableListOf<PlacedQuickTool>()
        var maxRowUsed = 0
        for (tool in tools) {
            val cw = tool.colSpan.coerceIn(1, columns)
            val rh = tool.rowSpan.coerceAtLeast(1)
            var found = false
            var r = 0
            while (!found) {
                for (c in 0..(columns - cw)) {
                    if (canPlace(c, r, cw, rh)) {
                        mark(c, r, cw, rh)
                        placed.add(PlacedQuickTool(tool, c, r, cw, rh))
                        if (r + rh > maxRowUsed) maxRowUsed = r + rh
                        found = true
                        break
                    }
                }
                if (!found) r++
            }
        }
        return placed to maxRowUsed
    }
}

data class RemoteSettings(
    val appRole: AppRole = AppRole.CONTROLLER,
    val unifiedControlMode: Boolean = true,
    val customBluetoothName: String = "TabRemote",
    val localDeviceName: String = "",
    val pointerSensitivity: Float = 1.3f,
    val scrollSensitivity: Float = 0.9f,
    val invertScroll: Boolean = false,
    val showHorizontalScrollGutter: Boolean = true,
    val airMouseSensitivity: Float = 2.0f,
    val airMouseDeadband: Float = 0.04f,
    val airMouseSmoothing: Float = 0.65f,
    val airMouseScrollMode: AirMouseScrollMode = AirMouseScrollMode.SCROLL_STRIP,
    val scrollSide: ScrollSide = ScrollSide.RIGHT,
    val buttonPosition: ButtonPosition = ButtonPosition.BOTTOM,
    val showRightClick: Boolean = true,
    val showMiddleClick: Boolean = false,
    val showFunctionButtons: Boolean = true,
    val tapToClick: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val hidProfileMode: HidProfileMode = HidProfileMode.PRECISION_TOUCHPAD,
    // Independent Volume Key profiles
    val touchpadVolumeDownAction: VolumeAction = VolumeAction.SCROLL_DOWN,
    val touchpadVolumeUpAction: VolumeAction = VolumeAction.SCROLL_UP,
    val airMouseVolumeDownAction: VolumeAction = VolumeAction.MOUSE_LEFT_CLICK,
    val airMouseVolumeUpAction: VolumeAction = VolumeAction.MOUSE_RIGHT_CLICK,
    val favoriteDeviceAddresses: Set<String> = emptySet(),
    val isTurkishKeyboardLayout: Boolean = true,
    val showPcNumpad: Boolean = false,
    val showPcNumberRow: Boolean = true,
    val splitPcKeyboard: Boolean = false,
    val pcSplitGapWeight: Float = 1.6f,
    val gamepadStyle: GamepadStyle = GamepadStyle.PLAYSTATION,
    val swapGamepadLeftWing: Boolean = false,
    val swapGamepadRightWing: Boolean = false,
    val mirrorGamepadHands: Boolean = false,
    val navDockOrder: List<NavDockItemId> = listOf(
        NavDockItemId.F1,
        NavDockItemId.BACK,
        NavDockItemId.HOME,
        NavDockItemId.RECENT,
        NavDockItemId.F2
    ),
    val hiddenNavDockItems: Set<NavDockItemId> = emptySet(),
    val mediaDockOrder: List<MediaDockItemId> = listOf(
        MediaDockItemId.PREV,
        MediaDockItemId.NEXT,
        MediaDockItemId.VOL_DOWN,
        MediaDockItemId.VOL_UP,
        MediaDockItemId.MUTE,
        MediaDockItemId.PLAY_PAUSE
    ),
    val hiddenMediaDockItems: Set<MediaDockItemId> = emptySet(),
    val quickToolsActiveOrder: List<QuickToolId> = QuickToolId.defaultActiveGrid,
    val quickToolsPoolOrder: List<QuickToolId> = QuickToolId.defaultPool,
    val liveTypingMode: Boolean = true
)

sealed interface ConnectionStatus {
    data object Disconnected : ConnectionStatus
    data object Initializing : ConnectionStatus
    data object ReadyToPair : ConnectionStatus
    data class Connected(
        val deviceName: String,
        val address: String,
        val isWifi: Boolean = false
    ) : ConnectionStatus
    data class Error(val message: String) : ConnectionStatus
}

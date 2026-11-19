package com.example.model

import kotlin.math.abs

enum class SettingsSubPage {
    MAIN,
    BLUETOOTH,
    GOOGLE_ACCOUNT,
    THEME,
    SENSITIVITY,
    VOLUME_KEYS,
    CURSOR,
    TOUCHPAD_LAYOUT
}

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

enum class HorizontalPanPosition(val label: String) {
    ABOVE_BUTTONS("Touchpad Üstünde / Altında"),
    BELOW_BUTTONS("Fare Tuşlarının Altında")
}

enum class InputSubMode(val label: String) {
    HUB_HOME("Medya & Panel"),
    PC_KEYBOARD("PC Klavye"),
    TV_REMOTE("Akıllı Kumanda"),
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

enum class CursorContrastMode(val label: String, val description: String) {
    MANUAL("Manuel", "Seçtiğiniz özel dolgu ve dış kenarlık renklerini kullanır"),
    INSTANT_BW("Anında Zıt", "Açık zeminde siyah, koyu zeminde beyaz imleçle anında kontrast"),
    GRADUAL_ADAPTIVE("Kademeli", "Arka plan parlaklığına göre rengi yumuşak kademeli dönüştürür")
}

enum class ThemePreset(
    val label: String,
    val primaryHex: Long,
    val accentHex: Long,
    val secondaryHex: Long,
    val darkHex: Long,
    val borderHex: Long
) {
    RUBY("Yakut Kırmızı", 0xFFFF2A4D, 0xFFFF4D6D, 0xFFFF758F, 0xFFCC1835, 0xFF3A161D),
    ELECTRIC_BLUE("Elektrik Mavi", 0xFF3B82F6, 0xFF60A5FA, 0xFF93C5FD, 0xFF2563EB, 0xFF172742),
    EMERALD("Zümrüt Yeşil", 0xFF10B981, 0xFF34D399, 0xFF6EE7B7, 0xFF059669, 0xFF12342A),
    AMBER("Amber Altın", 0xFFF59E0B, 0xFFFBBF24, 0xFFFDE68A, 0xFFD97706, 0xFF3B2B11),
    VIOLET("Mor Violet", 0xFFA855F7, 0xFFC084FC, 0xFFD8B4FE, 0xFF9333EA, 0xFF2F1A45)
}

enum class QuickToolId(
    val label: String,
    val shortLabel: String,
    val colSpan: Int,
    val rowSpan: Int,
    val isDpad: Boolean = false,
    val shortcutHint: String = ""
) {
    DPAD("Yön Tuşları", "YÖN", 3, 2, isDpad = true, shortcutHint = "Oklar"),
    SPACE("Space", "SPACE", 2, 1, shortcutHint = "Boşluk"),
    BACKSPACE("Backspace", "GERİ SİL", 2, 1, shortcutHint = "Sil"),
    ENTER("Enter", "ENTER", 2, 1, shortcutHint = "Onayla"),
    DEL("Del", "DEL", 1, 1, shortcutHint = "İleri Sil"),
    CUT("Kes", "KES", 1, 1, shortcutHint = "Ctrl+X"),
    COPY("Kopyala", "KOPYA", 1, 1, shortcutHint = "Ctrl+C"),
    PASTE("Yapıştır", "YAPIŞTIR", 1, 1, shortcutHint = "Ctrl+V"),
    ESC("Esc", "ESC", 1, 1, shortcutHint = "Çıkış"),
    TAB("Tab", "TAB", 1, 1, shortcutHint = "Sekme"),
    HOME_KEY("Home", "HOME", 1, 1, shortcutHint = "Başa"),
    END_KEY("End", "END", 1, 1, shortcutHint = "Sona"),
    PG_UP("PgUp", "PGUP", 1, 1, shortcutHint = "Yukarı"),
    PG_DN("PgDn", "PGDN", 1, 1, shortcutHint = "Aşağı"),
    NEW_TAB("Yeni Sekme", "YENİ SEKME", 2, 1, shortcutHint = "Ctrl+T"),
    CLOSE_TAB("Sekme Kapat", "KAPAT", 2, 1, shortcutHint = "Ctrl+W"),
    REFRESH("Yenile", "YENİLE", 2, 1, shortcutHint = "Ctrl+R"),
    ADDRESS_BAR("Adres", "ADRES", 2, 1, shortcutHint = "Ctrl+L"),
    FULLSCREEN("Tam Ekran", "TAM EKRAN", 2, 1, shortcutHint = "F11"),
    ALT_F4("Kapat", "KAPAT", 1, 1, shortcutHint = "Alt+F4"),
    SELECT_ALL("Tümünü Seç", "SEÇ", 1, 1, shortcutHint = "Ctrl+A"),
    UNDO("Geri Al", "GERİ AL", 1, 1, shortcutHint = "Ctrl+Z"),
    REDO("İleri Al", "İLERİ AL", 1, 1, shortcutHint = "Ctrl+Y"),
    ZOOM_IN("Yakınlaştır", "ZOOM+", 1, 1, shortcutHint = "Ctrl +"),
    ZOOM_OUT("Uzaklaştır", "ZOOM-", 1, 1, shortcutHint = "Ctrl -"),
    TR_CCEDIL("ç", "ç", 1, 1),
    TR_GBREVE("ğ", "ğ", 1, 1),
    TR_DOTLESS_I("ı", "ı", 1, 1),
    TR_OUML("ö", "ö", 1, 1),
    TR_SCEDIL("ş", "ş", 1, 1),
    TR_UUML("ü", "ü", 1, 1),
    TR_CAPITAL_I("İ", "İ", 1, 1),
    PUNCT_COMMA(",", ",", 1, 1),
    PUNCT_DOT(".", ".", 1, 1);

    val isFixedSingleCell: Boolean
        get() = name.startsWith("TR_") || name.startsWith("PUNCT_")

    fun allowedSpans(): List<Pair<Int, Int>> = when {
        isDpad -> listOf(3 to 2)
        isFixedSingleCell -> listOf(1 to 1)
        this == SPACE -> listOf(2 to 1, 1 to 1)
        colSpan >= 2 -> listOf(2 to 1, 1 to 1)
        else -> listOf(1 to 1, 2 to 1)
    }

    fun nextSpan(currentColSpan: Int, currentRowSpan: Int): Pair<Int, Int> {
        val options = allowedSpans()
        val idx = options.indexOfFirst { it.first == currentColSpan && it.second == currentRowSpan }
        return if (idx >= 0) options[(idx + 1) % options.size] else options.first()
    }

    companion object {
        val defaultActiveGrid: List<QuickToolId> = listOf(
            ESC, TAB, COPY, PASTE, UNDO, SELECT_ALL,
            DPAD, BACKSPACE, DEL, ENTER,
            SPACE, TR_CCEDIL, TR_GBREVE, TR_DOTLESS_I,
            TR_OUML, TR_SCEDIL, TR_UUML, TR_CAPITAL_I, PUNCT_COMMA, PUNCT_DOT
        )

        val defaultPool: List<QuickToolId> = entries.filter { it !in defaultActiveGrid }

        val defaultPlacements: List<QuickToolPlacement> by lazy {
            QuickToolsLayoutEngine.pack(defaultActiveGrid, QuickToolsLayoutEngine.GRID_COLUMNS).first
                .map { QuickToolPlacement(it.tool, it.col, it.row, it.colSpan, it.rowSpan) }
        }
    }
}

enum class WifiQuickWidgetId(
    val label: String,
    val shortcutHint: String,
    val defaultColSpan: Int,
    val defaultRowSpan: Int = 1
) {
    PLAY_PAUSE("Oynat / Duraklat", "Medya", 2),
    PREV("Önceki", "Parça", 1),
    NEXT("Sonraki", "Parça", 1),
    MUTE("Sessiz", "Sustur", 1),
    VOL_DOWN("Ses -", "Azalt", 1),
    VOL_UP("Ses +", "Artır", 1),
    BACK("Geri", "Sistem", 2),
    HOME("Ana Ekran", "Başlatıcı", 2),
    RECENTS("Son Uyg.", "Görevler", 2),
    NOTIFICATIONS("Bildirimler", "Panel", 2),
    QUICK_SETTINGS("Hızlı Ayar", "Sistem", 2),
    SCREENSHOT("Ekran Gör.", "Yakala", 2),
    LOCK_SCREEN("Ekran Kilitle", "Kilit", 2),
    ZOOM_IN("Yakınlaştır", "Pinch +", 1),
    ZOOM_OUT("Uzaklaştır", "Pinch -", 1),
    REFRESH("Yenile", "Ctrl+R", 1),
    NEW_TAB("Yeni Sekme", "Ctrl+T", 2),
    CLOSE_TAB("Sekme Kapat", "Ctrl+W", 1),
    ADDRESS_BAR("Adres", "Ctrl+L", 1),
    FULLSCREEN("Tam Ekran", "F11", 1);

    fun allowedSpans(): List<Pair<Int, Int>> = if (defaultColSpan >= 2) {
        listOf(2 to 1, 1 to 1)
    } else {
        listOf(1 to 1, 2 to 1)
    }

    fun nextSpan(currentColSpan: Int, currentRowSpan: Int): Pair<Int, Int> {
        val options = allowedSpans()
        val idx = options.indexOfFirst { it.first == currentColSpan && it.second == currentRowSpan }
        return if (idx >= 0) options[(idx + 1) % options.size] else options.first()
    }

    companion object {
        val defaultActiveWidgets: List<WifiQuickWidgetId> = listOf(
            PREV, PLAY_PAUSE, NEXT, VOL_DOWN, VOL_UP,
            BACK, HOME, RECENTS,
            NOTIFICATIONS, QUICK_SETTINGS, MUTE, ZOOM_IN,
            SCREENSHOT, LOCK_SCREEN, ZOOM_OUT, REFRESH
        )

        val defaultPlacements: List<WifiWidgetPlacement> by lazy {
            SparseGridEngine.packLinear(
                items = defaultActiveWidgets.map {
                    SparseGridNode(it, it.defaultColSpan, it.defaultRowSpan)
                },
                columns = SparseGridEngine.GRID_COLUMNS
            ).map { WifiWidgetPlacement(it.id, it.col, it.row, it.colSpan, it.rowSpan) }
        }
    }
}

data class GamepadLayoutOffsets(
    val dpadX: Float = 0f,
    val dpadY: Float = 0f,
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,
    val faceButtonsX: Float = 0f,
    val faceButtonsY: Float = 0f,
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,
    val centerPadX: Float = 0f,
    val centerPadY: Float = 0f
)

data class PlacedQuickTool(
    val tool: QuickToolId,
    val col: Int,
    val row: Int,
    val colSpan: Int,
    val rowSpan: Int
)

data class QuickToolPlacement(
    val tool: QuickToolId,
    val col: Int,
    val row: Int,
    val colSpan: Int = tool.colSpan,
    val rowSpan: Int = tool.rowSpan
)

data class WifiWidgetPlacement(
    val widget: WifiQuickWidgetId,
    val col: Int,
    val row: Int,
    val colSpan: Int = widget.defaultColSpan,
    val rowSpan: Int = widget.defaultRowSpan
)

data class SparseGridNode<T>(
    val id: T,
    val colSpan: Int,
    val rowSpan: Int
)

data class SparseCell<T>(
    val id: T,
    val col: Int,
    val row: Int,
    val colSpan: Int,
    val rowSpan: Int
)

/**
 * One UI 9 style 6-column coordinate-preserving 2D grid engine.
 * - Preserves intentional empty gaps between items.
 * - On collision during move/resize, slides colliding items to their nearest open cell.
 */
object SparseGridEngine {
    const val GRID_COLUMNS = 6

    fun <T> packLinear(
        items: List<SparseGridNode<T>>,
        columns: Int = GRID_COLUMNS
    ): List<SparseCell<T>> {
        if (items.isEmpty()) return emptyList()
        val occupied = mutableListOf<BooleanArray>()

        fun ensureRow(r: Int) {
            while (occupied.size <= r) occupied.add(BooleanArray(columns))
        }

        fun canPlace(startCol: Int, startRow: Int, colSpan: Int, rowSpan: Int): Boolean {
            if (startCol < 0 || startRow < 0 || startCol + colSpan > columns) return false
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

        val placed = mutableListOf<SparseCell<T>>()
        for (item in items) {
            val cw = item.colSpan.coerceIn(1, columns)
            val rh = item.rowSpan.coerceAtLeast(1)
            var found = false
            var r = 0
            while (!found) {
                for (c in 0..(columns - cw)) {
                    if (canPlace(c, r, cw, rh)) {
                        mark(c, r, cw, rh)
                        placed.add(SparseCell(item.id, c, r, cw, rh))
                        found = true
                        break
                    }
                }
                if (!found) r++
            }
        }
        return placed
    }

    fun <T> totalRows(cells: List<SparseCell<T>>, minRows: Int = 1): Int {
        val used = cells.maxOfOrNull { it.row + it.rowSpan } ?: 0
        return maxOf(used, minRows)
    }

    fun <T> placeWithPush(
        cells: List<SparseCell<T>>,
        targetId: T,
        targetCol: Int,
        targetRow: Int,
        targetColSpan: Int,
        targetRowSpan: Int,
        columns: Int = GRID_COLUMNS
    ): List<SparseCell<T>> {
        val cw = targetColSpan.coerceIn(1, columns)
        val rh = targetRowSpan.coerceAtLeast(1)
        val clampedCol = targetCol.coerceIn(0, (columns - cw).coerceAtLeast(0))
        val clampedRow = targetRow.coerceAtLeast(0)

        val targetCell = SparseCell(targetId, clampedCol, clampedRow, cw, rh)
        val occupied = mutableListOf<BooleanArray>()

        fun ensureRow(r: Int) {
            while (occupied.size <= r) occupied.add(BooleanArray(columns))
        }

        fun isAreaFree(col: Int, row: Int, colSpan: Int, rowSpan: Int): Boolean {
            if (col < 0 || row < 0 || col + colSpan > columns) return false
            for (r in row until row + rowSpan) {
                ensureRow(r)
                for (c in col until col + colSpan) {
                    if (occupied[r][c]) return false
                }
            }
            return true
        }

        fun markArea(col: Int, row: Int, colSpan: Int, rowSpan: Int) {
            for (r in row until row + rowSpan) {
                ensureRow(r)
                for (c in col until col + colSpan) {
                    occupied[r][c] = true
                }
            }
        }

        markArea(targetCell.col, targetCell.row, targetCell.colSpan, targetCell.rowSpan)

        val others = cells.filterNot { it.id == targetId }.sortedWith(compareBy({ it.row }, { it.col }))
        val stationary = mutableListOf<SparseCell<T>>()
        val pushed = mutableListOf<SparseCell<T>>()

        for (other in others) {
            val ocw = other.colSpan.coerceIn(1, columns)
            val orh = other.rowSpan.coerceAtLeast(1)
            val oCol = other.col.coerceIn(0, (columns - ocw).coerceAtLeast(0))
            val oRow = other.row.coerceAtLeast(0)
            if (isAreaFree(oCol, oRow, ocw, orh)) {
                markArea(oCol, oRow, ocw, orh)
                stationary.add(other.copy(col = oCol, row = oRow, colSpan = ocw, rowSpan = orh))
            } else {
                pushed.add(other.copy(col = oCol, row = oRow, colSpan = ocw, rowSpan = orh))
            }
        }

        val relocated = mutableListOf<SparseCell<T>>()
        for (item in pushed) {
            val maxSearchRow = maxOf(occupied.size + item.rowSpan + 3, item.row + 6)
            val candidates = mutableListOf<Triple<Int, Int, Int>>() // dist, row, col
            for (r in 0..maxSearchRow) {
                for (c in 0..(columns - item.colSpan)) {
                    if (isAreaFree(c, r, item.colSpan, item.rowSpan)) {
                        val dist = abs(r - item.row) * 10 + abs(c - item.col)
                        candidates.add(Triple(dist, r, c))
                    }
                }
            }
            val chosen = candidates.minWithOrNull(compareBy({ it.first }, { it.second }, { it.third }))
            val finalRow: Int
            val finalCol: Int
            if (chosen != null) {
                finalRow = chosen.second
                finalCol = chosen.third
            } else {
                var rFallback = occupied.size
                while (!isAreaFree(0, rFallback, item.colSpan, item.rowSpan)) {
                    rFallback++
                }
                finalRow = rFallback
                finalCol = 0
            }
            markArea(finalCol, finalRow, item.colSpan, item.rowSpan)
            relocated.add(item.copy(col = finalCol, row = finalRow))
        }

        return (listOf(targetCell) + stationary + relocated)
            .sortedWith(compareBy({ it.row }, { it.col }))
    }

    fun <T> addAtFirstOpenSlot(
        cells: List<SparseCell<T>>,
        id: T,
        colSpan: Int,
        rowSpan: Int,
        columns: Int = GRID_COLUMNS
    ): List<SparseCell<T>> {
        if (cells.any { it.id == id }) return cells
        val cw = colSpan.coerceIn(1, columns)
        val rh = rowSpan.coerceAtLeast(1)
        val occupied = mutableListOf<BooleanArray>()

        fun ensureRow(r: Int) {
            while (occupied.size <= r) occupied.add(BooleanArray(columns))
        }

        for (cell in cells) {
            for (r in cell.row until cell.row + cell.rowSpan) {
                ensureRow(r)
                for (c in cell.col until (cell.col + cell.colSpan).coerceAtMost(columns)) {
                    occupied[r][c] = true
                }
            }
        }

        fun isFree(col: Int, row: Int): Boolean {
            if (col + cw > columns) return false
            for (r in row until row + rh) {
                ensureRow(r)
                for (c in col until col + cw) {
                    if (occupied[r][c]) return false
                }
            }
            return true
        }

        var row = 0
        while (true) {
            for (col in 0..(columns - cw)) {
                if (isFree(col, row)) {
                    return (cells + SparseCell(id, col, row, cw, rh))
                        .sortedWith(compareBy({ it.row }, { it.col }))
                }
            }
            row++
        }
    }

    fun <T> normalize(
        cells: List<SparseCell<T>>,
        columns: Int = GRID_COLUMNS
    ): List<SparseCell<T>> {
        if (cells.isEmpty()) return emptyList()
        var acc = emptyList<SparseCell<T>>()
        val deduped = cells.distinctBy { it.id }
        for (cell in deduped) {
            acc = placeWithPush(
                cells = acc,
                targetId = cell.id,
                targetCol = cell.col,
                targetRow = cell.row,
                targetColSpan = cell.colSpan,
                targetRowSpan = cell.rowSpan,
                columns = columns
            )
        }
        return acc
    }
}

object QuickToolsLayoutEngine {
    const val GRID_COLUMNS = 6

    fun pack(tools: List<QuickToolId>, columns: Int = GRID_COLUMNS): Pair<List<PlacedQuickTool>, Int> {
        if (tools.isEmpty()) return emptyList<PlacedQuickTool>() to 0
        val nodes = tools.map { SparseGridNode(it, it.colSpan.coerceIn(1, columns), it.rowSpan.coerceAtLeast(1)) }
        val cells = SparseGridEngine.packLinear(nodes, columns)
        val maxRowUsed = SparseGridEngine.totalRows(cells, minRows = 0)
        return cells.map { PlacedQuickTool(it.id, it.col, it.row, it.colSpan, it.rowSpan) } to maxRowUsed
    }

    fun toSparseCells(placements: List<QuickToolPlacement>): List<SparseCell<QuickToolId>> =
        placements.map { SparseCell(it.tool, it.col, it.row, it.colSpan, it.rowSpan) }

    fun fromSparseCells(cells: List<SparseCell<QuickToolId>>): List<QuickToolPlacement> =
        cells.map { QuickToolPlacement(it.id, it.col, it.row, it.colSpan, it.rowSpan) }
}

data class ReceiverTelemetry(
    val activeAppPackage: String = "",
    val activeAppName: String = "",
    val mediaTitle: String = "",
    val mediaArtist: String = "",
    val mediaAppName: String = "",
    val isMediaPlaying: Boolean = false,
    val mediaPositionMs: Long = 0L,
    val mediaDurationMs: Long = 0L,
    val volumePercent: Int = 50,
    val isMuted: Boolean = false,
    val batteryPercent: Int = -1,
    val isBatteryCharging: Boolean = false,
    val notificationListenerEnabled: Boolean = false,
    val updatedAtMs: Long = 0L
)

data class RemoteSettings(
    val appRole: AppRole = AppRole.CONTROLLER,
    val localDeviceName: String = "",
    val pointerSensitivity: Float = 1.3f,
    val scrollSensitivity: Float = 0.9f,
    val invertScroll: Boolean = false,
    val showHorizontalScrollGutter: Boolean = true,
    val horizontalPanPosition: HorizontalPanPosition = HorizontalPanPosition.ABOVE_BUTTONS,
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
    val gamepadOffsets: GamepadLayoutOffsets = GamepadLayoutOffsets(),
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
    val quickToolsPlacements: List<QuickToolPlacement> = QuickToolId.defaultPlacements,
    val wifiControlPlacements: List<WifiWidgetPlacement> = WifiQuickWidgetId.defaultPlacements,
    val liveTypingMode: Boolean = true,
    val themePreset: ThemePreset = ThemePreset.RUBY,
    val useDynamicColor: Boolean = false,
    val cursorScale: Float = 1.0f,
    val cursorColorArgb: Int = 0xFFFF2A4D.toInt(),
    val cursorBorderColorArgb: Int = 0xFFFFFFFF.toInt(),
    val cursorContrastMode: CursorContrastMode = CursorContrastMode.MANUAL
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

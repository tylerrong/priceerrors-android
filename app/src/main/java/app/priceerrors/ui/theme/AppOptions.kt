package app.priceerrors.ui.theme

import androidx.compose.ui.graphics.Color

enum class PaletteOption(
    val key: String,
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val tintColor: Color,
) {
    MINT("mint", "Mint", Color(0xFF00C897), Color(0xFFFF7EB6), Color(0xFFFFC93D), Color(0xFFEAFBF5)),
    GRAPE("grape", "Grape", Color(0xFF7C4DFF), Color(0xFFFF7EB6), Color(0xFFFFC93D), Color(0xFFF4F0FF)),
    CHERRY("cherry", "Cherry", Color(0xFFFF3D6E), Color(0xFFFFC93D), Color(0xFF7C4DFF), Color(0xFFFFEFF3)),
    COBALT("cobalt", "Cobalt", Color(0xFF3D5AFF), Color(0xFFFFC93D), Color(0xFFFF7EB6), Color(0xFFEEF1FF)),
    TANGERINE("tangerine", "Tangerine", Color(0xFFFF6B35), Color(0xFF7C4DFF), Color(0xFFFFC93D), Color(0xFFFFF2EB)),
    ;

    companion object {
        fun fromKey(key: String?): PaletteOption = entries.firstOrNull { it.key == key } ?: MINT
    }
}

enum class AppearanceOption(val key: String, val displayName: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    ;

    companion object {
        fun fromKey(key: String?): AppearanceOption = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class FeedLayoutOption(val key: String, val displayName: String) {
    SCROLL("scroll", "Scroll"),
    SWIPE("swipe", "Swipe"),
    LIST("list", "List"),
    ;

    companion object {
        fun fromKey(key: String?): FeedLayoutOption = when (key) {
            // Legacy installs stored "grid"; map to the vertical scroll layout.
            "grid" -> SCROLL
            else -> entries.firstOrNull { it.key == key } ?: SCROLL
        }
    }
}

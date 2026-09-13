package app.priceerrors.feature.profile

import androidx.compose.ui.graphics.Color
import app.priceerrors.ui.theme.Mint
import app.priceerrors.ui.theme.Pink
import app.priceerrors.ui.theme.Yellow

enum class ProfileTab {
    FEED,
    ALERTS,
    BROWSE,
    PROFILE,
}

enum class ProfilePalette(
    val label: String,
    val primary: Color,
    val secondary: Color,
) {
    MINT("Mint", Mint, Pink),
    GRAPE("Grape", Color(0xFF7C4DFF), Pink),
    CHERRY("Cherry", Color(0xFFFF3D6E), Yellow),
    COBALT("Cobalt", Color(0xFF3D5AFF), Yellow),
    TANGERINE("Tangerine", Color(0xFFFF6B35), Color(0xFF7C4DFF)),
}

enum class ProfileFeedLayout(val label: String) {
    SCROLL("Scroll"),
    SWIPE("Swipe"),
    LIST("List"),
}

enum class ProfileAppearance(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

data class ProfilePost(
    val id: String,
    val title: String,
    val priceLabel: String,
    val category: String,
)

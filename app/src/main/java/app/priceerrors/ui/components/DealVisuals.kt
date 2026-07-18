package app.priceerrors.ui.components

import androidx.compose.ui.graphics.Color

data class DealVisuals(
    val background: Color,
    val accent: Color,
    val emoji: String,
)

fun dealVisuals(category: String, darkTheme: Boolean): DealVisuals =
    when (category.trim().lowercase()) {
        "tech" -> DealVisuals(Color(0xFFE8F4FD), if (darkTheme) Color(0xFF8FA3FF) else Color(0xFF3D5AFF), "🎧")
        "fashion" -> DealVisuals(Color(0xFFFFF0F5), if (darkTheme) Color(0xFFFF7EA0) else Color(0xFFFF3D6E), "👟")
        "food" -> DealVisuals(Color(0xFFE8F5E9), if (darkTheme) Color(0xFF7ED982) else Color(0xFF4CAF50), "🍿")
        "beauty" -> DealVisuals(Color(0xFFFFF0E5), if (darkTheme) Color(0xFFFF9066) else Color(0xFFFF6B35), "✨")
        "travel" -> DealVisuals(Color(0xFFE3F2FD), if (darkTheme) Color(0xFF8FA3FF) else Color(0xFF2D5BFF), "✈️")
        "events", "event" -> DealVisuals(Color(0xFFFCE4EC), if (darkTheme) Color(0xFFFF6B9D) else Color(0xFFE91E63), "🎟️")
        "amazon" -> DealVisuals(Color(0xFFFFF3E0), if (darkTheme) Color(0xFFFFB340) else Color(0xFFFF9900), "📦")
        else -> DealVisuals(
            background = if (darkTheme) Color(0xFF292929) else Color(0xFFF7F5F0),
            accent = if (darkTheme) Color(0xFFEDEDED) else Color(0xFF0A0A0A),
            emoji = "🏷️",
        )
    }

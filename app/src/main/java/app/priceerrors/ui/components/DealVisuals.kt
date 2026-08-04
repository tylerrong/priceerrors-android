package app.priceerrors.ui.components

import androidx.compose.ui.graphics.Color
import kotlin.math.min

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
        // Yellow to match the iOS palette. As on iOS the light-mode value is
        // pulled down from #FFC93D so it stays legible as text, not just as fill.
        "gaming" -> DealVisuals(Color(0xFFFFF8E1), if (darkTheme) Color(0xFFFFD75E) else Color(0xFFB8860B), "🕹️")
        "amazon" -> DealVisuals(Color(0xFFFFF3E0), if (darkTheme) Color(0xFFFFB340) else Color(0xFFFF9900), "📦")
        else -> DealVisuals(
            background = if (darkTheme) Color(0xFF292929) else Color(0xFFF7F5F0),
            accent = if (darkTheme) Color(0xFFEDEDED) else Color(0xFF0A0A0A),
            emoji = "🏷️",
        )
    }

/**
 * Same hue, nudged to stay readable as text on a tinted chip in either
 * appearance. The category palette spans very light (Gaming yellow) to very
 * dark (Fashion crimson), so a single fixed color can't work for both: pale
 * hues vanish on the light tint, dark hues vanish on the dark card. Scale
 * toward a target luminance instead, which preserves the hue.
 *
 * The targets match the iOS client so both platforms clear WCAG AA (4.5:1)
 * against the 14% tint chip.
 */
fun legibleOnCard(color: Color, darkTheme: Boolean): Color {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return if (darkTheme) {
        if (luminance >= 0.72f) return color
        val factor = 0.72f / maxOf(luminance, 0.01f)
        Color(
            red = min(1f, color.red * factor),
            green = min(1f, color.green * factor),
            blue = min(1f, color.blue * factor),
            alpha = color.alpha,
        )
    } else {
        if (luminance <= 0.30f) return color
        val factor = 0.30f / luminance
        Color(
            red = color.red * factor,
            green = color.green * factor,
            blue = color.blue * factor,
            alpha = color.alpha,
        )
    }
}

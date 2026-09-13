package app.priceerrors.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DealVisualsTest {

    private fun luminance(color: Color) =
        0.299f * color.red + 0.587f * color.green + 0.114f * color.blue

    /** Contrast of two opaque colors, per WCAG 2.1. */
    private fun contrastRatio(foreground: Color, background: Color): Float {
        fun relative(color: Color): Float {
            fun channel(value: Float) =
                if (value <= 0.03928f) value / 12.92f
                else Math.pow(((value + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
            return 0.2126f * channel(color.red) +
                0.7152f * channel(color.green) +
                0.0722f * channel(color.blue)
        }
        val lighter = maxOf(relative(foreground), relative(background))
        val darker = minOf(relative(foreground), relative(background))
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    /** The 14% category tint composited over the card it sits on. */
    private fun chipBackground(accent: Color, card: Color): Color = Color(
        red = accent.red * 0.14f + card.red * 0.86f,
        green = accent.green * 0.14f + card.green * 0.86f,
        blue = accent.blue * 0.14f + card.blue * 0.86f,
    )

    private val categories =
        listOf("tech", "fashion", "food", "beauty", "travel", "events", "gaming", "amazon", "other")

    @Test
    fun `gaming has its own palette entry rather than the fallback`() {
        val gaming = dealVisuals("gaming", darkTheme = false)
        val fallback = dealVisuals("nonsense", darkTheme = false)
        assertTrue(gaming.accent != fallback.accent)
        assertEquals("🕹️", gaming.emoji)
    }

    @Test
    fun `category lookup ignores case and surrounding space`() {
        assertEquals(dealVisuals("gaming", false), dealVisuals("  Gaming ", false))
    }

    @Test
    fun `image stages use the saturated iOS category palette`() {
        val expected = mapOf(
            "tech" to Color(0xFF2D5BFF),
            "fashion" to Color(0xFF7C4DFF),
            "food" to Color(0xFF4CAF50),
            "beauty" to Color(0xFFFF7EB6),
            "travel" to Color(0xFF00B4D8),
            "events" to Color(0xFFE91E63),
            "gaming" to Color(0xFFFFC93D),
            "amazon" to Color(0xFFFF8A3D),
            "other" to Color(0xFF00C897),
        )

        expected.forEach { (category, color) ->
            assertEquals(category, color, dealVisuals(category, darkTheme = false).background)
            assertEquals(category, color, dealVisuals(category, darkTheme = true).background)
        }
    }

    @Test
    fun `every category tag clears WCAG AA on its tinted chip`() {
        val lightCard = Color(0xFFFFFFFF)
        val darkCard = Color(0xFF1E1E1E)
        for (category in categories) {
            for (darkTheme in listOf(false, true)) {
                val accent = dealVisuals(category, darkTheme).accent
                val text = legibleOnCard(accent, darkTheme)
                val chip = chipBackground(accent, if (darkTheme) darkCard else lightCard)
                val ratio = contrastRatio(text, chip)
                assertTrue(
                    "$category (darkTheme=$darkTheme) contrast was $ratio",
                    ratio >= 4.5f,
                )
            }
        }
    }

    @Test
    fun `legibility scaling preserves hue and leaves in-range colors alone`() {
        // Already dark enough for a light chip — must pass through untouched.
        val darkEnough = Color(0xFF3D5AFF)
        if (luminance(darkEnough) <= 0.30f) {
            assertEquals(darkEnough, legibleOnCard(darkEnough, darkTheme = false))
        }

        // Pale yellow on a light chip gets pulled down, keeping red > green > blue.
        val paleYellow = Color(0xFFFFC93D)
        val adjusted = legibleOnCard(paleYellow, darkTheme = false)
        assertTrue(luminance(adjusted) < luminance(paleYellow))
        assertTrue(adjusted.red > adjusted.green)
        assertTrue(adjusted.green > adjusted.blue)
    }
}

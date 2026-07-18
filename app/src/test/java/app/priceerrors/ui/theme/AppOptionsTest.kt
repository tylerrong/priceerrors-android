package app.priceerrors.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AppOptionsTest {
    @Test
    fun `unknown persisted keys safely fall back to defaults`() {
        assertEquals(PaletteOption.MINT, PaletteOption.fromKey("unknown"))
        assertEquals(AppearanceOption.SYSTEM, AppearanceOption.fromKey(null))
        assertEquals(FeedLayoutOption.SWIPE, FeedLayoutOption.fromKey(""))
    }

    @Test
    fun `known persisted keys restore exact choices`() {
        assertEquals(PaletteOption.COBALT, PaletteOption.fromKey("cobalt"))
        assertEquals(AppearanceOption.DARK, AppearanceOption.fromKey("dark"))
        assertEquals(FeedLayoutOption.GRID, FeedLayoutOption.fromKey("grid"))
    }
}

package app.priceerrors.feature.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileHelpersTest {
    @Test
    fun `initials handle full single and blank names`() {
        assertEquals("TR", profileInitials("Tyler Rong"))
        assertEquals("TY", profileInitials("Tyler"))
        assertEquals("ME", profileInitials("   "))
    }

    @Test
    fun `savings use US currency and never render negative values`() {
        assertEquals("$1,234.50", formatSavings(1234.5))
        assertEquals("$0.00", formatSavings(-10.0))
    }
}

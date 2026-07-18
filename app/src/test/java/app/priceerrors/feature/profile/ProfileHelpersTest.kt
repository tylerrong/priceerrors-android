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
}

package app.priceerrors.ui.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilitySupportTest {
    @Test
    fun `zero animator scale disables decorative motion`() {
        assertFalse(animationsEnabled(0f))
        assertTrue(animationsEnabled(0.5f))
        assertTrue(animationsEnabled(1f))
    }
}

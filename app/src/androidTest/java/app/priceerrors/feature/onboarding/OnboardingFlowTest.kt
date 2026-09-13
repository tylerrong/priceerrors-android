package app.priceerrors.feature.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.priceerrors.ui.accessibility.PriceErrorsTestTags
import app.priceerrors.ui.theme.PriceErrorsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingFlowTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun twoProductSlidesAdvanceInOrder() {
        var completed = false
        compose.mainClock.autoAdvance = false
        compose.setContent {
            PriceErrorsTheme {
                OnboardingScreen(onContinue = { completed = true })
            }
        }
        compose.mainClock.advanceTimeBy(2_000)

        compose.onNodeWithText("Save").assertIsDisplayed()
        compose.onNodeWithContentDescription("Page 1 of 6").assertIsDisplayed()
        compose.onNodeWithTag(PriceErrorsTestTags.ONBOARDING_CONTINUE).performClick()
        compose.mainClock.advanceTimeBy(2_000)

        compose.onNodeWithText("Price glitches happen\nall the time.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Page 2 of 6").assertIsDisplayed()
        compose.onNodeWithTag(PriceErrorsTestTags.ONBOARDING_CONTINUE).performClick()

        compose.runOnIdle { assertTrue(completed) }
    }

    @Test
    fun allSelectionReplacesIndividualCategories() {
        var submitted: Set<String>? = null
        compose.setContent {
            PriceErrorsTheme {
                PreferencesScreen(onContinue = { submitted = it })
            }
        }

        compose.onNodeWithTag(PriceErrorsTestTags.PREFERENCES_CONTINUE).assertIsNotEnabled()
        compose.onNodeWithTag("preference_tech").performClick().assertIsSelected()
        compose.onNodeWithTag("preference_all").performClick().assertIsSelected()
        compose.onNodeWithTag("preference_tech").assertIsNotSelected()
        compose.onNodeWithText("All deal alerts ready").assertIsDisplayed()
        compose.onNodeWithTag(PriceErrorsTestTags.PREFERENCES_CONTINUE).performClick()

        compose.runOnIdle { assertEquals(setOf("All"), submitted) }
    }

    @Test
    fun notificationPrePromptRequestsBeforeContinuing() {
        var requested = false
        var completed = false
        compose.setContent {
            PriceErrorsTheme {
                NotificationPermissionStep(
                    onEnableAlerts = { requested = true },
                    onContinue = { completed = true },
                )
            }
        }

        compose.onNodeWithText("Turn on deal alerts").assertIsDisplayed()
        compose.onNodeWithTag("notification_enable").performClick()

        compose.runOnIdle {
            assertTrue(requested)
            assertTrue(completed)
        }
    }
}

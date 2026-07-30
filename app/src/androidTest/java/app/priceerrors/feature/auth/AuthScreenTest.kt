package app.priceerrors.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.priceerrors.ui.accessibility.PriceErrorsTestTags
import app.priceerrors.ui.theme.PriceErrorsTheme
import org.junit.Rule
import org.junit.Test

class AuthScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun invalidEmailShowsAccessibleError() {
        compose.setContent {
            PriceErrorsTheme {
                AuthScreen(onAuthenticated = { _, _ -> })
            }
        }

        compose.onNodeWithContentDescription("Full name").performTextInput("Test User")
        compose.onNodeWithContentDescription("Email").performTextInput("invalid")
        compose.onNodeWithContentDescription("Password").performTextInput("secret1")
        compose.onNodeWithTag(PriceErrorsTestTags.AUTH_SUBMIT).performClick()

        compose.onNodeWithText("Enter a valid email address.").assertIsDisplayed()
    }

    @Test
    fun googleAuthenticationUsesTheGoogleLogoInsteadOfATextGlyph() {
        compose.setContent {
            PriceErrorsTheme {
                AuthScreen(onAuthenticated = { _, _ -> })
            }
        }

        compose.onNodeWithTag(
            PriceErrorsTestTags.GOOGLE_AUTH_LOGO,
            useUnmergedTree = true,
        ).assertIsDisplayed()
        compose.onAllNodesWithText("G", useUnmergedTree = true).assertCountEquals(0)
    }
}

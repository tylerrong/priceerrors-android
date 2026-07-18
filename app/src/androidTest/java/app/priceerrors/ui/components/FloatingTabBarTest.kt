package app.priceerrors.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.priceerrors.navigation.MainTab
import app.priceerrors.ui.accessibility.PriceErrorsTestTags
import app.priceerrors.ui.theme.PriceErrorsTheme
import org.junit.Rule
import org.junit.Test

class FloatingTabBarTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun browseTabExposesAndUpdatesSelection() {
        var selected by mutableStateOf(MainTab.FEED)
        compose.setContent {
            PriceErrorsTheme {
                FloatingTabBar(
                    selectedTab = selected,
                    onTabSelected = { selected = it },
                )
            }
        }

        compose.onNodeWithTag(PriceErrorsTestTags.BROWSE_TAB).performClick()
        compose.onNodeWithTag(PriceErrorsTestTags.BROWSE_TAB).assertIsSelected()
    }
}

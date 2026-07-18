package app.priceerrors.feature.feed

import app.priceerrors.MainDispatcherRule
import app.priceerrors.core.data.FakeDealData
import app.priceerrors.core.data.FakeDealRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state receives fake deals`() = runTest {
        val viewModel = FeedViewModel(FakeDealRepository())

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(FakeDealData.deals, viewModel.uiState.value.deals)
    }

    @Test
    fun `selection uses canonical backend id and back clears it`() = runTest {
        val viewModel = FeedViewModel(FakeDealRepository())
        advanceUntilIdle()
        val canonicalId = FakeDealData.deals.first { it.category == "Tech" }.id

        viewModel.selectDeal(canonicalId)

        assertEquals(canonicalId, viewModel.uiState.value.selectedDealId)
        assertEquals(canonicalId, viewModel.uiState.value.selectedDeal?.id)

        viewModel.clearSelection()

        assertNull(viewModel.uiState.value.selectedDealId)
    }

    @Test
    fun `unknown and source ids cannot open a detail screen`() = runTest {
        val viewModel = FeedViewModel(FakeDealRepository())
        advanceUntilIdle()
        val sourceId = FakeDealData.deals.first().sourceId

        viewModel.selectDeal(sourceId)
        viewModel.selectDeal("missing-deal")

        assertNull(viewModel.uiState.value.selectedDealId)
    }

    @Test
    fun `refresh failure becomes a retryable message`() = runTest {
        val repository = FakeDealRepository(
            refreshResult = Result.failure(IllegalStateException("Network unavailable")),
        )
        val viewModel = FeedViewModel(repository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals("Network unavailable", viewModel.uiState.value.errorMessage)
    }
}

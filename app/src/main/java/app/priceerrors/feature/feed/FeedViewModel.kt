package app.priceerrors.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.priceerrors.core.data.DealRepository
import app.priceerrors.core.model.Deal
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val deals: List<Deal> = emptyList(),
    val totalDealCount: Int = 0,
    val selectedDealId: String? = null,
    val errorMessage: String? = null,
    val lastRefreshed: Instant? = null,
) {
    val selectedDeal: Deal?
        get() = selectedDealId?.let { id -> deals.firstOrNull { deal -> deal.id == id } }
}

class FeedViewModel(
    private val repository: DealRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository
                .observeFeed()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to load deals.",
                        )
                    }
                }
                .collect { deals ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            deals = deals,
                            totalDealCount = maxOf(current.totalDealCount, deals.size),
                            selectedDealId = current.selectedDealId?.takeIf { selectedId ->
                                deals.any { deal -> deal.id == selectedId }
                            },
                        )
                    }
                }
        }

        viewModelScope.launch {
            repository.observeMetadata().collect { metadata ->
                if (metadata != null) {
                    _uiState.update { current ->
                        current.copy(
                            totalDealCount = maxOf(metadata.total, current.deals.size),
                        )
                    }
                }
            }
        }

        loadInitialFeed()
    }

    /**
     * Pulls the first page on creation. A network-backed repository starts
     * empty, so without this the feed would sit blank until the user pulled to
     * refresh.
     */
    private fun loadInitialFeed() {
        viewModelScope.launch {
            repository.refresh().onSuccess {
                _uiState.update { current -> current.copy(lastRefreshed = Instant.now()) }
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to load deals.",
                    )
                }
            }
            _uiState.update { current -> current.copy(isLoading = false) }
        }
    }

    fun selectDeal(id: String) {
        _uiState.update { current ->
            if (current.deals.any { deal -> deal.id == id }) {
                current.copy(selectedDealId = id)
            } else {
                current
            }
        }
    }

    fun clearSelection() {
        _uiState.update { current -> current.copy(selectedDealId = null) }
    }

    fun refresh(forceFull: Boolean = false) {
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            val hadDeals = _uiState.value.deals.isNotEmpty()
            _uiState.update { current ->
                current.copy(
                    isRefreshing = true,
                    isLoading = !hadDeals,
                    errorMessage = null,
                )
            }

            repository.refresh(forceFull = forceFull).fold(
                onSuccess = {
                    _uiState.update { current ->
                        current.copy(
                            isRefreshing = false,
                            isLoading = false,
                            lastRefreshed = Instant.now(),
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { current ->
                        current.copy(
                            isRefreshing = false,
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to refresh deals.",
                        )
                    }
                },
            )
        }
    }

    class Factory(
        private val repository: DealRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
                return FeedViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

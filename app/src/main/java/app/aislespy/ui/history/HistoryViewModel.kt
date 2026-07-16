package app.aislespy.ui.history

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.aislespy.AisleSpyApp
import app.aislespy.data.local.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * History list: newest first, delete one / clear all (UI_UX.md history screen).
 */
class HistoryViewModel(
    private val historyRepository: HistoryRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val showClearConfirm = MutableStateFlow(false)

    val uiState: StateFlow<HistoryUiState> = combine(
        historyRepository.observeLatestFirst(),
        showClearConfirm,
    ) { entries, confirm ->
        val now = clock()
        val items = entries.map { it.toHistoryItemUi(now) }
        HistoryUiState(
            items = items,
            empty = items.isEmpty(),
            showClearConfirm = confirm,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(),
    )

    fun delete(barcode: String) {
        viewModelScope.launch {
            historyRepository.deleteByBarcode(barcode)
        }
    }

    fun requestClearAll() {
        showClearConfirm.value = true
    }

    fun dismissClearConfirm() {
        showClearConfirm.value = false
    }

    fun confirmClearAll() {
        viewModelScope.launch {
            historyRepository.clearAll()
            showClearConfirm.value = false
        }
    }

    class Factory(
        private val application: Application,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            val container = (application as AisleSpyApp).container
            return HistoryViewModel(
                historyRepository = container.historyRepository,
                clock = container.clock,
            ) as T
        }
    }
}

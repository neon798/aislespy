package app.aislespy.ui.scan

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.aislespy.AisleSpyApp
import app.aislespy.data.local.HistoryRepository
import app.aislespy.ui.history.HistoryItemUi
import app.aislespy.ui.history.toHistoryItemUi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Camera permission presentation for the scan screen (DOMAIN_MODELS / UI_UX). */
enum class CameraPermission {
    /** Not granted yet; show rationale + grant CTA. */
    Rationale,

    /** User denied; keep rationale + manual entry, do not auto-reprompt. */
    Denied,

    /** Granted; show CameraX preview. */
    Granted,
}

/**
 * Scan screen state (DOMAIN_MODELS.md [ScanUiState]).
 * [recent] is capped at [HistoryRepository.RECENT_LIMIT] (10).
 */
data class ScanUiState(
    val permission: CameraPermission = CameraPermission.Rationale,
    val cameraActive: Boolean = false,
    val torchEnabled: Boolean = false,
    val lastError: String? = null,
    val recent: List<HistoryItemUi> = emptyList(),
)

sealed interface ScanEvent {
    data class NavigateToResult(val barcode: String) : ScanEvent
}

/**
 * Scan screen state + one-shot navigation events.
 *
 * Debounce/stability lives in [ScanDebouncer] (shared with the analyzer);
 * this VM only reacts to accepted codes and permission/torch UI.
 * Recent strip shares [HistoryRepository] with the history screen.
 */
class ScanViewModel(
    private val debouncer: ScanDebouncer = ScanDebouncer(),
    historyRepository: HistoryRepository? = null,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val permissionState = MutableStateFlow(
        ScanUiState(
            permission = CameraPermission.Rationale,
            cameraActive = false,
            torchEnabled = false,
            lastError = null,
            recent = emptyList(),
        ),
    )

    private val recentFlow = historyRepository
        ?.observeRecent(HistoryRepository.RECENT_LIMIT)
        ?: flowOf(emptyList())

    val uiState: StateFlow<ScanUiState> = combine(
        permissionState,
        recentFlow,
    ) { base, entries ->
        val now = clock()
        base.copy(recent = entries.map { it.toHistoryItemUi(now) })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScanUiState(),
    )

    private val _events = MutableSharedFlow<ScanEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ScanEvent> = _events.asSharedFlow()

    /** Exposed so [BarcodeAnalyzer] and the VM share one debounce instance. */
    fun debouncer(): ScanDebouncer = debouncer

    fun onPermissionStatus(granted: Boolean, fromUserRequest: Boolean = false) {
        if (granted) {
            permissionState.update {
                it.copy(
                    permission = CameraPermission.Granted,
                    cameraActive = true,
                    lastError = null,
                )
            }
        } else {
            // First open without permission → Rationale; after an explicit deny → Denied.
            val next = if (fromUserRequest) {
                CameraPermission.Denied
            } else {
                CameraPermission.Rationale
            }
            permissionState.update {
                it.copy(
                    permission = next,
                    cameraActive = false,
                    torchEnabled = false,
                )
            }
        }
    }

    fun toggleTorch() {
        permissionState.update {
            if (it.permission != CameraPermission.Granted) it
            else it.copy(torchEnabled = !it.torchEnabled)
        }
    }

    fun setTorch(enabled: Boolean) {
        permissionState.update {
            it.copy(torchEnabled = enabled && it.permission == CameraPermission.Granted)
        }
    }

    /**
     * Called only when [ScanDebouncer] accepts a decode — emits a single navigation event.
     */
    fun onBarcodeAccepted(barcode: String) {
        viewModelScope.launch {
            _events.emit(ScanEvent.NavigateToResult(barcode))
        }
    }

    fun reportCameraError(message: String) {
        permissionState.update { it.copy(lastError = message) }
    }

    class Factory(
        private val application: Application,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ScanViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            val container = (application as AisleSpyApp).container
            return ScanViewModel(
                historyRepository = container.historyRepository,
                clock = container.clock,
            ) as T
        }
    }
}

package app.aislespy.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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

data class ScanUiState(
    val permission: CameraPermission = CameraPermission.Rationale,
    val cameraActive: Boolean = false,
    val torchEnabled: Boolean = false,
    val lastError: String? = null,
    // recent: List<HistoryItemUi> — wired when history (T-500) lands
)

sealed interface ScanEvent {
    data class NavigateToResult(val barcode: String) : ScanEvent
}

/**
 * Scan screen state + one-shot navigation events.
 *
 * Debounce/stability lives in [ScanDebouncer] (shared with the analyzer);
 * this VM only reacts to accepted codes and permission/torch UI.
 */
class ScanViewModel(
    private val debouncer: ScanDebouncer = ScanDebouncer(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ScanEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ScanEvent> = _events.asSharedFlow()

    /** Exposed so [BarcodeAnalyzer] and the VM share one debounce instance. */
    fun debouncer(): ScanDebouncer = debouncer

    fun onPermissionStatus(granted: Boolean, fromUserRequest: Boolean = false) {
        if (granted) {
            _uiState.update {
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
            _uiState.update {
                it.copy(
                    permission = next,
                    cameraActive = false,
                    torchEnabled = false,
                )
            }
        }
    }

    fun toggleTorch() {
        _uiState.update {
            if (it.permission != CameraPermission.Granted) it
            else it.copy(torchEnabled = !it.torchEnabled)
        }
    }

    fun setTorch(enabled: Boolean) {
        _uiState.update { it.copy(torchEnabled = enabled && it.permission == CameraPermission.Granted) }
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
        _uiState.update { it.copy(lastError = message) }
    }
}

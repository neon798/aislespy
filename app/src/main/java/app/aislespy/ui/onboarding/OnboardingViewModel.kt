package app.aislespy.ui.onboarding

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.aislespy.AisleSpyApp
import app.aislespy.data.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * First-launch onboarding (T-510). Completing persists [UserPrefs.setFirstLaunchDone]
 * then signals the UI to navigate to scan.
 */
class OnboardingViewModel(
    private val userPrefs: UserPrefs,
) : ViewModel() {

    private val _events = MutableSharedFlow<OnboardingEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    fun onStartScanning() {
        viewModelScope.launch {
            userPrefs.setFirstLaunchDone()
            _events.emit(OnboardingEvent.NavigateToScan)
        }
    }

    class Factory(
        private val application: Application,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            val container = (application as AisleSpyApp).container
            return OnboardingViewModel(userPrefs = container.userPrefs) as T
        }
    }
}

sealed class OnboardingEvent {
    data object NavigateToScan : OnboardingEvent()
}

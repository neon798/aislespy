package app.aislespy.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.aislespy.AisleSpyApp
import app.aislespy.BuildConfig
import app.aislespy.domain.ScoringConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Exposes version / privacy summary for the settings hub (T-510).
 *
 * Versions are injected at construction so unit tests can assert container inputs without Android.
 */
class SettingsViewModel(
    appVersion: String,
    methodologyVersion: String,
    knowledgePackVersion: String,
    privacySummary: String = DEFAULT_PRIVACY_SUMMARY,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            appVersion = appVersion,
            methodologyVersion = methodologyVersion,
            knowledgePackVersion = knowledgePackVersion,
            privacySummary = privacySummary,
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    class Factory(
        private val application: Application,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            val container = (application as AisleSpyApp).container
            val foodVersion = container.foodKnowledgePack.version
            val beautyVersion = container.beautyKnowledgePack.version
            return SettingsViewModel(
                appVersion = BuildConfig.VERSION_NAME,
                methodologyVersion = ScoringConfig.METHODOLOGY_VERSION,
                knowledgePackVersion = formatKnowledgePackVersions(foodVersion, beautyVersion),
                privacySummary = DEFAULT_PRIVACY_SUMMARY,
            ) as T
        }
    }

    companion object {
        /** One-line privacy blurb for settings (PRIVACY.md short version). */
        const val DEFAULT_PRIVACY_SUMMARY: String =
            "No accounts. No tracking. History stays on this device. " +
                "Product lookups go only to Open Food Facts and Open Beauty Facts."

        fun formatKnowledgePackVersions(foodVersion: String, beautyVersion: String): String =
            "food $foodVersion · beauty $beautyVersion"
    }
}

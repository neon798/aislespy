package app.aislespy.ui.settings

import app.aislespy.domain.ScoringConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SettingsViewModel exposes versions from constructor / container inputs (T-510).
 */
class SettingsViewModelTest {

    @Test
    fun uiState_populatedFromConstructorInputs() {
        val appVersion = "0.1.0-test"
        val methodologyVersion = ScoringConfig.METHODOLOGY_VERSION
        val knowledgePackVersion = SettingsViewModel.formatKnowledgePackVersions("1.0.0", "1.2.0")
        val privacySummary = "No accounts. No tracking."

        val vm = SettingsViewModel(
            appVersion = appVersion,
            methodologyVersion = methodologyVersion,
            knowledgePackVersion = knowledgePackVersion,
            privacySummary = privacySummary,
        )

        val state = vm.uiState.value
        assertEquals(appVersion, state.appVersion)
        assertEquals(methodologyVersion, state.methodologyVersion)
        assertEquals("food 1.0.0 · beauty 1.2.0", state.knowledgePackVersion)
        assertEquals(privacySummary, state.privacySummary)
        assertTrue(state.methodologyVersion.isNotBlank())
    }

    @Test
    fun formatKnowledgePackVersions_includesFoodAndBeauty() {
        val formatted = SettingsViewModel.formatKnowledgePackVersions("2.0.0", "3.1.0")
        assertEquals("food 2.0.0 · beauty 3.1.0", formatted)
    }

    @Test
    fun defaultPrivacySummary_mentionsNoAccountsAndOffObf() {
        val summary = SettingsViewModel.DEFAULT_PRIVACY_SUMMARY.lowercase()
        assertTrue(summary.contains("no accounts"))
        assertTrue(summary.contains("open food facts"))
        assertTrue(summary.contains("open beauty facts"))
    }
}

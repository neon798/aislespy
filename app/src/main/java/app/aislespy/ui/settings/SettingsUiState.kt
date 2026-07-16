package app.aislespy.ui.settings

/**
 * Settings screen state (DOMAIN_MODELS.md [SettingsUiState]).
 */
data class SettingsUiState(
    val appVersion: String = "",
    val methodologyVersion: String = "",
    val knowledgePackVersion: String = "",
    val privacySummary: String = "",
)

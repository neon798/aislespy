package app.aislespy.ui.ingredient

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.aislespy.AisleSpyApp
import app.aislespy.data.knowledge.KnowledgePack
import app.aislespy.domain.knowledge.IngredientHit
import app.aislespy.domain.knowledge.IngredientSearch
import app.aislespy.ui.result.ConcernDetailStore
import app.aislespy.ui.result.IngredientDetailUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * UI state for the Ingredients lookup tab (ADR-023).
 */
data class IngredientLookupUiState(
    val query: String = "",
    val results: List<IngredientHit> = emptyList(),
) {
    /** True when the user has not typed a non-blank query yet. */
    val isPrompt: Boolean get() = query.isBlank()

    /** True when a non-blank query produced zero hits. */
    val isNoResults: Boolean get() = query.isNotBlank() && results.isEmpty()
}

/**
 * Cross-pack ingredient search ViewModel. Holds both knowledge packs from
 * [app.aislespy.di.AppContainer] and recomputes results on each query change
 * (packs are small; no debounce required).
 */
class IngredientLookupViewModel(
    private val foodPack: KnowledgePack,
    private val beautyPack: KnowledgePack,
    private val concernDetailStore: ConcernDetailStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IngredientLookupUiState())
    val uiState: StateFlow<IngredientLookupUiState> = _uiState.asStateFlow()

    fun onQueryChange(query: String) {
        val results = IngredientSearch.search(query, foodPack, beautyPack)
        _uiState.update {
            IngredientLookupUiState(query = query, results = results)
        }
    }

    /**
     * Publish the hit into [ConcernDetailStore] and return the concern id for navigation.
     * Maps pack entry → [IngredientDetailUi] (title as name; no product position).
     */
    fun selectHit(hit: IngredientHit): String {
        val entry = hit.entry
        concernDetailStore.put(
            IngredientDetailUi(
                id = entry.id,
                name = entry.title,
                severity = entry.severity,
                fullWhy = entry.why,
                sources = entry.sources,
                positionHint = null,
            ),
        )
        return entry.id
    }

    class Factory(
        private val application: Application,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(IngredientLookupViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            val container = (application as AisleSpyApp).container
            return IngredientLookupViewModel(
                foodPack = container.foodKnowledgePack,
                beautyPack = container.beautyKnowledgePack,
                concernDetailStore = container.concernDetailStore,
            ) as T
        }
    }
}

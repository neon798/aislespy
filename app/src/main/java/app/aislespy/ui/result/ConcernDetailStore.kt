package app.aislespy.ui.result

import app.aislespy.domain.model.Concern
import app.aislespy.domain.model.ScoreResult

/**
 * Simple process-local holder for the last score result / concerns so the
 * ingredient detail route can resolve full why + sources without complex
 * nav-arg encoding.
 *
 * Scoped via [app.aislespy.di.AppContainer] (application lifetime).
 * Thread-safe for main-thread UI + Default-dispatcher scoring writes.
 */
class ConcernDetailStore {
    @Volatile
    private var byId: Map<String, IngredientDetailUi> = emptyMap()

    @Volatile
    var lastScoreResult: ScoreResult? = null
        private set

    fun publish(scoreResult: ScoreResult) {
        lastScoreResult = scoreResult
        byId = scoreResult.concerns.associate { it.id to it.toDetailUi() }
    }

    fun publishEmpty() {
        lastScoreResult = null
        byId = emptyMap()
    }

    fun get(concernId: String): IngredientDetailUi? = byId[concernId]

    /**
     * Merge a single detail (ingredient lookup tab) without wiping scored concerns.
     * Same id overwrites; used when navigating from the Ingredients search list.
     */
    @Synchronized
    fun put(detail: IngredientDetailUi) {
        byId = byId + (detail.id to detail)
    }

    private fun Concern.toDetailUi(): IngredientDetailUi = IngredientDetailUi(
        id = id,
        name = displayName,
        severity = severity,
        fullWhy = shortWhy,
        sources = sources,
        positionHint = positionHint,
    )
}

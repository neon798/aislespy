package app.aislespy.ui.result

import app.aislespy.domain.model.Product

/**
 * Process-local holder for an ambiguous OFF/OBF product pair so the category
 * chooser and the follow-up [result/{barcode}?source=food|beauty] screen can
 * share already-fetched products without a second network round-trip.
 *
 * Scoped via [app.aislespy.di.AppContainer] (application lifetime), same pattern
 * as [ConcernDetailStore]. Empty after process death — callers must refetch.
 */
class ChoicePairStore {

    data class Pair(
        val barcode: String,
        val food: Product,
        val beauty: Product,
    )

    @Volatile
    private var current: Pair? = null

    fun put(barcode: String, food: Product, beauty: Product) {
        current = Pair(barcode = barcode, food = food, beauty = beauty)
    }

    /** Returns the pair only when it matches [barcode]. */
    fun get(barcode: String): Pair? = current?.takeIf { it.barcode == barcode }

    fun clear() {
        current = null
    }
}

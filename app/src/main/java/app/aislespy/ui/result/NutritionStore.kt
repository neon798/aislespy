package app.aislespy.ui.result

import app.aislespy.domain.model.Product

/**
 * Process-local holder for nutrition display data so the nutrition route can
 * show Nutri-Score + nutriments without complex nav-arg encoding.
 *
 * Scoped via [app.aislespy.di.AppContainer] (application lifetime), same pattern
 * as [ConcernDetailStore]. Nutrition is informational only (ADR-018).
 */
class NutritionStore {
    @Volatile
    private var current: NutritionUi? = null

    @Volatile
    private var barcode: String? = null

    fun publish(product: Product) {
        barcode = product.barcode
        current = NutritionUi.from(product)
    }

    fun publishEmpty() {
        barcode = null
        current = null
    }

    fun get(barcode: String): NutritionUi? {
        if (this.barcode != barcode) return null
        return current
    }

    fun get(): NutritionUi? = current
}

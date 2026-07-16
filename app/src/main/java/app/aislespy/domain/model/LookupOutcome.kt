package app.aislespy.domain.model

/**
 * Repository-level outcome of dual OFF/OBF product lookup.
 * See DOMAIN_MODELS.md and API_CONTRACTS.md parallel algorithm.
 */
sealed class LookupOutcome {
    data class Found(val product: Product) : LookupOutcome()

    data class NeedsCategoryChoice(
        val food: Product,
        val beauty: Product,
    ) : LookupOutcome()

    data class NotFound(val barcode: String) : LookupOutcome()

    data class NetworkError(
        val message: String,
        val barcode: String,
    ) : LookupOutcome()
}

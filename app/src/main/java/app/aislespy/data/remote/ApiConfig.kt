package app.aislespy.data.remote

/**
 * Central network configuration for Open Food Facts / Open Beauty Facts.
 * Field list and timeouts match [docs/API_CONTRACTS.md].
 */
object ApiConfig {
    const val OFF_BASE_URL = "https://world.openfoodfacts.org"
    const val OBF_BASE_URL = "https://world.openbeautyfacts.org"

    /**
     * Reduced field filter always requested on product lookups.
     * Must stay in sync with API_CONTRACTS.md.
     */
    const val FIELDS =
        "code,product_name,brands,image_front_url,image_front_small_url," +
            "nutriscore_grade,nutriscore_score,nova_group,additives_tags," +
            "ingredients_text,ingredients_tags,allergens_tags,labels_tags," +
            "categories_tags,nutriments"

    const val CONNECT_TIMEOUT_SECONDS = 10L
    const val READ_TIMEOUT_SECONDS = 20L

    /**
     * Product cache TTL (API_CONTRACTS.md caching policy).
     * Fresh cache hits skip network; expired rows are ignored and may be purged.
     */
    const val PRODUCT_CACHE_TTL_MS: Long = 7L * 24 * 60 * 60 * 1000

    private const val USER_AGENT_REPO = "https://github.com/neon798/aislespy"

    /** Template: `AisleSpy/<version> (Android; https://github.com/neon798/aislespy)` */
    fun userAgent(versionName: String): String =
        "AisleSpy/$versionName (Android; $USER_AGENT_REPO)"

    /**
     * Contribute / search URLs for products not in OFF/OBF (API_CONTRACTS.md).
     * Open in browser via Intent.ACTION_VIEW from the NotFound result state.
     */
    fun contributeFoodUrl(barcode: String): String =
        "$OFF_BASE_URL/cgi/product.pl?type=search&code=$barcode"

    fun contributeBeautyUrl(barcode: String): String =
        "$OBF_BASE_URL/cgi/product.pl?type=search&code=$barcode"
}

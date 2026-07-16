package app.aislespy.data.remote

import app.aislespy.data.remote.dto.ProductResponseDto

/**
 * Raw per-database outcome of a single OFF or OBF product request.
 * Dual-DB combination lives in ProductRepository (T-220).
 */
sealed class ProductApiResult {
    data class Found(val dto: ProductResponseDto) : ProductApiResult()
    data object NotFound : ProductApiResult()
    data class Error(val message: String) : ProductApiResult()
}

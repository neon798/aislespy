package app.aislespy.data.remote

import app.aislespy.data.remote.dto.ProductResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Open Food Facts v2 product API. */
interface OffApi {
    @GET("/api/v2/product/{barcode}")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = ApiConfig.FIELDS,
    ): Response<ProductResponseDto>
}

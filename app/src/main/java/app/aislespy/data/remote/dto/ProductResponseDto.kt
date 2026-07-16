package app.aislespy.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OFF/OBF v2 product response body (filtered fields).
 * Configured with [ignoreUnknownKeys] at the Json instance level.
 */
@Serializable
data class ProductResponseDto(
    val status: Int,
    val code: String? = null,
    val product: ProductDto? = null,
)

@Serializable
data class ProductDto(
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null,
    @SerialName("image_front_url") val imageFrontUrl: String? = null,
    @SerialName("image_front_small_url") val imageFrontSmallUrl: String? = null,
    @SerialName("nutriscore_grade") val nutriscoreGrade: String? = null,
    @SerialName("nutriscore_score") val nutriscoreScore: Int? = null,
    @SerialName("nova_group") val novaGroup: Int? = null,
    @SerialName("additives_tags") val additivesTags: List<String>? = null,
    @SerialName("ingredients_text") val ingredientsText: String? = null,
    @SerialName("ingredients_tags") val ingredientsTags: List<String>? = null,
    @SerialName("allergens_tags") val allergensTags: List<String>? = null,
    @SerialName("labels_tags") val labelsTags: List<String>? = null,
    @SerialName("categories_tags") val categoriesTags: List<String>? = null,
    @SerialName("ingredients_analysis_tags") val ingredientsAnalysisTags: List<String> = emptyList(),
    val nutriments: NutrimentsDto? = null,
)

@Serializable
data class NutrimentsDto(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerialName("sugars_100g") val sugars100g: Double? = null,
    @SerialName("salt_100g") val salt100g: Double? = null,
    @SerialName("saturated-fat_100g") val saturatedFat100g: Double? = null,
    @SerialName("fiber_100g") val fiber100g: Double? = null,
    @SerialName("proteins_100g") val proteins100g: Double? = null,
)

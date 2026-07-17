package app.aislespy.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain product entity mapped from OFF/OBF DTOs.
 * See DOMAIN_MODELS.md for field meanings.
 *
 * [Serializable] for Room product_cache JSON payloads (kotlinx-serialization is pure Kotlin;
 * domain stays Android-free).
 */
@Serializable
data class Product(
    val barcode: String,
    val name: String,
    val brands: String?,
    val brandsTags: List<String> = emptyList(),
    val imageUrl: String?,
    val category: ProductCategory,
    val sourceDb: SourceDb,
    val ingredientsText: String?,
    val ingredientsTags: List<String>,
    val additivesTags: List<String>,
    val allergensTags: List<String>,
    val labelsTags: List<String>,
    val categoriesTags: List<String>,
    val ingredientsAnalysisTags: List<String> = emptyList(),
    val nutriscoreGrade: Char?,
    val nutriscoreScore: Int?,
    val novaGroup: Int?,
    val nutriments: Nutriments?,
)

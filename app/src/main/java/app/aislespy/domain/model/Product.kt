package app.aislespy.domain.model

/**
 * Domain product entity mapped from OFF/OBF DTOs.
 * See DOMAIN_MODELS.md for field meanings.
 */
data class Product(
    val barcode: String,
    val name: String,
    val brands: String?,
    val imageUrl: String?,
    val category: ProductCategory,
    val sourceDb: SourceDb,
    val ingredientsText: String?,
    val ingredientsTags: List<String>,
    val additivesTags: List<String>,
    val allergensTags: List<String>,
    val labelsTags: List<String>,
    val categoriesTags: List<String>,
    val nutriscoreGrade: Char?,
    val nutriscoreScore: Int?,
    val novaGroup: Int?,
    val nutriments: Nutriments?,
)

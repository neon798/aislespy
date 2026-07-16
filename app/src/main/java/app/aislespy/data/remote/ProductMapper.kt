package app.aislespy.data.remote

import app.aislespy.data.remote.dto.NutrimentsDto
import app.aislespy.data.remote.dto.ProductDto
import app.aislespy.data.remote.dto.ProductResponseDto
import app.aislespy.domain.model.Nutriments
import app.aislespy.domain.model.Product
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.SourceDb

/**
 * Maps OFF/OBF response DTOs to domain [Product].
 */
object ProductMapper {

    fun toFoodProduct(response: ProductResponseDto): Product {
        val product = requireNotNull(response.product) { "product required for Found mapping" }
        return product.toDomain(
            barcode = response.code ?: product.code.orEmpty(),
            category = ProductCategory.Food,
            sourceDb = SourceDb.OpenFoodFacts,
        )
    }

    fun toBeautyProduct(response: ProductResponseDto): Product {
        val product = requireNotNull(response.product) { "product required for Found mapping" }
        return product.toDomain(
            barcode = response.code ?: product.code.orEmpty(),
            category = ProductCategory.Beauty,
            sourceDb = SourceDb.OpenBeautyFacts,
        )
    }

    fun ProductDto.toDomain(
        barcode: String,
        category: ProductCategory,
        sourceDb: SourceDb,
    ): Product {
        val resolvedBarcode = barcode.ifBlank { code.orEmpty() }
        return Product(
            barcode = resolvedBarcode,
            name = productName?.trim()?.takeIf { it.isNotEmpty() } ?: "Unknown product",
            brands = brands?.trim()?.takeIf { it.isNotEmpty() },
            imageUrl = upgradeToHttps(imageFrontUrl ?: imageFrontSmallUrl),
            category = category,
            sourceDb = sourceDb,
            ingredientsText = ingredientsText,
            ingredientsTags = ingredientsTags.orEmpty(),
            additivesTags = additivesTags.orEmpty(),
            allergensTags = allergensTags.orEmpty(),
            labelsTags = labelsTags.orEmpty(),
            categoriesTags = categoriesTags.orEmpty(),
            nutriscoreGrade = nutriscoreGrade.toNutriscoreChar(),
            nutriscoreScore = nutriscoreScore,
            novaGroup = novaGroup,
            nutriments = nutriments?.toDomain(),
        )
    }

    private fun NutrimentsDto.toDomain(): Nutriments =
        Nutriments(
            energyKcal100g = energyKcal100g,
            sugars100g = sugars100g,
            salt100g = salt100g,
            saturatedFat100g = saturatedFat100g,
            fiber100g = fiber100g,
            proteins100g = proteins100g,
        )

    /** Prefer HTTPS; upgrade plain http image URLs from older OFF data. */
    internal fun upgradeToHttps(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return if (url.startsWith("http://", ignoreCase = true)) {
            "https://" + url.removePrefix("http://").removePrefix("HTTP://")
        } else {
            url
        }
    }

    /** OFF sends grade as string; domain uses lowercase Char `a`–`e`. */
    internal fun String?.toNutriscoreChar(): Char? {
        val trimmed = this?.trim()?.lowercase().orEmpty()
        if (trimmed.isEmpty()) return null
        val c = trimmed[0]
        return if (c in 'a'..'e') c else c
    }
}

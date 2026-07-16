package app.aislespy.domain.scoring

import app.aislespy.domain.model.Nutriments
import app.aislespy.domain.model.Product
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.SourceDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryResolverTest {

    @Test
    fun clearlyFood_nutriscorePresent() {
        assertTrue(CategoryResolver.clearlyFood(product(nutriscoreGrade = 'e')))
    }

    @Test
    fun clearlyFood_novaPresent() {
        assertTrue(CategoryResolver.clearlyFood(product(novaGroup = 4)))
    }

    @Test
    fun clearlyFood_foodCategories() {
        assertTrue(
            CategoryResolver.clearlyFood(
                product(categoriesTags = listOf("en:plant-based-foods", "en:snacks")),
            ),
        )
        assertTrue(
            CategoryResolver.clearlyFood(
                product(categoriesTags = listOf("en:beverages")),
            ),
        )
        assertTrue(
            CategoryResolver.clearlyFood(
                product(categoriesTags = listOf("en:breakfasts")),
            ),
        )
    }

    @Test
    fun clearlyFood_eNumberAdditives() {
        assertTrue(
            CategoryResolver.clearlyFood(
                product(additivesTags = listOf("en:e322", "en:e476")),
            ),
        )
    }

    @Test
    fun clearlyFood_emptySparseProduct_isFalse() {
        assertFalse(CategoryResolver.clearlyFood(product()))
        assertFalse(
            CategoryResolver.clearlyFood(
                product(additivesTags = listOf("en:some-additive")),
            ),
        )
        assertFalse(
            CategoryResolver.clearlyFood(
                product(categoriesTags = listOf("en:unknown-tag")),
            ),
        )
    }

    @Test
    fun clearlyBeauty_cosmeticCategories() {
        assertTrue(
            CategoryResolver.clearlyBeauty(
                product(
                    category = ProductCategory.Beauty,
                    sourceDb = SourceDb.OpenBeautyFacts,
                    categoriesTags = listOf("en:skin-care"),
                ),
            ),
        )
        assertTrue(
            CategoryResolver.clearlyBeauty(
                product(
                    category = ProductCategory.Beauty,
                    sourceDb = SourceDb.OpenBeautyFacts,
                    categoriesTags = listOf("en:hair-care", "en:makeup"),
                ),
            ),
        )
        assertTrue(
            CategoryResolver.clearlyBeauty(
                product(
                    category = ProductCategory.Beauty,
                    sourceDb = SourceDb.OpenBeautyFacts,
                    categoriesTags = listOf("en:hygiene"),
                ),
            ),
        )
    }

    @Test
    fun clearlyBeauty_withoutCosmeticCategories_isFalse() {
        assertFalse(
            CategoryResolver.clearlyBeauty(
                product(
                    category = ProductCategory.Beauty,
                    sourceDb = SourceDb.OpenBeautyFacts,
                ),
            ),
        )
    }

    @Test
    fun resolve_clearlyFoodNotBeauty_returnsFood() {
        val off = product(
            nutriscoreGrade = 'e',
            novaGroup = 4,
            categoriesTags = listOf("en:snacks", "en:chocolate-spreads"),
            additivesTags = listOf("en:e322"),
        )
        val obf = product(
            category = ProductCategory.Beauty,
            sourceDb = SourceDb.OpenBeautyFacts,
            // empty / non-cosmetic categories
            categoriesTags = emptyList(),
        )
        assertEquals(CategoryResolver.Decision.Food, CategoryResolver.resolve(off, obf))
    }

    @Test
    fun resolve_clearlyBeautyNotFood_returnsBeauty() {
        val off = product() // sparse, no food signals
        val obf = product(
            category = ProductCategory.Beauty,
            sourceDb = SourceDb.OpenBeautyFacts,
            categoriesTags = listOf("en:skin-care"),
        )
        assertEquals(CategoryResolver.Decision.Beauty, CategoryResolver.resolve(off, obf))
    }

    @Test
    fun resolve_bothSignals_returnsAmbiguous() {
        val off = product(nutriscoreGrade = 'c', categoriesTags = listOf("en:beverages"))
        val obf = product(
            category = ProductCategory.Beauty,
            sourceDb = SourceDb.OpenBeautyFacts,
            categoriesTags = listOf("en:makeup"),
        )
        assertEquals(CategoryResolver.Decision.Ambiguous, CategoryResolver.resolve(off, obf))
    }

    @Test
    fun resolve_neitherClear_returnsAmbiguous() {
        val off = product(name = "Mystery A")
        val obf = product(
            name = "Mystery B",
            category = ProductCategory.Beauty,
            sourceDb = SourceDb.OpenBeautyFacts,
        )
        assertEquals(CategoryResolver.Decision.Ambiguous, CategoryResolver.resolve(off, obf))
    }

    @Test
    fun resolve_onlyOff_returnsFood() {
        assertEquals(CategoryResolver.Decision.Food, CategoryResolver.resolve(product(), null))
    }

    @Test
    fun resolve_onlyObf_returnsBeauty() {
        assertEquals(
            CategoryResolver.Decision.Beauty,
            CategoryResolver.resolve(
                null,
                product(category = ProductCategory.Beauty, sourceDb = SourceDb.OpenBeautyFacts),
            ),
        )
    }

    @Test
    fun resolve_neither_returnsAmbiguous() {
        assertEquals(CategoryResolver.Decision.Ambiguous, CategoryResolver.resolve(null, null))
    }

    private fun product(
        barcode: String = "123",
        name: String = "Test",
        brands: String? = null,
        imageUrl: String? = null,
        category: ProductCategory = ProductCategory.Food,
        sourceDb: SourceDb = SourceDb.OpenFoodFacts,
        ingredientsText: String? = null,
        ingredientsTags: List<String> = emptyList(),
        additivesTags: List<String> = emptyList(),
        allergensTags: List<String> = emptyList(),
        labelsTags: List<String> = emptyList(),
        categoriesTags: List<String> = emptyList(),
        nutriscoreGrade: Char? = null,
        nutriscoreScore: Int? = null,
        novaGroup: Int? = null,
        nutriments: Nutriments? = null,
    ): Product = Product(
        barcode = barcode,
        name = name,
        brands = brands,
        imageUrl = imageUrl,
        category = category,
        sourceDb = sourceDb,
        ingredientsText = ingredientsText,
        ingredientsTags = ingredientsTags,
        additivesTags = additivesTags,
        allergensTags = allergensTags,
        labelsTags = labelsTags,
        categoriesTags = categoriesTags,
        nutriscoreGrade = nutriscoreGrade,
        nutriscoreScore = nutriscoreScore,
        novaGroup = novaGroup,
        nutriments = nutriments,
    )
}

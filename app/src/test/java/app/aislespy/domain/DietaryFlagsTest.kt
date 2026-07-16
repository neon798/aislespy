package app.aislespy.domain

import app.aislespy.domain.model.Product
import app.aislespy.domain.model.ProductCategory
import app.aislespy.domain.model.SourceDb
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure JVM tests for [DietaryFlagsResolver] tri-state rules (ADR-014).
 */
class DietaryFlagsTest {

    // --- vegan ---

    @Test
    fun vegan_yes_fromAnalysisTag() {
        val flags = flags(analysis = listOf("en:vegan"))
        assertEquals(DietaryStatus.Yes, flags.vegan)
    }

    @Test
    fun vegan_yes_fromLabelsTag() {
        val flags = flags(labels = listOf("en:vegan"))
        assertEquals(DietaryStatus.Yes, flags.vegan)
    }

    @Test
    fun vegan_no_fromNonVeganAnalysis() {
        val flags = flags(analysis = listOf("en:non-vegan"))
        assertEquals(DietaryStatus.No, flags.vegan)
    }

    @Test
    fun vegan_unknown_whenMaybeVegan() {
        val flags = flags(analysis = listOf("en:maybe-vegan"))
        assertEquals(DietaryStatus.Unknown, flags.vegan)
    }

    @Test
    fun vegan_unknown_whenAbsent() {
        val flags = flags()
        assertEquals(DietaryStatus.Unknown, flags.vegan)
    }

    // --- vegetarian ---

    @Test
    fun vegetarian_yes_fromAnalysisOrLabels() {
        assertEquals(
            DietaryStatus.Yes,
            flags(analysis = listOf("en:vegetarian")).vegetarian,
        )
        assertEquals(
            DietaryStatus.Yes,
            flags(labels = listOf("en:vegetarian")).vegetarian,
        )
    }

    @Test
    fun vegetarian_no_fromNonVegetarian() {
        val flags = flags(analysis = listOf("en:non-vegetarian"))
        assertEquals(DietaryStatus.No, flags.vegetarian)
    }

    @Test
    fun vegetarian_unknown_whenMaybeOrAbsent() {
        assertEquals(
            DietaryStatus.Unknown,
            flags(analysis = listOf("en:maybe-vegetarian")).vegetarian,
        )
        assertEquals(DietaryStatus.Unknown, flags().vegetarian)
    }

    @Test
    fun veganYes_impliesVegetarianYes_whenVegetarianUnknown() {
        val flags = flags(analysis = listOf("en:vegan"))
        assertEquals(DietaryStatus.Yes, flags.vegan)
        assertEquals(DietaryStatus.Yes, flags.vegetarian)
    }

    @Test
    fun veganYes_doesNotOverrideExplicitNonVegetarian() {
        val flags = flags(
            analysis = listOf("en:vegan", "en:non-vegetarian"),
        )
        // vegan Yes from tag; vegetarian remains No if explicitly non-vegetarian
        assertEquals(DietaryStatus.Yes, flags.vegan)
        assertEquals(DietaryStatus.No, flags.vegetarian)
    }

    // --- dairy-free ---

    @Test
    fun dairyFree_no_whenAllergenMilk() {
        val flags = flags(allergens = listOf("en:milk"))
        assertEquals(DietaryStatus.No, flags.dairyFree)
    }

    @Test
    fun dairyFree_yes_fromDairyFreeLabels() {
        for (label in listOf(
            "en:no-lactose",
            "en:lactose-free",
            "en:dairy-free",
            "en:milk-free",
        )) {
            assertEquals(
                "label $label",
                DietaryStatus.Yes,
                flags(labels = listOf(label)).dairyFree,
            )
        }
    }

    @Test
    fun dairyFree_yes_whenVeganYes() {
        val flags = flags(analysis = listOf("en:vegan"))
        assertEquals(DietaryStatus.Yes, flags.dairyFree)
    }

    @Test
    fun dairyFree_unknown_byDefault() {
        val flags = flags()
        assertEquals(DietaryStatus.Unknown, flags.dairyFree)
    }

    @Test
    fun dairyFree_allergenMilk_winsOverVeganAndLabels() {
        val flags = flags(
            analysis = listOf("en:vegan"),
            allergens = listOf("en:milk"),
            labels = listOf("en:dairy-free"),
        )
        assertEquals(DietaryStatus.No, flags.dairyFree)
    }

    // --- combined Nutella-like ---

    @Test
    fun nutellaLike_nonVegan_vegetarian_containsDairy() {
        val flags = flags(
            analysis = listOf("en:non-vegan", "en:vegetarian"),
            allergens = listOf("en:milk"),
        )
        assertEquals(DietaryStatus.No, flags.vegan)
        assertEquals(DietaryStatus.Yes, flags.vegetarian)
        assertEquals(DietaryStatus.No, flags.dairyFree)
    }

    // --- helpers ---

    private fun flags(
        analysis: List<String> = emptyList(),
        labels: List<String> = emptyList(),
        allergens: List<String> = emptyList(),
    ): DietaryFlags = DietaryFlagsResolver.from(
        Product(
            barcode = "0",
            name = "Test",
            brands = null,
            imageUrl = null,
            category = ProductCategory.Food,
            sourceDb = SourceDb.OpenFoodFacts,
            ingredientsText = null,
            ingredientsTags = emptyList(),
            additivesTags = emptyList(),
            allergensTags = allergens,
            labelsTags = labels,
            categoriesTags = emptyList(),
            ingredientsAnalysisTags = analysis,
            nutriscoreGrade = null,
            nutriscoreScore = null,
            novaGroup = null,
            nutriments = null,
        ),
    )
}

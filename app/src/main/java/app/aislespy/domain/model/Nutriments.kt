package app.aislespy.domain.model

import kotlinx.serialization.Serializable

/**
 * Optional nutriment subset used for food scoring and display.
 * Values are per 100g as provided by Open Food Facts.
 */
@Serializable
data class Nutriments(
    val energyKcal100g: Double? = null,
    val sugars100g: Double? = null,
    val salt100g: Double? = null,
    val saturatedFat100g: Double? = null,
    val fiber100g: Double? = null,
    val proteins100g: Double? = null,
)

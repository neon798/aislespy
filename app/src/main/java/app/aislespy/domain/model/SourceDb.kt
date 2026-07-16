package app.aislespy.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SourceDb {
    OpenFoodFacts,
    OpenBeautyFacts,
}

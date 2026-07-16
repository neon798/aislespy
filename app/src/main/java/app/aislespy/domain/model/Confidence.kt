package app.aislespy.domain.model

/**
 * How complete the inputs were for a score (DOMAIN_MODELS.md).
 */
enum class Confidence {
    /** Core inputs present (food: Nutri-Score + NOVA). */
    High,

    /** Some core inputs missing; reweighted. */
    Medium,

    /** Sparse data; score is rough. */
    Low,
    ;
}

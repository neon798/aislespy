package app.aislespy.domain.model

/**
 * Full output of a [app.aislespy.domain.scoring.ScoreEngine].
 * See DOMAIN_MODELS.md.
 */
data class ScoreResult(
    val total: Int,
    val band: ScoreBand,
    val confidence: Confidence,
    val components: List<ScoreComponent>,
    val concerns: List<Concern>,
    val methodologyVersion: String,
    val summarySentence: String,
    /** One-liner naming the biggest weighted drags; null when none exceed the threshold. */
    val driverSentence: String? = null,
    /** Human labels of components dropped for missing data (e.g. "NOVA (no data)"). */
    val omittedComponents: List<String> = emptyList(),
)

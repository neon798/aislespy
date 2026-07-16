package app.aislespy.data.knowledge

import kotlinx.serialization.Serializable

/**
 * Single curated additive/ingredient row from a knowledge pack JSON file.
 * Schema: knowledge/schema/food_additive.schema.json (and beauty counterpart).
 */
@Serializable
data class KnowledgePackEntry(
    val id: String,
    val names: List<String>,
    val aliases: List<String> = emptyList(),
    val domain: String,
    val severity: Int,
    val categories: List<String> = emptyList(),
    val title: String,
    val why: String,
    val sources: List<String>,
)

/**
 * Versioned knowledge pack: severity, why, and sources for matching.
 * Prefer wrapped form: { "version", "domain", "entries" }.
 */
@Serializable
data class KnowledgePack(
    val version: String,
    val domain: String,
    val entries: List<KnowledgePackEntry>,
)

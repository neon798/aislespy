package app.aislespy.domain.knowledge

import app.aislespy.data.knowledge.KnowledgeMatcher
import app.aislespy.data.knowledge.KnowledgePack
import app.aislespy.data.knowledge.KnowledgePackEntry

/**
 * One knowledge-pack entry hit from a cross-pack ingredient lookup.
 *
 * [domain] is the pack domain (`food` / `beauty`), independent of product scan.
 */
data class IngredientHit(
    val entry: KnowledgePackEntry,
    val domain: String,
)

/**
 * Pure, JVM-testable search across food + beauty knowledge packs (ADR-023).
 *
 * Matching reuses [KnowledgeMatcher.normalize]: lowercase, trim, collapse spaces,
 * strip punctuation except hyphens. Aliases strip a language prefix (`en:`) for
 * matching; E-number digit forms (`250` ↔ `e250`) are also indexed.
 */
object IngredientSearch {

    const val MAX_RESULTS = 60

    private enum class RankTier {
        EXACT,
        PREFIX,
        CONTAINS,
    }

    private data class RankedHit(
        val hit: IngredientHit,
        val tier: RankTier,
    )

    /**
     * Search both packs for [query]. Blank/whitespace → empty list.
     * Dedupes by (domain, id). Caps at [MAX_RESULTS].
     */
    fun search(
        query: String,
        food: KnowledgePack,
        beauty: KnowledgePack,
    ): List<IngredientHit> {
        val q = KnowledgeMatcher.normalize(query)
        if (q.isEmpty()) return emptyList()

        val bestByKey = LinkedHashMap<String, RankedHit>()

        fun consider(pack: KnowledgePack) {
            for (entry in pack.entries) {
                val domain = pack.domain.ifBlank { entry.domain }
                val tier = bestTier(q, searchableFields(entry)) ?: continue
                val key = "$domain\u0000${entry.id}"
                val candidate = RankedHit(IngredientHit(entry = entry, domain = domain), tier)
                val existing = bestByKey[key]
                if (existing == null || tier.ordinal < existing.tier.ordinal) {
                    bestByKey[key] = candidate
                }
            }
        }

        consider(food)
        consider(beauty)

        return bestByKey.values
            .sortedWith(
                compareBy<RankedHit> { it.tier.ordinal }
                    .thenByDescending { it.hit.entry.severity }
                    .thenBy { it.hit.entry.title.lowercase() },
            )
            .take(MAX_RESULTS)
            .map { it.hit }
    }

    /**
     * Title, names, id, aliases (with `en:` stripped), and E-number digit forms.
     */
    internal fun searchableFields(entry: KnowledgePackEntry): List<String> {
        val fields = ArrayList<String>(8 + entry.names.size + entry.aliases.size)
        fields += KnowledgeMatcher.normalize(entry.title)
        for (name in entry.names) {
            fields += KnowledgeMatcher.normalize(name)
        }
        fields += KnowledgeMatcher.normalize(entry.id)
        for (alias in entry.aliases) {
            fields += KnowledgeMatcher.normalize(stripLangPrefix(alias))
        }
        // E-number digit form: id/alias "e250" also matches query "250"
        val eNumberDigits = Regex("^e(\\d+[a-z]?)$")
        val digitForms = ArrayList<String>()
        for (field in fields) {
            val m = eNumberDigits.matchEntire(field)
            if (m != null) digitForms += m.groupValues[1]
        }
        fields += digitForms
        return fields.filter { it.isNotEmpty() }.distinct()
    }

    private fun stripLangPrefix(raw: String): String {
        val colon = raw.indexOf(':')
        return if (colon >= 0) raw.substring(colon + 1) else raw
    }

    /**
     * Exact always allowed (incl. short E-numbers / ids).
     * Prefix and pure substring require query length ≥ 2 to avoid 1-char noise.
     */
    private fun bestTier(query: String, fields: List<String>): RankTier? {
        var best: RankTier? = null
        for (field in fields) {
            val tier = when {
                field == query -> RankTier.EXACT
                query.length >= 2 && field.startsWith(query) -> RankTier.PREFIX
                query.length >= 2 && field.contains(query) -> RankTier.CONTAINS
                else -> null
            } ?: continue
            if (best == null || tier.ordinal < best.ordinal) {
                best = tier
            }
            if (best == RankTier.EXACT) return RankTier.EXACT
        }
        return best
    }
}

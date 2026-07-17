package app.aislespy.domain

import app.aislespy.domain.model.Product
import kotlinx.serialization.Serializable

/**
 * Result of brand-ownership resolution for a product (ADR-019).
 * Informational only — never factored into scores.
 */
sealed class BrandOwnership {
    /**
     * Product brand matched a sourced conglomerate entry.
     * @param parentDisplay UI label for the parent (e.g. "Nestlé")
     * @param sources citations from the pack entry
     */
    data class Corporate(
        val parentDisplay: String,
        val sources: List<String>,
    ) : BrandOwnership()

    /**
     * Product brand matched the verified-independent allowlist.
     * @param display brand display name from the pack
     * @param note optional short note (e.g. "Family-owned")
     * @param sources citations from the pack entry
     */
    data class Independent(
        val display: String,
        val note: String?,
        val sources: List<String>,
    ) : BrandOwnership()
}

/**
 * Single curated brand ownership row from brand_ownership_v1.json.
 * Separate from food/beauty severity packs — no severity field.
 */
@Serializable
data class BrandOwnershipEntry(
    val id: String,
    val ownership: String,
    val brandAliases: List<String>,
    val parent: String? = null,
    val parentDisplay: String? = null,
    val display: String? = null,
    val note: String? = null,
    val sources: List<String>,
) {
    val isConglomerate: Boolean get() = ownership == OWNERSHIP_CONGLOMERATE
    val isIndependent: Boolean get() = ownership == OWNERSHIP_INDEPENDENT

    companion object {
        const val OWNERSHIP_CONGLOMERATE = "conglomerate"
        const val OWNERSHIP_INDEPENDENT = "independent"
    }
}

/**
 * Versioned brand ownership pack (wrapped form).
 * domain should be "brand_ownership".
 */
@Serializable
data class BrandOwnershipPack(
    val version: String,
    val domain: String,
    val entries: List<BrandOwnershipEntry>,
)

/**
 * Pure resolver: OFF/OBF `brands_tags` + ownership pack → [BrandOwnership]?.
 *
 * Conservative exact-token matching only (ADR-019):
 * - Normalize tags and aliases (lowercase, trim).
 * - Exact token match — no loose substring.
 * - First / most-specific match wins (longest matching alias length).
 * - Conglomerate + independent both hit → null (data conflict fail-safe).
 * - No match → null (never infer independence from absence).
 */
object BrandOwnershipResolver {

    fun resolve(product: Product, pack: BrandOwnershipPack): BrandOwnership? {
        val tags = product.brandsTags
            .map { normalizeToken(it) }
            .filter { it.isNotEmpty() }
            .toSet()
        if (tags.isEmpty() || pack.entries.isEmpty()) return null

        val conglomerateHits = mutableListOf<Pair<BrandOwnershipEntry, Int>>()
        val independentHits = mutableListOf<Pair<BrandOwnershipEntry, Int>>()

        for (entry in pack.entries) {
            val bestAliasLen = bestMatchingAliasLength(entry, tags) ?: continue
            when {
                entry.isConglomerate -> conglomerateHits += entry to bestAliasLen
                entry.isIndependent -> independentHits += entry to bestAliasLen
            }
        }

        // Data conflict: brand on both lists → fail safe (pack bug).
        if (conglomerateHits.isNotEmpty() && independentHits.isNotEmpty()) {
            return null
        }
        if (conglomerateHits.isEmpty() && independentHits.isEmpty()) {
            return null
        }

        return if (conglomerateHits.isNotEmpty()) {
            val best = conglomerateHits.maxBy { it.second }.first
            val parentDisplay = best.parentDisplay?.takeIf { it.isNotBlank() } ?: return null
            BrandOwnership.Corporate(
                parentDisplay = parentDisplay,
                sources = best.sources,
            )
        } else {
            val best = independentHits.maxBy { it.second }.first
            val display = best.display?.takeIf { it.isNotBlank() } ?: return null
            BrandOwnership.Independent(
                display = display,
                note = best.note?.takeIf { it.isNotBlank() },
                sources = best.sources,
            )
        }
    }

    /**
     * Longest matching alias length for [entry] against [tags], or null if none.
     * Longer alias = more specific (e.g. "en:dr-bronner-s" over a hypothetical short tag).
     */
    private fun bestMatchingAliasLength(
        entry: BrandOwnershipEntry,
        tags: Set<String>,
    ): Int? {
        var best: Int? = null
        for (alias in entry.brandAliases) {
            val n = normalizeToken(alias)
            if (n.isEmpty()) continue
            if (n in tags) {
                val len = n.length
                if (best == null || len > best) best = len
            }
        }
        return best
    }

    /** Lowercase + trim; conservative exact token match input. */
    fun normalizeToken(raw: String): String = raw.trim().lowercase()
}

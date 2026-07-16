package app.aislespy.data.knowledge

import app.aislespy.domain.model.MatchedIngredient

/**
 * Matches product additive/ingredient signals against a [KnowledgePack].
 *
 * Algorithm (docs/KNOWLEDGE_PACK.md):
 * - normalize: lowercase, trim, collapse spaces, strip punctuation except hyphens
 * - tag match: exact alias equality (keep `en:` prefix), case-insensitive
 * - name equality on normalized tokens from tags/text
 * - whole-word containment in ingredients_text (names with length ≥ 4 only)
 * - prefer longer names when matches overlap
 * - at most one match per entry id
 * - listIndex when an ordered ingredient list is available
 */
object KnowledgeMatcher {

    private const val MIN_CONTAINMENT_NAME_LENGTH = 4

    private data class Candidate(
        val entry: KnowledgePackEntry,
        val displayName: String,
        val matchedOn: String,
        val listIndex: Int?,
        val nameLength: Int,
    )

    /**
     * Run the knowledge-pack matching algorithm.
     *
     * @param pack curated pack (typically food additives)
     * @param additivesTags OFF/OBF `additives_tags`
     * @param ingredientsTags OFF/OBF `ingredients_tags`
     * @param allergensTags OFF/OBF `allergens_tags`
     * @param ingredientsText free-text ingredient list
     * @param orderedIngredientNames optional pre-parsed ordered names; if null,
     *   derived from [ingredientsText] by splitting on commas/parentheses
     */
    fun match(
        pack: KnowledgePack,
        additivesTags: List<String> = emptyList(),
        ingredientsTags: List<String> = emptyList(),
        allergensTags: List<String> = emptyList(),
        ingredientsText: String? = null,
        orderedIngredientNames: List<String>? = null,
    ): List<MatchedIngredient> {
        val tagSet = (additivesTags + ingredientsTags + allergensTags)
            .map { tagNormalize(it) }
            .filter { it.isNotEmpty() }
            .toSet()

        val orderedList: List<String>? = when {
            orderedIngredientNames != null -> orderedIngredientNames
            !ingredientsText.isNullOrBlank() -> parseIngredientList(ingredientsText)
            else -> null
        }

        val tokens = buildTokens(tagSet, orderedList, ingredientsText)
        val normalizedText = ingredientsText?.let { normalize(it) }.orEmpty()

        val candidates = ArrayList<Candidate>(pack.entries.size)

        for (entry in pack.entries) {
            val aliasHit = entry.aliases.firstOrNull { alias ->
                tagNormalize(alias) in tagSet
            }
            if (aliasHit != null) {
                val display = longestName(entry) ?: entry.title
                candidates += Candidate(
                    entry = entry,
                    displayName = display,
                    matchedOn = "alias:$aliasHit",
                    listIndex = listIndexForEntry(entry, orderedList),
                    nameLength = normalize(display).length.coerceAtLeast(display.length),
                )
                continue
            }

            var matched: Candidate? = null
            for (name in entry.names.sortedByDescending { it.length }) {
                val n = normalize(name)
                if (n.isEmpty()) continue
                val tokenIndex = tokens.indexOfFirst { normalize(it) == n }
                if (tokenIndex >= 0) {
                    val listIndex = orderedList?.let { indexInOrderedList(n, it) }
                    matched = Candidate(
                        entry = entry,
                        displayName = name,
                        matchedOn = "name:$name",
                        listIndex = listIndex,
                        nameLength = n.length,
                    )
                    break
                }
            }
            if (matched != null) {
                candidates += matched
                continue
            }

            if (normalizedText.isNotEmpty()) {
                for (name in entry.names.sortedByDescending { it.length }) {
                    val n = normalize(name)
                    if (n.length < MIN_CONTAINMENT_NAME_LENGTH) continue
                    if (containsWholeWord(normalizedText, n)) {
                        candidates += Candidate(
                            entry = entry,
                            displayName = name,
                            matchedOn = "text:$name",
                            listIndex = orderedList?.let { indexInOrderedList(n, it) },
                            nameLength = n.length,
                        )
                        break
                    }
                }
            }
        }

        return resolveOverlaps(candidates).map { c ->
            MatchedIngredient(
                entryId = c.entry.id,
                displayName = c.displayName,
                severity = c.entry.severity,
                why = c.entry.why,
                sources = c.entry.sources,
                matchedOn = c.matchedOn,
                listIndex = c.listIndex,
            )
        }
    }

    /** lowercase, trim, collapse spaces, strip punctuation except hyphens. */
    fun normalize(s: String): String {
        val lower = s.lowercase().trim()
        val stripped = buildString(lower.length) {
            for (ch in lower) {
                when {
                    ch.isLetterOrDigit() || ch == '-' -> append(ch)
                    ch.isWhitespace() -> append(' ')
                    // strip other punctuation
                }
            }
        }
        return stripped.replace(Regex("\\s+"), " ").trim()
    }

    /** Tag match form: lowercase as-is, keep `en:` prefix. */
    fun tagNormalize(t: String): String = t.lowercase().trim()

    /**
     * Split ingredients text into ordered fragments on commas and parentheses.
     */
    fun parseIngredientList(text: String): List<String> =
        text.split(Regex("[,()]"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    // --- internals ---

    private fun buildTokens(
        tagSet: Set<String>,
        orderedList: List<String>?,
        ingredientsText: String?,
    ): List<String> {
        val tokens = ArrayList<String>()
        orderedList?.let { tokens.addAll(it) }
        for (tag in tagSet) {
            tokens += tagToNameToken(tag)
        }
        if (!ingredientsText.isNullOrBlank()) {
            val normalized = normalize(ingredientsText)
            tokens += normalized.split(' ').filter { it.isNotEmpty() }
        }
        return tokens
    }

    /** `en:sodium-nitrite` → `sodium nitrite` for name-equality tokens. */
    private fun tagToNameToken(tag: String): String {
        val withoutLang = if (':' in tag) tag.substringAfter(':') else tag
        return withoutLang.replace('-', ' ')
    }

    private fun longestName(entry: KnowledgePackEntry): String? =
        entry.names.maxByOrNull { it.length }

    private fun listIndexForEntry(
        entry: KnowledgePackEntry,
        orderedList: List<String>?,
    ): Int? {
        if (orderedList == null) return null
        for (name in entry.names.sortedByDescending { it.length }) {
            val idx = indexInOrderedList(normalize(name), orderedList)
            if (idx != null) return idx
        }
        return null
    }

    private fun indexInOrderedList(normalizedName: String, orderedList: List<String>): Int? {
        if (normalizedName.isEmpty()) return null
        val exact = orderedList.indexOfFirst { normalize(it) == normalizedName }
        if (exact >= 0) return exact
        if (normalizedName.length >= MIN_CONTAINMENT_NAME_LENGTH) {
            val contained = orderedList.indexOfFirst { item ->
                containsWholeWord(normalize(item), normalizedName)
            }
            if (contained >= 0) return contained
        }
        return null
    }

    /**
     * Whole-word / whole-phrase containment on already-normalized strings.
     * Boundaries are start/end or non-alphanumeric (hyphen counts as part of a token).
     */
    internal fun containsWholeWord(normalizedText: String, normalizedWord: String): Boolean {
        if (normalizedWord.isEmpty() || normalizedText.isEmpty()) return false
        if (normalizedText == normalizedWord) return true
        val pattern = Regex(
            "(^|[^a-z0-9-])${Regex.escape(normalizedWord)}([^a-z0-9-]|$)",
        )
        return pattern.containsMatchIn(normalizedText)
    }

    /**
     * When two matches share a listIndex, keep the longer name.
     * Entry ids are already unique among [candidates] (one attempt per entry).
     */
    private fun resolveOverlaps(candidates: List<Candidate>): List<Candidate> {
        val sorted = candidates.sortedByDescending { it.nameLength }
        val claimedIndices = mutableSetOf<Int>()
        val claimedIds = mutableSetOf<String>()
        val result = ArrayList<Candidate>(sorted.size)
        for (c in sorted) {
            if (c.entry.id in claimedIds) continue
            val idx = c.listIndex
            if (idx != null && idx in claimedIndices) {
                // Overlap at same list position: longer name already claimed the slot
                continue
            }
            claimedIds += c.entry.id
            if (idx != null) claimedIndices += idx
            result += c
        }
        return result.sortedWith(
            compareBy<Candidate> { it.listIndex ?: Int.MAX_VALUE }
                .thenBy { it.entry.id },
        )
    }
}

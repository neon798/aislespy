package app.aislespy.data.knowledge

import android.content.Context
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Loads and parses ingredient/additive knowledge packs.
 *
 * [parse] is pure (String → [KnowledgePack]) so it can be unit-tested on the JVM
 * without Android assets. [loadFromAssets] is the runtime path used by [app.aislespy.di.AppContainer].
 */
object KnowledgePackLoader {

    const val FOOD_PACK_ASSET: String = "knowledge/food_additives_v1.json"
    const val BEAUTY_PACK_ASSET: String = "knowledge/beauty_ingredients_v1.json"

    private val defaultJson: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Parse a knowledge-pack JSON string. Fails fast with a clear message on invalid JSON.
     */
    fun parse(jsonString: String, json: Json = defaultJson): KnowledgePack {
        if (jsonString.isBlank()) {
            throw IllegalArgumentException("Invalid knowledge pack JSON: empty input")
        }
        return try {
            json.decodeFromString(KnowledgePack.serializer(), jsonString)
        } catch (e: SerializationException) {
            throw IllegalArgumentException(
                "Invalid knowledge pack JSON: ${e.message ?: e::class.simpleName}",
                e,
            )
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Invalid knowledge pack JSON: ${e.message ?: e::class.simpleName}",
                e,
            )
        }
    }

    /**
     * Load a pack from Android assets (e.g. [FOOD_PACK_ASSET]).
     */
    fun loadFromAssets(
        context: Context,
        assetPath: String = FOOD_PACK_ASSET,
        json: Json = defaultJson,
    ): KnowledgePack {
        val text = try {
            context.assets.open(assetPath).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Failed to read knowledge pack asset '$assetPath': ${e.message}",
                e,
            )
        }
        return parse(text, json)
    }
}

package app.aislespy.di

import android.content.Context
import androidx.room.Room
import app.aislespy.BuildConfig
import app.aislespy.data.knowledge.KnowledgePack
import app.aislespy.data.knowledge.KnowledgePackLoader
import app.aislespy.data.local.AisleSpyDatabase
import app.aislespy.data.local.HistoryDao
import app.aislespy.data.local.HistoryRepository
import app.aislespy.data.local.ProductCacheDao
import app.aislespy.data.remote.ApiConfig
import app.aislespy.data.remote.ObfApi
import app.aislespy.data.remote.OffApi
import app.aislespy.data.remote.ProductRepository
import app.aislespy.domain.scoring.BeautyScoreEngine
import app.aislespy.domain.scoring.CategoryResolver
import app.aislespy.domain.scoring.FoodScoreEngine
import app.aislespy.domain.scoring.ScoreEngine
import app.aislespy.ui.result.ChoicePairStore
import app.aislespy.ui.result.ConcernDetailStore
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Manual composition root for AisleSpy.
 *
 * Holds application-scoped dependencies. Constructed once from [app.aislespy.AisleSpyApp]
 * and read by [app.aislespy.MainActivity] (and later ViewModel factories).
 *
 * Dependencies are added as phases land — do not introduce Hilt unless complexity forces it
 * (see ARCHITECTURE.md / AGENTS.md).
 */
class AppContainer(
    private val appContext: Context,
) {
    val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    /**
     * Injectable clock for cache TTL and history timestamps (testable via constructor fakes).
     */
    val clock: () -> Long = { System.currentTimeMillis() }

    /**
     * Room database (history + product_cache).
     *
     * Pre-1.0: [RoomDatabase.Builder.fallbackToDestructiveMigration] is OK — no production
     * users yet; schema may change without migrations. Replace with proper migrations before
     * a release that must preserve local history across upgrades.
     */
    val db: AisleSpyDatabase by lazy {
        Room.databaseBuilder(
            appContext.applicationContext,
            AisleSpyDatabase::class.java,
            DB_NAME,
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    val historyDao: HistoryDao by lazy { db.historyDao() }

    val productCacheDao: ProductCacheDao by lazy { db.productCacheDao() }

    val historyRepository: HistoryRepository by lazy { HistoryRepository(historyDao) }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", ApiConfig.userAgent(BuildConfig.VERSION_NAME))
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    val offApi: OffApi by lazy {
        createRetrofit(ApiConfig.OFF_BASE_URL).create(OffApi::class.java)
    }

    val obfApi: ObfApi by lazy {
        createRetrofit(ApiConfig.OBF_BASE_URL).create(ObfApi::class.java)
    }

    val repository: ProductRepository by lazy {
        ProductRepository(
            offApi = offApi,
            obfApi = obfApi,
            categoryResolver = CategoryResolver,
            productCacheDao = productCacheDao,
            clock = clock,
            cacheTtlMs = ApiConfig.PRODUCT_CACHE_TTL_MS,
            json = ProductRepository.defaultCacheJson,
        )
    }

    /**
     * Food knowledge pack (T-300/T-310). Loaded lazily on first access from assets.
     * Prefer loading off the main thread when first needed (lazy is fine for MVP).
     */
    val foodKnowledgePack: KnowledgePack by lazy {
        KnowledgePackLoader.loadFromAssets(
            context = appContext,
            assetPath = KnowledgePackLoader.FOOD_PACK_ASSET,
            json = json,
        )
    }

    /**
     * Beauty knowledge pack (T-400). Loaded lazily, independent of the food pack.
     */
    val beautyKnowledgePack: KnowledgePack by lazy {
        KnowledgePackLoader.loadFromAssets(
            context = appContext,
            assetPath = KnowledgePackLoader.BEAUTY_PACK_ASSET,
            json = json,
        )
    }

    /** @deprecated Prefer [foodKnowledgePack]; kept for any residual call sites. */
    val knowledgePack: KnowledgePack get() = foodKnowledgePack

    /** Pure food scoring engine (T-320). Stateless; safe to share. */
    val foodScoreEngine: ScoreEngine by lazy { FoodScoreEngine() }

    /** Pure beauty scoring engine (T-410). Stateless; safe to share. */
    val beautyScoreEngine: ScoreEngine by lazy { BeautyScoreEngine() }

    /** Last scored concerns for ingredient detail navigation (T-330). */
    val concernDetailStore: ConcernDetailStore by lazy { ConcernDetailStore() }

    /** Ambiguous food/beauty pair for category chooser → result (T-420). */
    val choicePairStore: ChoicePairStore by lazy { ChoicePairStore() }

    private fun createRetrofit(baseUrl: String): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl.ensureTrailingSlash())
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    companion object {
        private const val DB_NAME = "aislespy.db"
    }
}

private fun String.ensureTrailingSlash(): String =
    if (endsWith("/")) this else "$this/"

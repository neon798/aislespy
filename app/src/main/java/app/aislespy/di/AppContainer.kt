package app.aislespy.di

import android.content.Context
import app.aislespy.BuildConfig
import app.aislespy.data.knowledge.KnowledgePack
import app.aislespy.data.knowledge.KnowledgePackLoader
import app.aislespy.data.remote.ApiConfig
import app.aislespy.data.remote.ObfApi
import app.aislespy.data.remote.OffApi
import app.aislespy.data.remote.ProductRepository
import app.aislespy.domain.scoring.CategoryResolver
import app.aislespy.domain.scoring.FoodScoreEngine
import app.aislespy.domain.scoring.ScoreEngine
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
 *
 * Planned wiring (placeholders until the owning tasks ship):
 * - **db** — Room [AisleSpyDatabase] for history + product cache (T-500)
 * - **beautyEngine** — [BeautyScoreEngine] (T-410)
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
        )
    }

    /**
     * Food knowledge pack (T-300/T-310). Loaded lazily on first access from assets.
     * Prefer loading off the main thread when first needed (lazy is fine for MVP).
     */
    val knowledgePack: KnowledgePack by lazy {
        KnowledgePackLoader.loadFromAssets(
            context = appContext,
            assetPath = KnowledgePackLoader.FOOD_PACK_ASSET,
            json = json,
        )
    }

    /** Pure food scoring engine (T-320). Stateless; safe to share. */
    val foodScoreEngine: ScoreEngine by lazy { FoodScoreEngine() }

    /** Last scored concerns for ingredient detail navigation (T-330). */
    val concernDetailStore: ConcernDetailStore by lazy { ConcernDetailStore() }

    // TODO(T-500): val db: AisleSpyDatabase
    // TODO(T-410): val beautyEngine: BeautyScoreEngine

    private fun createRetrofit(baseUrl: String): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl.ensureTrailingSlash())
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
}

private fun String.ensureTrailingSlash(): String =
    if (endsWith("/")) this else "$this/"

package app.aislespy.di

import android.content.Context
import app.aislespy.BuildConfig
import app.aislespy.data.remote.ApiConfig
import app.aislespy.data.remote.ObfApi
import app.aislespy.data.remote.OffApi
import app.aislespy.data.remote.ProductRepository
import app.aislespy.domain.scoring.CategoryResolver
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
 * - **knowledgePack** — loaded food/beauty risk JSON (T-310)
 * - **foodEngine** — [FoodScoreEngine] (T-320)
 * - **beautyEngine** — [BeautyScoreEngine] (T-410)
 */
class AppContainer(
    @Suppress("unused") private val appContext: Context,
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

    // TODO(T-500): val db: AisleSpyDatabase
    // TODO(T-310): val knowledgePack: KnowledgePack
    // TODO(T-320): val foodEngine: FoodScoreEngine
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

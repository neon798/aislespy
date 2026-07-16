package app.aislespy.di

import android.content.Context

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
 * - **httpClient** — Ktor or OkHttp client with mandatory User-Agent (T-210)
 * - **offApi** — Open Food Facts client (T-210)
 * - **obfApi** — Open Beauty Facts client (T-210)
 * - **db** — Room [AisleSpyDatabase] for history + product cache (T-500)
 * - **knowledgePack** — loaded food/beauty risk JSON (T-310)
 * - **repository** — [ProductRepository] dual lookup + cache (T-220)
 * - **foodEngine** — [FoodScoreEngine] (T-320)
 * - **beautyEngine** — [BeautyScoreEngine] (T-410)
 */
class AppContainer(
    @Suppress("unused") private val appContext: Context,
) {
    // TODO(T-210): val httpClient: HttpClient
    // TODO(T-210): val offApi: OffApi
    // TODO(T-210): val obfApi: ObfApi
    // TODO(T-500): val db: AisleSpyDatabase
    // TODO(T-310): val knowledgePack: KnowledgePack
    // TODO(T-220): val repository: ProductRepository
    // TODO(T-320): val foodEngine: FoodScoreEngine
    // TODO(T-410): val beautyEngine: BeautyScoreEngine
}

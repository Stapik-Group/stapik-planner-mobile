package pl.stapik.planner.data.repository

import kotlinx.serialization.json.Json
import pl.stapik.planner.data.cache.PlannerCacheStorage
import pl.stapik.planner.data.config.ApiConfigStorage
import pl.stapik.planner.data.model.PlannerPayload
import pl.stapik.planner.data.model.PlannerSyncEnvelope
import pl.stapik.planner.data.network.NetworkModule

class MissingConfigException : Exception("API connection not configured")

data class PlannerFetchResult(
    val payload: PlannerPayload,
    val updatedAt: String // ISO-8601 from DocumentResponse.updatedAt - authoritative timestamp
)

class PlannerRepository(
    private val apiConfigStorage: ApiConfigStorage,
    private val cacheStorage: PlannerCacheStorage
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchPlan(): PlannerFetchOutcome {
        val networkResult = runCatching {
            val config = apiConfigStorage.load() ?: throw MissingConfigException()
            val api = NetworkModule.createApi(baseUrl = config.baseUrl)
            val document = api.getDocument(slotKey = SLOT_KEY, apiKey = config.apiKey)
            val syncEnvelope = json.decodeFromString<PlannerSyncEnvelope>(document.content)
            PlannerFetchResult(payload = syncEnvelope.payload, updatedAt = document.updatedAt)
        }

        networkResult.onSuccess { result ->
            cacheStorage.save(payload = result.payload, updatedAt = result.updatedAt)
        }

        return networkResult.fold(
            onSuccess = { PlannerFetchOutcome.Fresh(it) },
            onFailure = { error ->
                // Not configured should go straight to the Connect prompt, not a stale cache.
                if (error is MissingConfigException) {
                    PlannerFetchOutcome.Failure(error)
                } else {
                    val cached = cacheStorage.load()
                    if (cached != null) {
                        PlannerFetchOutcome.Cached(cached, error)
                    } else {
                        PlannerFetchOutcome.Failure(error)
                    }
                }
            }
        )
    }

    private companion object {
        const val SLOT_KEY = "planner.json"
    }
}

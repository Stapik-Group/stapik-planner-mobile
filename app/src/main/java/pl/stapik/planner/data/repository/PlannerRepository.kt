package pl.stapik.planner.data.repository

import kotlinx.serialization.json.Json
import pl.stapik.common.cloud.CloudConfig
import pl.stapik.common.cloud.HttpCloudGateway
import pl.stapik.common.cloud.HttpTransport
import pl.stapik.common.cloud.OkHttpTransport
import pl.stapik.common.model.SlotKey
import pl.stapik.common.sync.CloudGateway
import pl.stapik.planner.data.cache.PlannerCacheStorage
import pl.stapik.planner.data.config.ApiConfig
import pl.stapik.planner.data.config.ApiConfigStorage
import pl.stapik.planner.data.model.PlannerPayload
import pl.stapik.planner.data.model.PlannerSyncEnvelope

class MissingConfigException : Exception("API connection not configured")

/** The server answered, but it has no planner document in its slot. */
class PlannerNotFoundException : Exception("No planner document on the server")

data class PlannerFetchResult(
    val payload: PlannerPayload,
    val updatedAt: String // ISO-8601 from RemoteDocument.updatedAt - authoritative timestamp
)

class PlannerRepository(
    private val apiConfigStorage: ApiConfigStorage,
    private val cacheStorage: PlannerCacheStorage,
    private val transport: HttpTransport = OkHttpTransport()
) {
    private val json = Json { ignoreUnknownKeys = true }

    // The gateway keeps the derived end-to-end key, which is slow to compute, so it is reused
    // for as long as the configuration stays the same.
    private var gateway: Pair<ApiConfig, CloudGateway>? = null

    @Synchronized
    private fun gatewayFor(config: ApiConfig): CloudGateway {
        gateway?.let { (known, ready) -> if (known == config) return ready }
        val created = HttpCloudGateway(CloudConfig(config.baseUrl, config.apiKey, config.passphrase), transport)
        gateway = config to created
        return created
    }

    suspend fun fetchPlan(): PlannerFetchOutcome {
        val networkResult = runCatching {
            val config = apiConfigStorage.load() ?: throw MissingConfigException()
            val document = gatewayFor(config).read(SlotKey(SLOT_KEY)) ?: throw PlannerNotFoundException()
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

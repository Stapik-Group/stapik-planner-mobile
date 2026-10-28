package pl.stapik.planner.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.stapik.common.sync.CloudError
import pl.stapik.common.sync.CloudException
import pl.stapik.common.sync.DocumentStore
import pl.stapik.common.sync.FirstConnectionChoice
import pl.stapik.common.sync.SyncEngine
import pl.stapik.common.sync.SyncOutcome
import pl.stapik.planner.data.config.ApiConfigStorage
import pl.stapik.planner.data.document.LegacyCacheMigration
import pl.stapik.planner.data.document.PlannerDocument
import pl.stapik.planner.data.model.PlannerPayload

class MissingConfigException : Exception("API connection not configured")

class PlannerNotFoundException : Exception("No planner document on the server")

data class PlannerFetchResult(
    val payload: PlannerPayload,
    val updatedAt: String
)

class PlannerRepository(
    private val apiConfigStorage: ApiConfigStorage,
    private val engine: SyncEngine<PlannerPayload>,
    private val store: DocumentStore,
    private val legacyMigration: LegacyCacheMigration
) {
    private val migrationLock = Mutex()
    private var migrated = false

    suspend fun fetchPlan(): PlannerFetchOutcome {
        if (apiConfigStorage.load() == null) return PlannerFetchOutcome.Failure(MissingConfigException())
        return try {
            migrateLegacyCacheOnce()
            outcomeOf(engine.sync(FirstConnectionChoice.USE_CLOUD))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PlannerFetchOutcome.Failure(e)
        }
    }

    private suspend fun migrateLegacyCacheOnce() = migrationLock.withLock {
        if (!migrated) {
            legacyMigration.run()
            migrated = true
        }
    }

    private suspend fun outcomeOf(outcome: SyncOutcome): PlannerFetchOutcome {
        val local = readLocal()
        return when (outcome) {
            SyncOutcome.Synchronized, SyncOutcome.ServerWon, SyncOutcome.LocalWon ->
                if (local == null) {
                    PlannerFetchOutcome.Failure(PlannerNotFoundException())
                } else {
                    PlannerFetchOutcome.Fresh(local)
                }
            else -> {
                val cause = causeOf(outcome)
                if (local == null || cause is MissingConfigException) {
                    PlannerFetchOutcome.Failure(cause)
                } else {
                    PlannerFetchOutcome.Cached(local, cause)
                }
            }
        }
    }

    private suspend fun readLocal(): PlannerFetchResult? {
        val document = store.load() ?: return null
        val payload = PlannerDocument.codec.decode(document.content)
        if (PlannerDocument.codec.isEmpty(payload)) return null
        return PlannerFetchResult(payload = payload, updatedAt = document.cloudBaseline)
    }

    private fun causeOf(outcome: SyncOutcome): Exception = when (outcome) {
        is SyncOutcome.Offline -> CloudException(outcome.cause)
        is SyncOutcome.AuthProblem -> CloudException(outcome.cause)
        is SyncOutcome.RemoteUnreadable -> CloudException(CloudError.Unreadable(outcome.reason, outcome.kind))
        SyncOutcome.KeptLocalUnsynced -> PlannerNotFoundException()
        SyncOutcome.LocalOnly -> MissingConfigException()
        else -> IllegalStateException("Unexpected sync outcome: $outcome")
    }
}

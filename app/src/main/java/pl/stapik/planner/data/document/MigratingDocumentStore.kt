package pl.stapik.planner.data.document

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.common.model.LocalDocument
import pl.stapik.common.sync.ConflictCopy
import pl.stapik.common.sync.DocumentStore
import pl.stapik.planner.data.cache.LegacyPlannerCache
import pl.stapik.planner.data.model.PlannerPayload

class MigratingDocumentStore(
    private val delegate: DocumentStore,
    private val legacyCache: LegacyPlannerCache
) : DocumentStore {
    private val lock = Mutex()
    private var migrated = false

    override suspend fun load(): LocalDocument<DocumentEnvelope>? {
        migrateOnce()
        return delegate.load()
    }

    override suspend fun save(document: LocalDocument<DocumentEnvelope>) = delegate.save(document)

    override suspend fun saveConflictCopy(copy: ConflictCopy) = delegate.saveConflictCopy(copy)

    override suspend fun discard() = delegate.discard()

    private suspend fun migrateOnce() = lock.withLock {
        if (!migrated) {
            migrate()
            migrated = true
        }
    }

    private suspend fun migrate() {
        val existing = try {
            delegate.load()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return
        }
        if (existing == null) {
            legacyCache.load()?.let { snapshot -> delegate.save(documentOf(snapshot.payload, snapshot.updatedAt)) }
        }
        legacyCache.clear()
    }

    private fun documentOf(payload: PlannerPayload, updatedAt: String): LocalDocument<DocumentEnvelope> {
        val envelope = PlannerDocument.codec.encode(payload, DocumentEnvelope.empty(updatedAt), updatedAt)
        return LocalDocument(content = envelope, cloudBaseline = updatedAt)
    }
}

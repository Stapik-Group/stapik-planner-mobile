package pl.stapik.planner.data.document

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.common.model.LocalDocument
import pl.stapik.common.sync.ConflictCopy
import pl.stapik.common.sync.DocumentStore
import pl.stapik.planner.data.cache.LegacyPlannerCache

class LegacyCacheCleanupStore(
    private val delegate: DocumentStore,
    private val legacyCache: LegacyPlannerCache
) : DocumentStore {
    private val lock = Mutex()
    private var cleaned = false

    override suspend fun load(): LocalDocument<DocumentEnvelope>? {
        cleanOnce()
        return delegate.load()
    }

    override suspend fun save(document: LocalDocument<DocumentEnvelope>) = delegate.save(document)

    override suspend fun saveConflictCopy(copy: ConflictCopy) = delegate.saveConflictCopy(copy)

    override suspend fun discard() = delegate.discard()

    private suspend fun cleanOnce() = lock.withLock {
        if (!cleaned) {
            try {
                legacyCache.clear()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return@withLock
            }
            cleaned = true
        }
    }
}

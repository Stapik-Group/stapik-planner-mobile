package pl.stapik.planner.data.document

import kotlinx.coroutines.CancellationException
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.common.model.LocalDocument
import pl.stapik.common.sync.DocumentStore
import pl.stapik.planner.data.cache.LegacyPlannerCache
import pl.stapik.planner.data.model.PlannerPayload

class LegacyCacheMigration(
    private val legacyCache: LegacyPlannerCache,
    private val store: DocumentStore
) {
    suspend fun run() {
        val existing = try {
            store.load()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return
        }
        if (existing == null) {
            legacyCache.load()?.let { snapshot -> store.save(documentOf(snapshot.payload, snapshot.updatedAt)) }
        }
        legacyCache.clear()
    }

    private fun documentOf(payload: PlannerPayload, updatedAt: String): LocalDocument<DocumentEnvelope> {
        val envelope = PlannerDocument.codec.encode(payload, DocumentEnvelope.empty(updatedAt), updatedAt)
        return LocalDocument(content = envelope, cloudBaseline = updatedAt)
    }
}

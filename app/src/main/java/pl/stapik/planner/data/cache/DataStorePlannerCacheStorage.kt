package pl.stapik.planner.data.cache

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.stapik.planner.data.model.PlannerPayload

private val Context.plannerCacheDataStore by preferencesDataStore(name = "planner_cache")

@Serializable
private data class CachedPlannerPayload(val payload: PlannerPayload, val updatedAt: String)

class DataStorePlannerCacheStorage(private val context: Context) :
    PlannerCacheStorage {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun load(): CachedPlanner? {
        val raw = context.plannerCacheDataStore.data.first()[KEY_PAYLOAD] ?: return null
        // A corrupted or pre-migration payload should not crash the app, just act as no cache.
        return runCatching { json.decodeFromString<CachedPlannerPayload>(raw) }
            .getOrNull()
            ?.let {
                CachedPlanner(
                    payload = it.payload,
                    updatedAt = it.updatedAt
                )
            }
    }

    override suspend fun save(payload: PlannerPayload, updatedAt: String) {
        val encoded = json.encodeToString(
            CachedPlannerPayload(
                payload = payload,
                updatedAt = updatedAt
            )
        )
        context.plannerCacheDataStore.edit { it[KEY_PAYLOAD] = encoded }
    }

    private companion object {
        val KEY_PAYLOAD = stringPreferencesKey("cached_planner")
    }
}

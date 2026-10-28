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
data class LegacyPlannerSnapshot(val payload: PlannerPayload, val updatedAt: String)

class LegacyPlannerCache(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): LegacyPlannerSnapshot? {
        val raw = context.plannerCacheDataStore.data.first()[KEY_PAYLOAD] ?: return null
        return runCatching { json.decodeFromString<LegacyPlannerSnapshot>(raw) }.getOrNull()
    }

    suspend fun clear() {
        context.plannerCacheDataStore.edit { it.clear() }
    }

    private companion object {
        val KEY_PAYLOAD = stringPreferencesKey("cached_planner")
    }
}

package pl.stapik.planner.data.cache

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.plannerCacheDataStore by preferencesDataStore(name = "planner_cache")

class LegacyPlannerCache(private val context: Context) {
    suspend fun clear() {
        if (context.plannerCacheDataStore.data.first().asMap().isNotEmpty()) {
            context.plannerCacheDataStore.edit { it.clear() }
        }
    }
}

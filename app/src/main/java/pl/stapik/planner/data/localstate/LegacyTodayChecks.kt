package pl.stapik.planner.data.localstate

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import kotlinx.coroutines.flow.first

private val Context.todayChecksDataStore by preferencesDataStore(name = "today_checks")

class LegacyTodayChecks(private val context: Context) {
    suspend fun readAll(): Map<String, Set<Int>> =
        context.todayChecksDataStore.data.first().asMap().entries
            .filter { it.value == true }
            .mapNotNull { parse(it.key.name) }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }

    suspend fun clear() {
        context.todayChecksDataStore.edit { it.clear() }
    }

    private fun parse(name: String): Pair<String, Int>? {
        val date = name.substringBefore(':')
        val index = name.substringAfter(':', missingDelimiterValue = "").toIntOrNull() ?: return null
        return runCatching { LocalDate.parse(date) }.getOrNull()?.let { it.toString() to index }
    }
}

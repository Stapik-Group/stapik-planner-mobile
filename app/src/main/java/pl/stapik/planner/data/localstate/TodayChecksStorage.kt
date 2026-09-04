package pl.stapik.planner.data.localstate

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.todayChecksDataStore by preferencesDataStore(name = "today_checks")

/**
 * Purely local, ephemeral "did I do this today" state for today's slots.
 * Never sent to the cloud and never part of [pl.stapik.planner.data.model.PlannerPayload] -
 * it lives alongside the synced plan only for rendering checkboxes on the
 * "today" page. Keys are "{isoDate}:{slotIndex}" so a new day naturally
 * starts unchecked without any explicit reset logic; entries older than a
 * couple of days are pruned lazily on write so the store doesn't grow
 * forever.
 */
interface TodayChecksStorage {
    fun observeChecked(date: LocalDate): Flow<Set<Int>>
    suspend fun setChecked(date: LocalDate, slotIndex: Int, checked: Boolean)
}

class DataStoreTodayChecksStorage(private val context: Context) : TodayChecksStorage {

    override fun observeChecked(date: LocalDate): Flow<Set<Int>> {
        val prefix = keyPrefix(date)
        return context.todayChecksDataStore.data.map { prefs ->
            prefs.asMap().entries
                .asSequence()
                .filter { it.key.name.startsWith(prefix) && it.value == true }
                .mapNotNull { it.key.name.removePrefix(prefix).toIntOrNull() }
                .toSet()
        }
    }

    override suspend fun setChecked(date: LocalDate, slotIndex: Int, checked: Boolean) {
        val key = booleanPreferencesKey(keyPrefix(date) + slotIndex)
        val cutoff = date.minusDays(RETENTION_DAYS)

        context.todayChecksDataStore.edit { prefs ->
            if (checked) {
                prefs[key] = true
            } else {
                prefs.remove(key)
            }

            val staleKeys = prefs.asMap().keys.filter { existingKey ->
                val entryDate = existingKey.name.substringBefore(':', missingDelimiterValue = "")
                runCatching { LocalDate.parse(entryDate) }.getOrNull()?.isBefore(cutoff) == true
            }
            staleKeys.forEach { prefs.remove(it) }
        }
    }

    private fun keyPrefix(date: LocalDate) = "$date:"

    private companion object {
        const val RETENTION_DAYS = 2L
    }
}

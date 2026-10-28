package pl.stapik.planner.data.localstate

import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import pl.stapik.common.document.MobileOnlyStore

class MobileOnlyTodayChecksStorage(
    private val store: MobileOnlyStore<TodayChecks>,
    private val legacy: LegacyTodayChecks
) : TodayChecksStorage {
    private val current = MutableStateFlow<TodayChecks?>(null)

    override fun observeChecked(date: LocalDate): Flow<Map<Int, String?>> =
        current.onStart { ensureLoaded() }
            .filterNotNull()
            .map { it.on(date) }
            .distinctUntilChanged()

    override suspend fun setChecked(date: LocalDate, slotIndex: Int, checked: Boolean, signature: String) {
        val cutoff = date.minusDays(RETENTION_DAYS)
        current.value = store.update { it.with(date, slotIndex, checked, signature).prunedBefore(cutoff) }
    }

    private suspend fun ensureLoaded() {
        if (current.value != null) return
        current.value = try {
            load()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            TodayChecks()
        }
    }

    private suspend fun load(): TodayChecks {
        val migrated = legacy.readAll()
        if (migrated.isEmpty()) return store.load()
        return store.update { it.merged(migrated) }.also { legacy.clear() }
    }

    private companion object {
        const val RETENTION_DAYS = 2L
    }
}

package pl.stapik.planner.data.localstate

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface TodayChecksStorage {
    fun observeChecked(date: LocalDate): Flow<Set<Int>>
    suspend fun setChecked(date: LocalDate, slotIndex: Int, checked: Boolean)
}

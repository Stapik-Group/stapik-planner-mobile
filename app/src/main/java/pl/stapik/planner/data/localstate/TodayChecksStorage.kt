package pl.stapik.planner.data.localstate

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface TodayChecksStorage {
    fun observeChecked(date: LocalDate): Flow<Map<Int, String?>>
    suspend fun setChecked(date: LocalDate, slotIndex: Int, checked: Boolean, signature: String)
}

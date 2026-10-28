package pl.stapik.planner.data.localstate

import java.time.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class TodayChecks(val byDate: Map<String, Set<Int>> = emptyMap()) {
    fun with(date: LocalDate, slotIndex: Int, checked: Boolean): TodayChecks {
        val key = date.toString()
        val slots = byDate[key].orEmpty().let { if (checked) it + slotIndex else it - slotIndex }
        return copy(byDate = if (slots.isEmpty()) byDate - key else byDate + (key to slots))
    }

    fun prunedBefore(cutoff: LocalDate): TodayChecks =
        copy(byDate = byDate.filterKeys { key -> runCatching { LocalDate.parse(key) }.getOrNull()?.isBefore(cutoff) != true })

    fun merged(other: Map<String, Set<Int>>): TodayChecks =
        copy(byDate = (byDate.keys + other.keys).associateWith { byDate[it].orEmpty() + other[it].orEmpty() })
}

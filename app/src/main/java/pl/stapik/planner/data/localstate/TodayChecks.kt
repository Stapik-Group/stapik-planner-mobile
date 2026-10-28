package pl.stapik.planner.data.localstate

import java.time.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class TodayChecks(
    val byDate: Map<String, Set<Int>> = emptyMap(),
    val signatures: Map<String, Map<Int, String>> = emptyMap()
) {
    fun on(date: LocalDate): Map<Int, String?> {
        val key = date.toString()
        val known = signatures[key].orEmpty()
        return byDate[key].orEmpty().associateWith { known[it] }
    }

    fun with(date: LocalDate, slotIndex: Int, checked: Boolean, signature: String): TodayChecks {
        val key = date.toString()
        val slots = byDate[key].orEmpty().let { if (checked) it + slotIndex else it - slotIndex }
        val known = signatures[key].orEmpty().let { if (checked) it + (slotIndex to signature) else it - slotIndex }
        return copy(
            byDate = if (slots.isEmpty()) byDate - key else byDate + (key to slots),
            signatures = if (known.isEmpty()) signatures - key else signatures + (key to known)
        )
    }

    fun prunedBefore(cutoff: LocalDate): TodayChecks =
        copy(
            byDate = byDate.filterKeys { isNotBefore(it, cutoff) },
            signatures = signatures.filterKeys { isNotBefore(it, cutoff) }
        )

    fun merged(other: Map<String, Set<Int>>): TodayChecks =
        copy(byDate = (byDate.keys + other.keys).associateWith { byDate[it].orEmpty() + other[it].orEmpty() })

    private fun isNotBefore(key: String, cutoff: LocalDate): Boolean =
        runCatching { LocalDate.parse(key) }.getOrNull()?.isBefore(cutoff) != true
}

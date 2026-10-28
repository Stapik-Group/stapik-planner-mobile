package pl.stapik.planner.ui.plan

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The pager loops through the 7 weekdays forever (Fri -> Sat -> Sun -> Mon -> ...)
 * instead of representing a real, ever-advancing date axis like the calendar's
 * week pager does. [initialPageFor] is picked so the app always opens on today's
 * weekday, wherever that lands in the virtual page range.
 */
object DayPaging {
    private const val PAGE_COUNT_BASE = 100_000
    const val PAGE_COUNT = PAGE_COUNT_BASE

    fun initialPageFor(date: LocalDate): Int {
        val base = PAGE_COUNT_BASE / 2
        val dateIndex = date.dayOfWeek.value - 1
        val baseIndex = base % 7
        val delta = ((dateIndex - baseIndex) + 7) % 7
        return base + delta
    }

    fun pageToDayOfWeek(page: Int): DayOfWeek = DayOfWeek.of(1 + (page % 7))
}

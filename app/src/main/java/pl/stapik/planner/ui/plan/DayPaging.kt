package pl.stapik.planner.ui.plan

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The pager loops through the 7 weekdays forever (Fri -> Sat -> Sun -> Mon -> ...)
 * instead of representing a real, ever-advancing date axis like the calendar's
 * week pager does. [INITIAL_PAGE] is picked so the app always opens on today's
 * weekday, wherever that lands in the virtual page range.
 */
object DayPaging {
    private const val PAGE_COUNT_BASE = 100_000
    const val PAGE_COUNT = PAGE_COUNT_BASE

    val INITIAL_PAGE: Int = run {
        val base = PAGE_COUNT_BASE / 2
        val todayIndex = LocalDate.now().dayOfWeek.value - 1 // 0=Monday..6=Sunday
        val baseIndex = base % 7
        val delta = ((todayIndex - baseIndex) + 7) % 7
        base + delta
    }

    fun pageToDayOfWeek(page: Int): DayOfWeek = DayOfWeek.of(1 + (page % 7))
}

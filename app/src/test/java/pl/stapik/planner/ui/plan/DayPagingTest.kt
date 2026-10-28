package pl.stapik.planner.ui.plan

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DayPagingTest {
    @Test
    fun initialPageShowsTheWeekdayOfTheGivenDate() {
        val monday = LocalDate.of(2026, 10, 26)

        (0L..6L).forEach { offset ->
            val date = monday.plusDays(offset)
            assertEquals(date.dayOfWeek, DayPaging.pageToDayOfWeek(DayPaging.initialPageFor(date)))
        }
    }

    @Test
    fun initialPageIsInsideThePageRange() {
        val page = DayPaging.initialPageFor(LocalDate.of(2026, 10, 28))

        assertTrue(page in 0 until DayPaging.PAGE_COUNT)
    }
}

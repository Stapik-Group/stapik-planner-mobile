package pl.stapik.planner.data.localstate

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayChecksTest {
    private val monday = LocalDate.of(2026, 10, 26)

    @Test
    fun checkedSlotIsReportedWithItsSignature() {
        val checks = TodayChecks().with(monday, slotIndex = 1, checked = true, signature = "run")

        assertEquals(mapOf<Int, String?>(1 to "run"), checks.on(monday))
    }

    @Test
    fun uncheckingTheLastSlotRemovesTheDate() {
        val checks = TodayChecks()
            .with(monday, slotIndex = 1, checked = true, signature = "run")
            .with(monday, slotIndex = 1, checked = false, signature = "run")

        assertTrue(checks.on(monday).isEmpty())
        assertTrue(checks.byDate.isEmpty())
        assertTrue(checks.signatures.isEmpty())
    }

    @Test
    fun checksOfOneDateDoNotShowOnAnother() {
        val checks = TodayChecks().with(monday, slotIndex = 0, checked = true, signature = "run")

        assertTrue(checks.on(monday.plusDays(1)).isEmpty())
    }

    @Test
    fun checkingAgainReplacesTheSignature() {
        val checks = TodayChecks()
            .with(monday, slotIndex = 0, checked = true, signature = "run")
            .with(monday, slotIndex = 0, checked = true, signature = "swim")

        assertEquals(mapOf<Int, String?>(0 to "swim"), checks.on(monday))
    }

    @Test
    fun pruningDropsOlderDatesTogetherWithTheirSignatures() {
        val checks = TodayChecks()
            .with(monday.minusDays(3), slotIndex = 0, checked = true, signature = "old")
            .with(monday, slotIndex = 0, checked = true, signature = "new")
            .prunedBefore(monday.minusDays(2))

        assertTrue(checks.on(monday.minusDays(3)).isEmpty())
        assertEquals(mapOf<Int, String?>(0 to "new"), checks.on(monday))
        assertEquals(setOf(monday.toString()), checks.signatures.keys)
    }

    @Test
    fun mergedLegacyChecksHaveNoSignature() {
        val checks = TodayChecks().merged(mapOf(monday.toString() to setOf(0, 2)))

        assertEquals(mapOf<Int, String?>(0 to null, 2 to null), checks.on(monday))
    }
}

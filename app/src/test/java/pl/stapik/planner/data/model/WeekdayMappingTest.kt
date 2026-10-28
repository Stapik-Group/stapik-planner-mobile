package pl.stapik.planner.data.model

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeekdayMappingTest {
    @Test
    fun everyWireNameMapsToItsDay() {
        val expected = mapOf(
            "monday" to DayOfWeek.MONDAY,
            "tuesday" to DayOfWeek.TUESDAY,
            "wednesday" to DayOfWeek.WEDNESDAY,
            "thursday" to DayOfWeek.THURSDAY,
            "friday" to DayOfWeek.FRIDAY,
            "saturday" to DayOfWeek.SATURDAY,
            "sunday" to DayOfWeek.SUNDAY
        )

        expected.forEach { (wire, day) -> assertEquals(day, WeekdayMapping.fromWireValue(wire)) }
    }

    @Test
    fun everyDayHasItsWireName() {
        DayOfWeek.entries.forEach { day -> assertEquals(day, WeekdayMapping.fromWireValue(WeekdayMapping.toWireValue(day))) }
        assertEquals("monday", WeekdayMapping.toWireValue(DayOfWeek.MONDAY))
    }

    @Test
    fun unknownNameMapsToNothing() {
        assertNull(WeekdayMapping.fromWireValue("someday"))
        assertNull(WeekdayMapping.fromWireValue(""))
    }
}

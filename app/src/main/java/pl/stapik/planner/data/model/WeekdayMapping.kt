package pl.stapik.planner.data.model

import java.time.DayOfWeek

object WeekdayMapping {
    fun fromWireValue(value: String): DayOfWeek = when (value) {
        "tuesday" -> DayOfWeek.TUESDAY
        "wednesday" -> DayOfWeek.WEDNESDAY
        "thursday" -> DayOfWeek.THURSDAY
        "friday" -> DayOfWeek.FRIDAY
        "saturday" -> DayOfWeek.SATURDAY
        "sunday" -> DayOfWeek.SUNDAY
        else -> DayOfWeek.MONDAY
    }
}

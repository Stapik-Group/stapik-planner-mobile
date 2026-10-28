package pl.stapik.planner.data.model

import java.time.DayOfWeek

object WeekdayMapping {
    fun fromWireValue(value: String): DayOfWeek? = when (value) {
        "monday" -> DayOfWeek.MONDAY
        "tuesday" -> DayOfWeek.TUESDAY
        "wednesday" -> DayOfWeek.WEDNESDAY
        "thursday" -> DayOfWeek.THURSDAY
        "friday" -> DayOfWeek.FRIDAY
        "saturday" -> DayOfWeek.SATURDAY
        "sunday" -> DayOfWeek.SUNDAY
        else -> null
    }
}

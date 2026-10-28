package pl.stapik.planner.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ActivityDef(
    val name: String,
    val difficulty: String // "light" | "normal" | "medium" | "hard" — see Difficulty.fromWireValue
)

@Serializable
data class PlannerSettings(val slots: Int = 4)

@Serializable
data class DaySlotsWire(
    val weekday: String, // "monday".."sunday", lowercase — see WeekdayMapping
    val slots: List<ActivityDef?>
)

@Serializable
data class PlannerPayload(
    val settings: PlannerSettings = PlannerSettings(),
    val activities: List<ActivityDef> = emptyList(),
    val weekPlan: List<DaySlotsWire> = emptyList()
)

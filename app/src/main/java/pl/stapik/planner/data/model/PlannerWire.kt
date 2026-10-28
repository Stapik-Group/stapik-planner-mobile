package pl.stapik.planner.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ActivityDef(
    val name: String,
    val difficulty: String, // "light" | "normal" | "medium" | "hard" — see Difficulty.fromWireValue
    val id: String? = null
)

@Serializable
data class PlannerSettings(val slots: Int = DEFAULT_SLOTS) {
    val supportedSlots: Int get() = slots.coerceIn(MIN_SLOTS, MAX_SLOTS)

    companion object {
        const val DEFAULT_SLOTS = 4
        const val MIN_SLOTS = 1
        const val MAX_SLOTS = 8
    }
}

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

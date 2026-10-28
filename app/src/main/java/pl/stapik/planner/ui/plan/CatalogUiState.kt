package pl.stapik.planner.ui.plan

import pl.stapik.planner.data.edit.PlannerEdits
import pl.stapik.planner.data.model.ActivityDef
import pl.stapik.planner.data.model.PlannerPayload

data class CatalogUiState(val payload: PlannerPayload = PlannerPayload()) {
    val activities: List<ActivityDef> get() = payload.activities.filter { !it.id.isNullOrBlank() }

    val slotsPerDay: Int get() = payload.settings.supportedSlots

    fun plannedCount(activityId: String): Int = PlannerEdits.plannedSlotCount(payload, activityId)

    fun lostBy(slotCount: Int): Int = PlannerEdits.lostAssignments(payload, slotCount)
}

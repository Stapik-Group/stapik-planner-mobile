package pl.stapik.planner.data.document

import pl.stapik.common.model.DocumentCodec
import pl.stapik.common.model.SlotKey
import pl.stapik.planner.data.model.PlannerPayload

object PlannerDocument {
    val slot = SlotKey("planner.json")

    val codec = DocumentCodec(PlannerPayload.serializer()) { payload ->
        payload.weekPlan.isEmpty() && payload.activities.isEmpty()
    }
}

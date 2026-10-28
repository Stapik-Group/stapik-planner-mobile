package pl.stapik.planner.data.edit

import pl.stapik.planner.data.model.PlannerPayload

sealed interface EditResult {
    data class Applied(val payload: PlannerPayload) : EditResult
    data object LoadLimitExceeded : EditResult
    data object InvalidName : EditResult
    data object NameTaken : EditResult
    data object UnknownActivity : EditResult
    data object OutOfRange : EditResult
    data object Unavailable : EditResult
}

package pl.stapik.planner.ui.plan

sealed interface PlanLoadError {
    data object NoNetwork : PlanLoadError
    data object Unauthorized : PlanLoadError
    data object NotFound : PlanLoadError
    data class Unknown(val message: String) : PlanLoadError
}

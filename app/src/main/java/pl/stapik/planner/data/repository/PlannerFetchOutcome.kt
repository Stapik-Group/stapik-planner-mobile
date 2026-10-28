package pl.stapik.planner.data.repository

sealed interface PlannerFetchOutcome {
    data class Fresh(val result: PlannerFetchResult) : PlannerFetchOutcome
    data class Cached(val result: PlannerFetchResult, val cause: Throwable) : PlannerFetchOutcome
    data class Failure(val cause: Throwable) : PlannerFetchOutcome
}

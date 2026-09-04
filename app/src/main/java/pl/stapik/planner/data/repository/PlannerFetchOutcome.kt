package pl.stapik.planner.data.repository

import pl.stapik.planner.data.cache.CachedPlanner

sealed interface PlannerFetchOutcome {
    data class Fresh(val result: PlannerFetchResult) : PlannerFetchOutcome
    data class Cached(val cached: CachedPlanner, val cause: Throwable) : PlannerFetchOutcome
    data class Failure(val cause: Throwable) : PlannerFetchOutcome
}

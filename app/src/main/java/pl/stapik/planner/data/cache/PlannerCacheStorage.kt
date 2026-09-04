package pl.stapik.planner.data.cache

import pl.stapik.planner.data.model.PlannerPayload

interface PlannerCacheStorage {
    suspend fun load(): CachedPlanner?
    suspend fun save(payload: PlannerPayload, updatedAt: String)
}

data class CachedPlanner(
    val payload: PlannerPayload,
    val updatedAt: String
)

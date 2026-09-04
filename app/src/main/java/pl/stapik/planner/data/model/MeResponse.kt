package pl.stapik.planner.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MeResponse(
    val extensionId: String,
    val extensionSlug: String,
    val keyLabel: String? = null,
    val scope: String,
    val lastUsedAt: String? = null
)
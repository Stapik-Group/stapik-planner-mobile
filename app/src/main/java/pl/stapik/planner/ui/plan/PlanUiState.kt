package pl.stapik.planner.ui.plan

import java.time.DayOfWeek
import pl.stapik.planner.data.model.DaySlotsWire

sealed interface PlanUiState {
    data object Loading : PlanUiState
    data class Success(
        val slotsPerDay: Int,
        val daysByWeekday: Map<DayOfWeek, DaySlotsWire>,
        val isRefreshing: Boolean = false,
        val isStale: Boolean = false,
        val updatedAt: String? = null
    ) : PlanUiState
    data class Error(val error: PlanLoadError) : PlanUiState
    data object NotConfigured : PlanUiState
}

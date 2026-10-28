package pl.stapik.planner.ui.plan

import java.time.DayOfWeek
import pl.stapik.common.presentation.LoadErrorKind
import pl.stapik.planner.data.model.DaySlotsWire

sealed interface PlanUiState {
    data object Loading : PlanUiState
    data class Success(
        val slotsPerDay: Int,
        val daysByWeekday: Map<DayOfWeek, DaySlotsWire>,
        val isRefreshing: Boolean = false
    ) : PlanUiState
    data class Error(val kind: LoadErrorKind) : PlanUiState
}

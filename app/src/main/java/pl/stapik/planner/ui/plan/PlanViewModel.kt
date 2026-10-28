package pl.stapik.planner.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.stapik.planner.data.localstate.TodayChecksStorage
import pl.stapik.planner.data.model.WeekdayMapping
import pl.stapik.planner.data.repository.MissingConfigException
import pl.stapik.planner.data.repository.PlannerFetchOutcome
import pl.stapik.planner.data.repository.PlannerFetchResult
import pl.stapik.planner.data.repository.PlannerRepository

class PlanViewModel(
    private val repository: PlannerRepository,
    private val todayChecksStorage: TodayChecksStorage
) : ViewModel() {
    private val _uiState = MutableStateFlow<PlanUiState>(PlanUiState.Loading)
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    val todayChecked: StateFlow<Set<Int>> = todayChecksStorage.observeChecked(LocalDate.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun refresh() {
        viewModelScope.launch {
            val currentSuccess = _uiState.value as? PlanUiState.Success
            _uiState.value = currentSuccess?.copy(isRefreshing = true) ?: PlanUiState.Loading
            _uiState.value = when (val outcome = repository.fetchPlan()) {
                is PlannerFetchOutcome.Fresh -> outcome.result.toSuccessState(isStale = false)
                is PlannerFetchOutcome.Cached -> outcome.result.toSuccessState(isStale = true)
                is PlannerFetchOutcome.Failure -> mapError(outcome.cause)
            }
        }
    }

    fun toggleTodayCheck(slotIndex: Int, checked: Boolean) {
        viewModelScope.launch {
            todayChecksStorage.setChecked(date = LocalDate.now(), slotIndex = slotIndex, checked = checked)
        }
    }

    private fun PlannerFetchResult.toSuccessState(isStale: Boolean) = PlanUiState.Success(
        slotsPerDay = payload.settings.slots,
        daysByWeekday = payload.weekPlan.associateBy { WeekdayMapping.fromWireValue(it.weekday) },
        isStale = isStale,
        updatedAt = updatedAt
    )

    private fun mapError(error: Throwable): PlanUiState = when (error) {
        is MissingConfigException -> PlanUiState.NotConfigured
        else -> PlanUiState.Error(error.toPlanLoadError())
    }
}

class PlanViewModelFactory(
    private val repository: PlannerRepository,
    private val todayChecksStorage: TodayChecksStorage
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PlanViewModel(repository, todayChecksStorage) as T
}

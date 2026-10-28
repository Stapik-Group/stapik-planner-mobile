package pl.stapik.planner.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.stapik.common.presentation.DocumentController
import pl.stapik.common.presentation.DocumentUiState
import pl.stapik.common.presentation.LoadErrorKind
import pl.stapik.common.presentation.SyncUiState
import pl.stapik.common.sync.FirstConnectionChoice
import pl.stapik.common.sync.SyncEngine
import pl.stapik.common.sync.SyncOutcome
import pl.stapik.planner.data.config.ApiConfigStorage
import pl.stapik.planner.data.document.PlannerDocument
import pl.stapik.planner.data.edit.EditResult
import pl.stapik.planner.data.edit.PlannerEdits
import pl.stapik.planner.data.localstate.TodayChecksStorage
import pl.stapik.planner.data.model.ActivityDef
import pl.stapik.planner.data.model.Difficulty
import pl.stapik.planner.data.model.PlannerPayload
import pl.stapik.planner.data.model.checkSignature
import pl.stapik.planner.data.model.WeekdayMapping

class PlanViewModel(
    private val controller: DocumentController<PlannerPayload>,
    private val todayChecksStorage: TodayChecksStorage
) : ViewModel() {
    private val _uiState = MutableStateFlow<PlanUiState>(PlanUiState.Loading)
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    val syncState: StateFlow<SyncUiState> = controller.sync

    private val _edit = MutableStateFlow(EditUiState())
    val editState: StateFlow<EditUiState> = _edit.asStateFlow()

    private val _catalog = MutableStateFlow(CatalogUiState())
    val catalog: StateFlow<CatalogUiState> = _catalog.asStateFlow()

    private val _sourceChoice = MutableStateFlow<SourceChoice?>(null)
    val sourceChoice: StateFlow<SourceChoice?> = _sourceChoice.asStateFlow()

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayChecked: StateFlow<Set<Int>> = combine(
        _today.flatMapLatest { date -> todayChecksStorage.observeChecked(date) },
        _uiState,
        _today
    ) { checked, state, date ->
        val slots = slotsOf(state, date)
        checked.filter { (index, signature) -> signature == null || signature == slots.getOrNull(index).checkSignature }.keys
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    suspend fun trackToday() {
        while (true) {
            val now = ZonedDateTime.now()
            _today.value = now.toLocalDate()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(Duration.between(now, nextMidnight).toMillis())
        }
    }

    private var refreshJob: Job? = null

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            val shown = _uiState.value as? PlanUiState.Success
            _uiState.value = shown?.copy(isRefreshing = true) ?: PlanUiState.Loading
            guarded {
                controller.load()
                publish(synced = false)
                if (controller.document.value is DocumentUiState.Success) {
                    askForSourceIfNeeded(controller.sync())
                }
                publish(synced = true)
            }
        }
    }

    fun chooseSource(choice: FirstConnectionChoice) {
        _sourceChoice.value = null
        viewModelScope.launch {
            guarded {
                askForSourceIfNeeded(controller.sync(choice))
                publish(synced = true)
            }
        }
    }

    fun dismissSourceChoice() {
        _sourceChoice.value = null
    }

    fun discardLocalAndResync() {
        viewModelScope.launch {
            _uiState.value = PlanUiState.Loading
            guarded {
                controller.discardLocalAndResync()
                publish(synced = true)
            }
        }
    }

    fun toggleEditing() {
        _edit.update { EditUiState(isEditing = !it.isEditing) }
    }

    fun dismissEditNotice() {
        _edit.update { it.copy(notice = null) }
    }

    fun assignSlot(weekday: DayOfWeek, slotIndex: Int, activityId: String?) {
        perform(operation = { payload -> PlannerEdits.assign(payload, weekday, slotIndex, activityId) }) { result ->
            if (result == EditResult.LoadLimitExceeded) _edit.update { it.copy(notice = EditNotice.LOAD_LIMIT) }
        }
    }

    fun addActivity(name: String, difficulty: Difficulty, onResult: (EditResult) -> Unit) {
        perform(operation = { payload -> PlannerEdits.addActivity(payload, name, difficulty) }, onResult = onResult)
    }

    fun updateActivity(id: String, name: String, difficulty: Difficulty, onResult: (EditResult) -> Unit) {
        perform(operation = { payload -> PlannerEdits.updateActivity(payload, id, name, difficulty) }, onResult = onResult)
    }

    fun removeActivity(id: String, onResult: (EditResult) -> Unit = {}) {
        perform(operation = { payload -> PlannerEdits.removeActivity(payload, id) }, onResult = onResult)
    }

    fun setSlotCount(count: Int, onResult: (EditResult) -> Unit = {}) {
        perform(operation = { payload -> PlannerEdits.setSlotCount(payload, count) }, onResult = onResult)
    }

    fun dismissNotice() {
        viewModelScope.launch { guarded { controller.dismissResolvedNotice() } }
    }

    fun toggleTodayCheck(slotIndex: Int, checked: Boolean) {
        viewModelScope.launch {
            val date = _today.value
            val signature = slotsOf(_uiState.value, date).getOrNull(slotIndex).checkSignature
            todayChecksStorage.setChecked(date = date, slotIndex = slotIndex, checked = checked, signature = signature)
        }
    }

    private fun slotsOf(state: PlanUiState, date: LocalDate): List<ActivityDef?> =
        (state as? PlanUiState.Success)?.daysByWeekday?.get(date.dayOfWeek)?.slots.orEmpty()

    private fun perform(operation: (PlannerPayload) -> EditResult, onResult: (EditResult) -> Unit) {
        viewModelScope.launch {
            val shown = controller.document.value as? DocumentUiState.Success
            val preview = if (shown == null) EditResult.Unavailable else operation(shown.value)
            if (preview is EditResult.Applied) {
                guarded {
                    afterEdit(controller.edit { latest -> (operation(latest) as? EditResult.Applied)?.payload ?: latest })
                }
            }
            onResult(preview)
        }
    }

    private fun afterEdit(outcome: SyncOutcome) {
        askForSourceIfNeeded(outcome)
        _edit.update { it.copy(notice = null) }
        publish(synced = true)
    }

    private fun askForSourceIfNeeded(outcome: SyncOutcome) {
        if (outcome is SyncOutcome.NeedsFirstConnectionChoice) {
            _sourceChoice.value = SourceChoice(outcome.canKeepLocal)
        }
    }

    private suspend fun guarded(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.value = PlanUiState.Error(LoadErrorKind.UNKNOWN)
        }
    }

    private fun publish(synced: Boolean) {
        _uiState.value = when (val document = controller.document.value) {
            DocumentUiState.Loading -> PlanUiState.Loading
            is DocumentUiState.Error -> PlanUiState.Error(document.kind)
            is DocumentUiState.Success -> {
                _catalog.value = CatalogUiState(document.value)
                if (synced || !PlannerDocument.codec.isEmpty(document.value)) {
                    document.value.toSuccessState()
                } else {
                    PlanUiState.Loading
                }
            }
        }
    }

    private fun PlannerPayload.toSuccessState() = PlanUiState.Success(
        slotsPerDay = settings.supportedSlots,
        activities = activities.filter { !it.id.isNullOrBlank() },
        daysByWeekday = weekPlan
            .mapNotNull { day -> WeekdayMapping.fromWireValue(day.weekday)?.let { it to day } }
            .toMap()
    )
}

class PlanViewModelFactory(
    private val engine: SyncEngine<PlannerPayload>,
    private val apiConfigStorage: ApiConfigStorage,
    private val todayChecksStorage: TodayChecksStorage
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val controller = DocumentController(engine = engine, connected = { apiConfigStorage.load() != null })
        return PlanViewModel(controller, todayChecksStorage) as T
    }
}

package pl.stapik.planner.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.launch
import pl.stapik.common.i18n.Messages
import pl.stapik.common.presentation.SyncAction
import pl.stapik.common.ui.components.BannerAction
import pl.stapik.common.ui.components.StapikBanner
import pl.stapik.common.ui.sync.FirstConnectionDialog
import pl.stapik.common.ui.sync.LoadErrorMessage
import pl.stapik.common.ui.sync.SyncStatusBanner
import pl.stapik.common.ui.theme.StapikTheme
import pl.stapik.planner.R
import pl.stapik.planner.data.edit.PlannerEdits
import pl.stapik.planner.ui.theme.LocalThemeColors
import pl.stapik.planner.util.AppInfo

@Composable
fun PlanPagerScreen(
    viewModelFactory: ViewModelProvider.Factory,
    onNavigateToConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val viewModel: PlanViewModel = viewModel(factory = viewModelFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val sourceChoice by viewModel.sourceChoice.collectAsStateWithLifecycle()
    val editState by viewModel.editState.collectAsStateWithLifecycle()
    var pickerTarget by remember { mutableStateOf<SlotTarget?>(null) }
    val context = LocalContext.current
    val todayChecked by viewModel.todayChecked.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = DayPaging.initialPageFor(LocalDate.now())) { DayPaging.PAGE_COUNT }
    val coroutineScope = rememberCoroutineScope()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val themeColors = LocalThemeColors.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { viewModel.trackToday() }
            viewModel.refresh()
        }
    }

    Column(modifier = modifier.fillMaxSize().background(themeColors.windowBackground)) {
        val currentDayOfWeek = DayPaging.pageToDayOfWeek(pagerState.currentPage)
        val isCurrentPageToday = currentDayOfWeek == today.dayOfWeek

        DayNavBar(
            dayOfWeek = currentDayOfWeek,
            isToday = isCurrentPageToday,
            onPreviousDay = {
                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            },
            onNextDay = {
                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            onJumpToToday = {
                val todayIndex = today.dayOfWeek.value - 1
                val currentIndex = pagerState.currentPage % 7
                val delta = ((todayIndex - currentIndex) + 7) % 7
                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + delta) }
            }
        )

        if (state is PlanUiState.Success) {
            SyncStatusBanner(
                state = syncState,
                onAction = { action ->
                    when (action) {
                        SyncAction.RETRY, SyncAction.CHOOSE_SOURCE -> viewModel.refresh()
                        SyncAction.DISMISS -> viewModel.dismissNotice()
                        SyncAction.RECONNECT, SyncAction.ENTER_PASSPHRASE -> onNavigateToConnect()
                        SyncAction.UPDATE_APP -> uriHandler.openUri(AppInfo.RELEASES_URL)
                    }
                }
            )
        }

        if (state is PlanUiState.Success) {
            EditBar(
                state = editState,
                onToggleEditing = viewModel::toggleEditing
            )
            if (editState.notice == EditNotice.LOAD_LIMIT) {
                StapikBanner(
                    text = stringResource(R.string.edit_load_limit, PlannerEdits.LOAD_LIMIT),
                    dotColor = StapikTheme.colors.syncConflict,
                    actions = listOf(BannerAction(Messages.text(context, "action.dismiss"), viewModel::dismissEditNotice))
                )
            }
        }

        val isRefreshing = (state as? PlanUiState.Success)?.isRefreshing ?: false

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f)
        ) {
            when (val current = state) {
                is PlanUiState.Loading -> CenteredMessage(stringResource(R.string.loading))

                is PlanUiState.Error -> LoadErrorMessage(
                    kind = current.kind,
                    onDiscardLocal = viewModel::discardLocalAndResync
                )

                is PlanUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            val pageDayOfWeek = DayPaging.pageToDayOfWeek(page)
                            DayColumnPage(
                                dayOfWeek = pageDayOfWeek,
                                daySlots = current.daysByWeekday[pageDayOfWeek],
                                slotsPerDay = current.slotsPerDay,
                                isToday = pageDayOfWeek == today.dayOfWeek,
                                isEditing = editState.isEditing,
                                todayChecked = todayChecked,
                                onToggleCheck = viewModel::toggleTodayCheck,
                                onSlotClick = { slotIndex -> pickerTarget = SlotTarget(pageDayOfWeek, slotIndex) }
                            )
                        }
                    }
                }
            }
        }
    }

    val target = pickerTarget
    val shown = state as? PlanUiState.Success
    if (target != null && shown != null) {
        ActivityPickerDialog(
            activities = shown.activities,
            current = shown.daysByWeekday[target.weekday]?.slots?.getOrNull(target.slotIndex),
            onPick = { activityId ->
                viewModel.assignSlot(target.weekday, target.slotIndex, activityId)
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null }
        )
    }

    sourceChoice?.let { choice ->
        FirstConnectionDialog(
            canKeepLocal = choice.canKeepLocal,
            onChoose = viewModel::chooseSource,
            onCancel = viewModel::dismissSourceChoice
        )
    }
}

private data class SlotTarget(val weekday: DayOfWeek, val slotIndex: Int)

@Composable
private fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    val themeColors = LocalThemeColors.current
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = themeColors.textDark)
    }
}

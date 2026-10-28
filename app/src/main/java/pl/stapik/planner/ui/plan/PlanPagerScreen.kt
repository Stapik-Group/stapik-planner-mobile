package pl.stapik.planner.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import kotlinx.coroutines.launch
import pl.stapik.common.presentation.SyncAction
import pl.stapik.common.presentation.isQuiet
import pl.stapik.common.ui.sync.LoadErrorMessage
import pl.stapik.common.ui.sync.SyncStatusBanner
import pl.stapik.planner.R
import pl.stapik.planner.ui.theme.LocalThemeColors
import pl.stapik.planner.ui.theme.themedSurface
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
    val todayChecked by viewModel.todayChecked.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = DayPaging.initialPageFor(LocalDate.now())) { DayPaging.PAGE_COUNT }
    val coroutineScope = rememberCoroutineScope()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val themeColors = LocalThemeColors.current

    LaunchedEffect(Unit) { viewModel.refresh() }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.trackToday() }
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

        if (state is PlanUiState.Success || state is PlanUiState.Empty) {
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

        val isRefreshing = (state as? PlanUiState.Success)?.isRefreshing ?: false

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f)
        ) {
            when (val current = state) {
                is PlanUiState.Loading -> CenteredMessage(stringResource(R.string.loading))

                is PlanUiState.NotConfigured -> CenteredMessageWithAction(
                    message = stringResource(R.string.not_configured_message),
                    actionLabel = stringResource(R.string.not_configured_button),
                    onAction = onNavigateToConnect
                )

                is PlanUiState.Empty ->
                    if (syncState.isQuiet) {
                        CenteredMessage(stringResource(R.string.error_not_found))
                    } else {
                        Box(modifier = Modifier.fillMaxSize())
                    }

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
                                todayChecked = todayChecked,
                                onToggleCheck = viewModel::toggleTodayCheck
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    val themeColors = LocalThemeColors.current
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = themeColors.textDark)
    }
}

@Composable
private fun CenteredMessageWithAction(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalThemeColors.current
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = themeColors.textDark)
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .themedSurface(themeColors = themeColors, backgroundColor = themeColors.cellBackground, raised = true)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(actionLabel, color = themeColors.textDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

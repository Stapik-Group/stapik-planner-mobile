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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import kotlinx.coroutines.launch
import pl.stapik.common.cloud.HttpTransport
import pl.stapik.common.i18n.Messages
import pl.stapik.planner.R
import pl.stapik.planner.data.cache.DataStorePlannerCacheStorage
import pl.stapik.planner.data.config.ApiConfigStorage
import pl.stapik.planner.data.localstate.DataStoreTodayChecksStorage
import pl.stapik.planner.data.repository.PlannerRepository
import pl.stapik.planner.ui.theme.LocalThemeColors
import pl.stapik.planner.ui.theme.retroBevel

@Composable
fun PlanPagerScreen(
    apiConfigStorage: ApiConfigStorage,
    transport: HttpTransport,
    onNavigateToConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: PlanViewModel = viewModel(
        factory = remember {
            PlanViewModelFactory(
                repository = PlannerRepository(
                    apiConfigStorage = apiConfigStorage,
                    cacheStorage = DataStorePlannerCacheStorage(context.applicationContext),
                    transport = transport
                ),
                todayChecksStorage = DataStoreTodayChecksStorage(context.applicationContext)
            )
        }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val todayChecked by viewModel.todayChecked.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = DayPaging.INITIAL_PAGE) { DayPaging.PAGE_COUNT }
    val coroutineScope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val themeColors = LocalThemeColors.current

    LaunchedEffect(Unit) { viewModel.refresh() }

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

                is PlanUiState.Error -> {
                    val message = when (val error = current.error) {
                        PlanLoadError.NoNetwork -> stringResource(R.string.error_no_network)
                        PlanLoadError.Unauthorized -> stringResource(R.string.error_unauthorized)
                        PlanLoadError.NotFound -> stringResource(R.string.error_not_found)
                        is PlanLoadError.Unreadable -> Messages.text(context, error.messageKey)
                        is PlanLoadError.Unknown -> stringResource(R.string.error_unknown, error.message)
                    }
                    CenteredMessageWithAction(
                        message = message,
                        actionLabel = stringResource(R.string.retry),
                        onAction = viewModel::refresh
                    )
                }

                is PlanUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (current.isStale) {
                            Box(
                                modifier = Modifier.fillMaxWidth().background(themeColors.cellBackground).padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.cached_data_banner, formatCachedTimestamp(current.updatedAt)),
                                    color = themeColors.textDark
                                )
                            }
                        }
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

private fun formatCachedTimestamp(updatedAt: String?): String {
    if (updatedAt == null) return ""
    return runCatching {
        val instant = java.time.Instant.parse(updatedAt)
        java.time.format.DateTimeFormatter
            .ofLocalizedDateTime(java.time.format.FormatStyle.SHORT)
            .withZone(java.time.ZoneId.systemDefault())
            .format(instant)
    }.getOrDefault(updatedAt)
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
                    .background(themeColors.cellBackground)
                    .retroBevel(raised = true)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(actionLabel, color = themeColors.textDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

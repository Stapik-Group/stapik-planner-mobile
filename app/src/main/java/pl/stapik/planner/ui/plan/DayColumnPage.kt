package pl.stapik.planner.ui.plan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.stapik.planner.R
import pl.stapik.planner.data.model.ActivityDef
import pl.stapik.planner.data.model.DaySlotsWire
import pl.stapik.planner.data.model.Difficulty
import pl.stapik.planner.ui.theme.LocalThemeColors
import java.time.DayOfWeek

@Composable
fun DayColumnPage(
    dayOfWeek: DayOfWeek,
    daySlots: DaySlotsWire?,
    slotsPerDay: Int,
    isToday: Boolean,
    todayChecked: Set<Int>,
    onToggleCheck: (slotIndex: Int, checked: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalThemeColors.current
    val slots: List<ActivityDef?> = remember(daySlots, slotsPerDay) {
        val raw = daySlots?.slots.orEmpty()
        List(slotsPerDay) { index -> raw.getOrNull(index) }
    }
    val loadPercent = slots.sumOf { it?.let { activity -> Difficulty.fromWireValue(activity.difficulty).loadPercent } ?: 0 }

    Column(modifier = modifier.fillMaxSize()) {
        DayLoadBar(loadPercent = loadPercent)

        if (isToday) {
            Text(
                text = stringResource(R.string.local_check_hint),
                color = themeColors.textMuted,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(slots.size) { index ->
                Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) {
                    SlotRow(
                        activity = slots[index],
                        showCheckbox = isToday,
                        checked = index in todayChecked,
                        onCheckedChange = { checked -> onToggleCheck(index, checked) }
                    )
                }
            }
        }
    }
}

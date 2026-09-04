package pl.stapik.planner.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.format.TextStyle
import pl.stapik.planner.R
import pl.stapik.planner.ui.theme.LocalThemeColors
import pl.stapik.planner.ui.theme.themedSurface

@Composable
fun DayNavBar(
    dayOfWeek: DayOfWeek,
    isToday: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onJumpToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val label = remember(dayOfWeek, locale) {
        dayOfWeek.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase() }
    }
    val themeColors = LocalThemeColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(themeColors.windowBackground)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavButton(label = "\u25C0", onClick = onPreviousDay)

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .themedSurface(themeColors = themeColors, backgroundColor = themeColors.accent, raised = false)
                .clickable(enabled = !isToday, onClick = onJumpToToday)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isToday) "${stringResource(R.string.today_label)} \u00B7 $label" else label,
                color = themeColors.textOnAccent,
                fontWeight = FontWeight.Bold
            )
        }

        NavButton(label = "\u25B6", onClick = onNextDay)
    }
}

@Composable
private fun NavButton(label: String, onClick: () -> Unit) {
    val themeColors = LocalThemeColors.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .themedSurface(themeColors = themeColors, backgroundColor = themeColors.cellBackground, raised = true)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = themeColors.textDark)
    }
}

package pl.stapik.planner.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.min
import pl.stapik.planner.R
import pl.stapik.planner.ui.theme.LocalThemeColors
import pl.stapik.planner.ui.theme.themedSurface

@Composable
fun DayLoadBar(loadPercent: Int, modifier: Modifier = Modifier) {
    val themeColors = LocalThemeColors.current
    val clampedFraction = min(loadPercent, 100) / 100f
    val isOverloaded = loadPercent > 100

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .themedSurface(themeColors = themeColors, backgroundColor = themeColors.cellBackgroundSecondary, raised = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clampedFraction)
                    .background(if (isOverloaded) OVERLOAD_COLOR else themeColors.accent)
            )
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.day_load_label, loadPercent),
                    color = themeColors.textDark
                )
            }
        }
    }
}

private val OVERLOAD_COLOR = androidx.compose.ui.graphics.Color(0xFFB00020)

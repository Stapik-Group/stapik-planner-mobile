package pl.stapik.planner.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import pl.stapik.planner.R
import pl.stapik.planner.ui.theme.LocalThemeColors

@Composable
fun DayLoadBar(loadPercent: Int, modifier: Modifier = Modifier) {
    val themeColors = LocalThemeColors.current
    val fraction = min(loadPercent, 100) / 100f
    val isOverloaded = loadPercent > 100
    val fillColor = if (isOverloaded) OVERLOAD_COLOR else themeColors.accent
    val shape = RoundedCornerShape(BAR_HEIGHT / 2)

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.day_load_label, loadPercent),
            color = if (isOverloaded) OVERLOAD_COLOR else themeColors.textDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .clip(shape)
                .background(themeColors.cellBackgroundSecondary)
                .border(1.dp, themeColors.borderLight, shape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(fillColor)
            )
        }
    }
}

private val BAR_HEIGHT = 10.dp
private val OVERLOAD_COLOR = Color(0xFFB00020)

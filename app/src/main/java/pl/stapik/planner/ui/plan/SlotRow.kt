package pl.stapik.planner.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import pl.stapik.planner.R
import pl.stapik.planner.data.model.ActivityDef
import pl.stapik.planner.data.model.Difficulty
import pl.stapik.planner.ui.theme.LocalThemeColors

@Composable
fun SlotRow(
    activity: ActivityDef?,
    showCheckbox: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalThemeColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(themeColors.cellBackgroundSecondary)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (activity == null) {
            Text(
                text = stringResource(R.string.empty_slot),
                color = themeColors.textMuted,
                modifier = Modifier.weight(1f)
            )
        } else {
            val difficulty = Difficulty.fromWireValue(activity.difficulty)
            Text(
                text = activity.name,
                color = themeColors.textDark,
                fontWeight = FontWeight.Medium,
                textDecoration = if (checked) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${difficultyLabel(difficulty)} (${difficulty.loadPercent}%)",
                color = themeColors.textMuted
            )
        }

        if (showCheckbox && activity != null) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = themeColors.accent)
            )
        }
    }
}

@Composable
private fun difficultyLabel(difficulty: Difficulty): String = when (difficulty) {
    Difficulty.LIGHT -> stringResource(R.string.difficulty_light)
    Difficulty.NORMAL -> stringResource(R.string.difficulty_normal)
    Difficulty.MEDIUM -> stringResource(R.string.difficulty_medium)
    Difficulty.HARD -> stringResource(R.string.difficulty_hard)
}

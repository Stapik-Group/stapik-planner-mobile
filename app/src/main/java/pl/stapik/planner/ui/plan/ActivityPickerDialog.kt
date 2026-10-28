package pl.stapik.planner.ui.plan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.stapik.common.i18n.Messages
import pl.stapik.common.ui.components.StapikDialog
import pl.stapik.common.ui.components.StapikOptionRow
import pl.stapik.planner.R
import pl.stapik.planner.data.model.ActivityDef
import pl.stapik.planner.data.model.Difficulty

@Composable
fun ActivityPickerDialog(
    activities: List<ActivityDef>,
    current: ActivityDef?,
    onPick: (activityId: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    StapikDialog(
        title = stringResource(R.string.edit_pick_title),
        confirmLabel = stringResource(R.string.edit_clear_slot),
        onConfirm = { onPick(null) },
        onDismiss = onDismiss,
        dismissLabel = Messages.text(context, "action.cancel")
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            if (activities.isEmpty()) {
                Text(stringResource(R.string.edit_no_activities))
            }
            activities.forEach { activity ->
                val difficulty = Difficulty.fromWireValue(activity.difficulty)
                StapikOptionRow(
                    label = "${activity.name} (${difficultyLabel(difficulty)}, ${difficulty.loadPercent}%)",
                    selected = activity.id == current?.id,
                    onClick = { onPick(activity.id) }
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

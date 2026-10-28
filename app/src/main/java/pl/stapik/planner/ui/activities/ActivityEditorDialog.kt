package pl.stapik.planner.ui.activities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.stapik.common.i18n.Messages
import pl.stapik.common.ui.components.StapikButton
import pl.stapik.common.ui.components.StapikDialog
import pl.stapik.common.ui.components.StapikOptionRow
import pl.stapik.common.ui.components.StapikTextField
import pl.stapik.planner.R
import pl.stapik.planner.data.edit.EditResult
import pl.stapik.planner.data.edit.PlannerEdits
import pl.stapik.planner.data.model.ActivityDef
import pl.stapik.planner.data.model.Difficulty
import pl.stapik.planner.ui.plan.difficultyLabel

@Composable
fun ActivityEditorDialog(
    activity: ActivityDef?,
    plannedSlots: Int,
    onSubmit: (name: String, difficulty: Difficulty, onResult: (EditResult) -> Unit) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(activity?.name.orEmpty()) }
    var difficulty by remember { mutableStateOf(Difficulty.fromWireValue(activity?.difficulty.orEmpty())) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmingDelete by remember { mutableStateOf(false) }
    val invalidName = stringResource(R.string.activity_error_name, PlannerEdits.MAX_NAME_LENGTH)
    val nameTaken = stringResource(R.string.activity_error_taken)
    val loadLimit = stringResource(R.string.edit_load_limit, PlannerEdits.LOAD_LIMIT)

    StapikDialog(
        title = stringResource(if (activity == null) R.string.activity_new_title else R.string.activity_edit_title),
        confirmLabel = stringResource(R.string.activity_save),
        onConfirm = {
            onSubmit(name, difficulty) { result ->
                when (result) {
                    is EditResult.Applied -> onDismiss()
                    EditResult.InvalidName -> error = invalidName
                    EditResult.NameTaken -> error = nameTaken
                    EditResult.LoadLimitExceeded -> error = loadLimit
                    else -> onDismiss()
                }
            }
        },
        onDismiss = onDismiss,
        dismissLabel = Messages.text(context, "action.cancel")
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StapikTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                label = stringResource(R.string.activity_name),
                error = error,
                modifier = Modifier.fillMaxWidth()
            )
            Difficulty.entries.forEach { option ->
                StapikOptionRow(
                    label = "${difficultyLabel(option)} (${option.loadPercent}%)",
                    selected = option == difficulty,
                    onClick = { difficulty = option }
                )
            }
            if (activity != null) {
                StapikButton(
                    text = stringResource(R.string.activity_delete),
                    onClick = { confirmingDelete = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (confirmingDelete) {
        StapikDialog(
            title = stringResource(R.string.activity_delete_title),
            confirmLabel = stringResource(R.string.activity_delete),
            onConfirm = {
                confirmingDelete = false
                onDelete()
            },
            onDismiss = { confirmingDelete = false },
            dismissLabel = Messages.text(context, "action.cancel")
        ) {
            Text(stringResource(R.string.activity_delete_message, plannedSlots))
        }
    }
}

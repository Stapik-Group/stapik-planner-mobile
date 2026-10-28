package pl.stapik.planner.ui.activities

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.stapik.common.i18n.Messages
import pl.stapik.common.ui.components.StapikButton
import pl.stapik.common.ui.components.StapikDialog
import pl.stapik.common.ui.components.StapikOptionRow
import pl.stapik.common.ui.components.StapikScreen
import pl.stapik.planner.R
import pl.stapik.planner.data.model.ActivityDef
import pl.stapik.planner.data.model.Difficulty
import pl.stapik.planner.data.model.PlannerSettings
import pl.stapik.planner.ui.plan.PlanViewModel
import pl.stapik.planner.ui.plan.difficultyLabel

private data class EditorTarget(val activity: ActivityDef?)

@Composable
fun ActivitiesScreen(
    viewModelFactory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: PlanViewModel = viewModel(factory = viewModelFactory)
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editor by remember { mutableStateOf<EditorTarget?>(null) }
    var pendingSlotCount by remember { mutableStateOf<Int?>(null) }
    val textColor = MaterialTheme.colorScheme.onBackground

    val changeSlotCount: (Int) -> Unit = { requested ->
        if (catalog.lostBy(requested) > 0) pendingSlotCount = requested else viewModel.setSlotCount(requested)
    }

    BackHandler(onBack = onBack)

    StapikScreen(
        title = stringResource(R.string.activities_title),
        onBack = onBack,
        backDescription = Messages.text(context, "action.back"),
        modifier = modifier
    ) {
        StapikButton(
            text = stringResource(R.string.activities_add),
            onClick = { editor = EditorTarget(null) },
            modifier = Modifier.fillMaxWidth()
        )
        if (catalog.activities.isEmpty()) {
            Text(stringResource(R.string.edit_no_activities), color = textColor)
        }
        catalog.activities.forEach { activity ->
            val difficulty = Difficulty.fromWireValue(activity.difficulty)
            StapikOptionRow(
                label = "${activity.name} (${difficultyLabel(difficulty)}, ${difficulty.loadPercent}%)",
                selected = false,
                onClick = { editor = EditorTarget(activity) }
            )
        }

        Text(stringResource(R.string.slots_title), color = textColor, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            StapikButton(
                text = "−",
                onClick = { changeSlotCount(catalog.slotsPerDay - 1) },
                enabled = catalog.slotsPerDay > PlannerSettings.MIN_SLOTS
            )
            Text(catalog.slotsPerDay.toString(), color = textColor, fontWeight = FontWeight.Bold)
            StapikButton(
                text = "+",
                onClick = { changeSlotCount(catalog.slotsPerDay + 1) },
                enabled = catalog.slotsPerDay < PlannerSettings.MAX_SLOTS
            )
        }
    }

    editor?.let { target ->
        ActivityEditorDialog(
            activity = target.activity,
            plannedSlots = target.activity?.id?.let { catalog.plannedCount(it) } ?: 0,
            onSubmit = { name, difficulty, onResult ->
                val id = target.activity?.id
                if (id == null) {
                    viewModel.addActivity(name, difficulty, onResult)
                } else {
                    viewModel.updateActivity(id, name, difficulty, onResult)
                }
            },
            onDelete = {
                target.activity?.id?.let { viewModel.removeActivity(it) }
                editor = null
            },
            onDismiss = { editor = null }
        )
    }

    pendingSlotCount?.let { requested ->
        StapikDialog(
            title = stringResource(R.string.slots_shrink_title),
            confirmLabel = stringResource(R.string.slots_shrink_confirm),
            onConfirm = {
                viewModel.setSlotCount(requested)
                pendingSlotCount = null
            },
            onDismiss = { pendingSlotCount = null },
            dismissLabel = Messages.text(context, "action.cancel")
        ) {
            Text(stringResource(R.string.slots_shrink_message, catalog.lostBy(requested)))
        }
    }
}

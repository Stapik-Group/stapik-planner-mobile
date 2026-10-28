package pl.stapik.planner.ui.plan

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.stapik.common.ui.components.StapikButton
import pl.stapik.planner.R

@Composable
fun EditBar(
    state: EditUiState,
    onToggleEditing: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        StapikButton(
            text = stringResource(if (state.isEditing) R.string.edit_done else R.string.edit_start),
            onClick = onToggleEditing,
            emphasized = state.isEditing
        )
    }
}

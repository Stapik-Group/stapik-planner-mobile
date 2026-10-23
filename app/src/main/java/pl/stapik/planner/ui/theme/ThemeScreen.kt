package pl.stapik.planner.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pl.stapik.common.i18n.Messages
import pl.stapik.common.theme.ThemeId
import pl.stapik.planner.R
import pl.stapik.planner.data.theme.ThemeStorage
import pl.stapik.planner.ui.common.RetroScreenHeader

@Composable
fun ThemeScreen(storage: ThemeStorage, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val currentTheme by storage.theme.collectAsState(initial = ThemeId.CLASSIC)
    val themeColors = LocalThemeColors.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize().background(themeColors.windowBackground)) {
        RetroScreenHeader(title = stringResource(R.string.menu_theme), onBack = onBack)
        Column(modifier = Modifier.padding(16.dp)) {
            ThemeId.entries.forEach { theme ->
                // The labels are the library's (theme.classic, theme.classic_pink, theme.dark, ...).
                val label = Messages.text(context, "theme." + theme.name.lowercase())
                val isSelected = theme == currentTheme
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .themedSurface(
                            themeColors = themeColors,
                            backgroundColor = if (isSelected) themeColors.accent else themeColors.cellBackground,
                            raised = !isSelected
                        )
                        .clickable { scope.launch { storage.setTheme(theme) } }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) themeColors.textOnAccent else themeColors.textDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
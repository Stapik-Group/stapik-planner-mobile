package pl.stapik.planner.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.stapik.common.theme.Rgb
import pl.stapik.common.theme.ThemeId
import pl.stapik.common.theme.ThemePalette
import pl.stapik.common.theme.ThemePalettes

/** The colours the planner's own screens use, taken from the shared theme palette. */
data class ThemeColors(
    val windowBackground: Color,
    val cellBackground: Color,
    val cellBackgroundSecondary: Color,
    val accent: Color,
    val textOnAccent: Color,
    val textDark: Color,
    val textMuted: Color,
    val borderLight: Color,
    val borderDark: Color,
    val usesBevel: Boolean,
    val cornerRadius: Dp
)

private fun Rgb.color(): Color = Color(0xFF000000.toInt() or this)

fun ThemePalette.toThemeColors(): ThemeColors = ThemeColors(
    windowBackground = background.color(),
    cellBackground = button.color(),
    cellBackgroundSecondary = surface.color(),
    accent = accent.color(),
    textOnAccent = onAccent.color(),
    textDark = text.color(),
    textMuted = textMuted.color(),
    borderLight = borderLight.color(),
    borderDark = borderDark.color(),
    usesBevel = usesBevel,
    cornerRadius = cornerRadiusDp.dp
)

private val byTheme: Map<ThemeId, ThemeColors> =
    ThemeId.entries.associateWith { ThemePalettes.of(it).toThemeColors() }

fun themeColorsFor(theme: ThemeId): ThemeColors = byTheme.getValue(theme)
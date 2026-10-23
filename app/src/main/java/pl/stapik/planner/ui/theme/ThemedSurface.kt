package pl.stapik.planner.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pl.stapik.common.ui.components.bevel

// Classic/Classic Pink keep the raised Win98 bevel look; the other themes get a flat
// rounded card with a thin border and a soft shadow, matching the desktop CSS and the
// other Stapik apps.
fun Modifier.themedSurface(
    themeColors: ThemeColors,
    backgroundColor: Color,
    raised: Boolean = true
): Modifier = if (themeColors.usesBevel) {
    this.background(backgroundColor).bevel(raised, themeColors.borderLight, themeColors.borderDark)
} else {
    val shape = RoundedCornerShape(themeColors.cornerRadius)
    this
        .shadow(elevation = if (raised) 2.dp else 0.dp, shape = shape)
        .clip(shape)
        .background(backgroundColor)
        .border(1.dp, themeColors.borderLight, shape)
}

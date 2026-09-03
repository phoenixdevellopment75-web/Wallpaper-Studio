package com.example.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Personalization preferences managing visual aesthetic settings.
 */
object PersonalizationPreferences

/**
 * Pass-through modifier ensuring zero ghosting or broken backdrop blur effects.
 */
fun Modifier.studioBackdropBlur(
    disableBlur: Boolean = true,
    radius: Dp = 0.dp
): Modifier = this

/**
 * Resolves surface container color for Material 3 surfaces.
 * Returns crisp, elegant, fully opaque solid M3 surfaceContainer color.
 */
@Composable
fun getBackdropSurfaceColor(
    disableBlur: Boolean = true,
    defaultAlphaWhenBlurred: Float = 1.0f
): Color {
    return MaterialTheme.colorScheme.surfaceContainer
}

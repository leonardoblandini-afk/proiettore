package it.leonardo.mediaprojector.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/** Tema scuro: più comodo su un palco con luci basse. */
@Composable
fun MediaProjectorTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(), content = content)
}

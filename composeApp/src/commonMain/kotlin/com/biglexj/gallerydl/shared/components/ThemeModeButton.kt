package com.biglexj.gallerydl.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.biglexj.gallerydl.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeModeButton(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val nextMode = when (currentMode) {
        ThemeMode.System -> ThemeMode.Light
        ThemeMode.Light -> ThemeMode.Dark
        ThemeMode.Dark -> ThemeMode.System
    }
    val currentLabel = when (currentMode) {
        ThemeMode.System -> "Sistema"
        ThemeMode.Light -> "Claro"
        ThemeMode.Dark -> "Oscuro"
    }
    val nextLabel = when (nextMode) {
        ThemeMode.System -> "Sistema"
        ThemeMode.Light -> "Claro"
        ThemeMode.Dark -> "Oscuro"
    }
    val actionLabel = "Cambiar al tema $nextLabel"
    val iconVector = remember(currentMode) { getThemeModeVector(currentMode) }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(actionLabel) } },
        state = rememberTooltipState(),
    ) {
        Box(
            modifier = modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .clickable(
                    role = Role.Button,
                    onClickLabel = actionLabel,
                    onClick = { onModeSelected(nextMode) },
                )
                .semantics { contentDescription = "Tema actual: $currentLabel. $actionLabel" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Vectores canónicos oficiales de Material Design 3 (24x24):
 * - System (Monitor / Desktop Windows)
 * - Light (Sol / WbSunny)
 * - Dark (Luna Creciente clásica de Material 3 DarkMode)
 */
fun getThemeModeVector(mode: ThemeMode): ImageVector {
    val pathData = when (mode) {
        ThemeMode.System -> {
            // Monitor / Máquina (Desktop Windows)
            "M20 3H4c-1.1 0-2 .9-2 2v11c0 1.1.9 2 2 2h6l-2 3v1h8v-1l-2-3h6c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 13H4V5h16v11z"
        }
        ThemeMode.Light -> {
            // Sol (WbSunny / LightMode)
            "M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zM2 13h2c.55 0 1-.45 1-1s-.45-1-1-1H2c-.55 0-1 .45-1 1s.45 1 1 1zm18 0h2c.55 0 1-.45 1-1s-.45-1-1-1h-2c-.55 0-1 .45-1 1s.45 1 1 1zM11 2v2c0 .55.45 1 1 1s1-.45 1-1V2c0-.55-.45-1-1-1s-1 .45-1 1zm0 18v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1zM5.99 4.58a.996.996 0 0 0-1.41 0 .996.996 0 0 0 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41L5.99 4.58zm12.37 12.37a.996.996 0 0 0-1.41 0 .996.996 0 0 0 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41l-1.06-1.06zm1.06-10.96a.996.996 0 0 0 0-1.41.996.996 0 0 0-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06zM7.05 18.36a.996.996 0 0 0 0-1.41.996.996 0 0 0-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06z"
        }
        ThemeMode.Dark -> {
            // Luna Creciente Canónica (Material 3 DarkMode)
            "M12.3 2a10 10 0 0 0-1.9 20 10 10 0 0 0 8.6-4.9 1 1 0 0 0-1-1.5 8 8 0 1 1-7.2-13.6 1 1 0 0 0 1.5-1z"
        }
    }

    return ImageVector.Builder(
        name = "ThemeMode_${mode.name}",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

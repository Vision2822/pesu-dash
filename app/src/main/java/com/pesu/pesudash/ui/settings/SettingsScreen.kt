package com.pesu.pesudash.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pesu.pesudash.ui.components.ShadcnButton
import com.pesu.pesudash.ui.components.ButtonVariant
import com.pesu.pesudash.ui.components.ShadcnCard
import com.pesu.pesudash.ui.components.ShadcnSeparator
import com.pesu.pesudash.ui.theme.AccentPresets
import com.pesu.pesudash.ui.theme.AppTheme
import com.pesu.pesudash.ui.theme.Inter
import com.pesu.pesudash.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    currentTheme:   ThemeMode,
    currentAccent:  Color,
    onThemeChange:  (ThemeMode) -> Unit,
    onAccentChange: (Color) -> Unit,
    onLogout:       () -> Unit,
    modifier:       Modifier = Modifier
) {
    val c = AppTheme.colors
    var showColorPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text       = "Settings",
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            fontSize   = 22.sp,
            color      = c.foreground
        )

        ShadcnCard {
            SectionLabel("Appearance")
            Spacer(Modifier.height(12.dp))

            Text(
                text       = "Theme",
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize   = 14.sp,
                color      = c.foreground
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    val isSelected = mode == currentTheme
                    val label = when (mode) {
                        ThemeMode.DARK   -> "Dark"
                        ThemeMode.LIGHT  -> "Light"
                        ThemeMode.SYSTEM -> "System"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) c.accent.copy(alpha = 0.15f)
                                else c.cardHover
                            )
                            .border(
                                1.dp,
                                if (isSelected) c.accent else c.border,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onThemeChange(mode) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = label,
                            fontFamily = Inter,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize   = 13.sp,
                            color      = if (isSelected) c.accent else c.mutedFg
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            ShadcnSeparator()
            Spacer(Modifier.height(16.dp))

            Text(
                text       = "Accent Color",
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize   = 14.sp,
                color      = c.foreground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = "Used for highlights and interactive elements",
                fontFamily = Inter,
                fontSize   = 12.sp,
                color      = c.mutedFg
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val presets = listOf(
                    AccentPresets.Blue,
                    AccentPresets.Purple,
                    AccentPresets.Green,
                    AccentPresets.Rose,
                    AccentPresets.Orange,
                    AccentPresets.Cyan
                )
                presets.forEach { preset ->
                    val isSelected = currentAccent == preset
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(preset)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) c.foreground else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onAccentChange(preset) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint     = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            ShadcnButton(
                text     = "Custom color…",
                onClick  = { showColorPicker = true },
                variant  = ButtonVariant.Outline,
                modifier = Modifier.fillMaxWidth()
            )
        }

        ShadcnCard {
            SectionLabel("Account")
            Spacer(Modifier.height(12.dp))
            ShadcnButton(
                text     = "Logout",
                onClick  = onLogout,
                variant  = ButtonVariant.Destructive,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = currentAccent,
            onDismiss    = { showColorPicker = false },
            onColorPicked = { color ->
                onAccentChange(color)
                showColorPicker = false
            }
        )
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor:  Color,
    onDismiss:     () -> Unit,
    onColorPicked: (Color) -> Unit
) {
    val c = AppTheme.colors

    val initArgb = initialColor.toArgb()
    val initHsv  = FloatArray(3)
    android.graphics.Color.colorToHSV(initArgb, initHsv)

    var hue        by remember { mutableFloatStateOf(initHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initHsv[1]) }
    var value      by remember { mutableFloatStateOf(initHsv[2]) }
    var alpha      by remember { mutableFloatStateOf(initialColor.alpha) }

    val pickedColor = remember(hue, saturation, value, alpha) {
        val argb = android.graphics.Color.HSVToColor(
            (alpha * 255).toInt(),
            floatArrayOf(hue, saturation, value)
        )
        Color(argb)
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.card)
                .border(1.dp, c.border, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text       = "Pick a color",
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 16.sp,
                color      = c.foreground
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(pickedColor)
                    .border(1.dp, c.border, RoundedCornerShape(10.dp))
            )

            SliderRow(
                label    = "Hue",
                value    = hue / 360f,
                onChange = { hue = it * 360f },
                color    = Color.hsv(hue, 1f, 1f)
            )

            SliderRow(
                label    = "Saturation",
                value    = saturation,
                onChange = { saturation = it },
                color    = Color.hsv(hue, saturation, value)
            )

            SliderRow(
                label    = "Brightness",
                value    = value,
                onChange = { value = it },
                color    = Color.hsv(hue, saturation, value)
            )

            SliderRow(
                label    = "Opacity",
                value    = alpha,
                onChange = { alpha = it },
                color    = pickedColor
            )

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShadcnButton(
                    text     = "Cancel",
                    onClick  = onDismiss,
                    variant  = ButtonVariant.Outline,
                    modifier = Modifier.weight(1f)
                )
                ShadcnButton(
                    text     = "Apply",
                    onClick  = { onColorPicked(pickedColor) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SliderRow(
    label:    String,
    value:    Float,
    onChange: (Float) -> Unit,
    color:    Color
) {
    val c = AppTheme.colors
    Column {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = label,
                fontFamily = Inter,
                fontSize   = 12.sp,
                color      = c.mutedFg
            )
            Text(
                text       = "${(value * 100).toInt()}%",
                fontFamily = Inter,
                fontSize   = 12.sp,
                color      = c.dimFg
            )
        }
        Slider(
            value         = value,
            onValueChange = onChange,
            modifier      = Modifier.fillMaxWidth(),
            colors        = SliderDefaults.colors(
                thumbColor       = color,
                activeTrackColor = color,
                inactiveTrackColor = c.border
            )
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        fontFamily    = Inter,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 12.sp,
        color         = AppTheme.colors.mutedFg,
        letterSpacing = 0.8.sp
    )
}
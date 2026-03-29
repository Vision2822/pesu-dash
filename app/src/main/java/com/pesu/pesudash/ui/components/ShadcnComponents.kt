package com.pesu.pesudash.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pesu.pesudash.ui.theme.AppTheme
import com.pesu.pesudash.ui.theme.Inter

@Composable
fun ShadcnCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(12.dp)

    val baseModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(c.card)
        .border(1.dp, c.border, shape)

    val finalModifier = if (onClick != null) {
        baseModifier.clickable { onClick() }
    } else baseModifier

    Column(
        modifier = finalModifier.padding(16.dp),
        content = content
    )
}

@Composable
fun ShadcnButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    variant: ButtonVariant = ButtonVariant.Default
) {
    val c = AppTheme.colors

    val (bgColor, textColor, borderColor) = when (variant) {
        ButtonVariant.Default     -> Triple(c.foreground, c.background, Color.Transparent)
        ButtonVariant.Secondary   -> Triple(c.cardHover, c.foreground, c.border)
        ButtonVariant.Destructive -> Triple(c.red, Color.White, Color.Transparent)
        ButtonVariant.Outline     -> Triple(Color.Transparent, c.foreground, c.border)
        ButtonVariant.Ghost       -> Triple(Color.Transparent, c.foreground, Color.Transparent)
        ButtonVariant.Green       -> Triple(c.green, Color.White, Color.Transparent)
    }

    Button(
        onClick  = onClick,
        modifier = modifier.height(40.dp),
        enabled  = enabled,
        shape    = RoundedCornerShape(8.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = bgColor,
            contentColor           = textColor,
            disabledContainerColor = bgColor.copy(alpha = 0.5f),
            disabledContentColor   = textColor.copy(alpha = 0.5f)
        ),
        border = if (borderColor != Color.Transparent)
            BorderStroke(1.dp, borderColor) else null,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color       = textColor,
                modifier    = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text       = text,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize   = 14.sp
            )
        }
    }
}

enum class ButtonVariant {
    Default, Secondary, Destructive, Outline, Ghost, Green
}

@Composable
fun ShadcnInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true
) {
    val c = AppTheme.colors

    OutlinedTextField(
        value               = value,
        onValueChange       = onValueChange,
        modifier            = modifier.fillMaxWidth(),
        singleLine          = singleLine,
        placeholder         = {
            Text(placeholder, color = c.dimFg, fontSize = 14.sp, fontFamily = Inter)
        },
        visualTransformation = visualTransformation,
        keyboardOptions      = keyboardOptions,
        trailingIcon         = trailingIcon,
        shape                = RoundedCornerShape(8.dp),
        textStyle            = LocalTextStyle.current.copy(
            fontFamily = Inter,
            fontSize   = 14.sp,
            color      = c.foreground
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = c.ring,
            unfocusedBorderColor    = c.border,
            focusedContainerColor   = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            cursorColor             = c.foreground,
            focusedTextColor        = c.foreground,
            unfocusedTextColor      = c.foreground
        )
    )
}

@Composable
fun ShadcnBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(6.dp),
        color    = color.copy(alpha = 0.12f),
        border   = BorderStroke(0.5.dp, color.copy(alpha = 0.25f))
    ) {
        Text(
            text       = text,
            color      = color,
            fontSize   = 11.sp,
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun ShadcnSeparator(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier  = modifier.fillMaxWidth(),
        thickness = 0.5.dp,
        color     = AppTheme.colors.border
    )
}
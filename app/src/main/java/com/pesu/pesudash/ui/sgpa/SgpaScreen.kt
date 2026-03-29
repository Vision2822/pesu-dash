package com.pesu.pesudash.ui.sgpa

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pesu.pesudash.ui.theme.AppTheme
import com.pesu.pesudash.ui.theme.Inter

@Composable
fun SgpaScreen(modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Box(
        modifier         = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text       = "SGPA",
                fontSize   = 20.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                color      = c.foreground
            )
            Text(
                text       = "Coming soon",
                fontSize   = 13.sp,
                fontFamily = Inter,
                color      = c.dimFg
            )
        }
    }
}
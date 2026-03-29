package com.pesu.pesudash.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pesu.pesudash.ui.theme.AppTheme

enum class NavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(
        label          = "Home",
        selectedIcon   = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    TIMETABLE(
        label          = "Timetable",
        selectedIcon   = Icons.Filled.DateRange,
        unselectedIcon = Icons.Outlined.DateRange
    ),
    ATTENDANCE(
        label          = "Attendance",
        selectedIcon   = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    ),
    SGPA(
        label          = "SGPA",
        selectedIcon   = Icons.Filled.Star,
        unselectedIcon = Icons.Outlined.Star
    ),
    ABOUT(
        label          = "About",
        selectedIcon   = Icons.Filled.Info,
        unselectedIcon = Icons.Outlined.Info
    ),
    SETTINGS(
        label          = "Settings",
        selectedIcon   = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

@Composable
fun BottomNavBar(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(c.background)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(c.border)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            NavTab.entries.forEach { tab ->
                NavBarItem(
                    tab        = tab,
                    isSelected = tab == currentTab,
                    onClick    = { onTabSelected(tab) },
                    modifier   = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    tab: NavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) c.cardHover
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(c.foreground)
            )
            Spacer(Modifier.height(4.dp))
        }

        Icon(
            imageVector        = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = tab.label,
            tint               = if (isSelected) c.foreground else c.dimFg,
            modifier           = Modifier.size(22.dp)
        )
    }
}
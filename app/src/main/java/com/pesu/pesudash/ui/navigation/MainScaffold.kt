package com.pesu.pesudash.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pesu.pesudash.data.local.SessionStore
import com.pesu.pesudash.data.model.UserProfile
import com.pesu.pesudash.data.repository.PesuRepository
import com.pesu.pesudash.ui.about.AboutScreen
import com.pesu.pesudash.ui.attendance.AttendanceScreen
import com.pesu.pesudash.ui.attendance.AttendanceViewModel
import com.pesu.pesudash.ui.home.HomeScreen
import com.pesu.pesudash.ui.home.HomeViewModel
import com.pesu.pesudash.ui.results.ResultsScreen
import com.pesu.pesudash.ui.results.ResultsViewModel
import com.pesu.pesudash.ui.settings.SettingsScreen
import com.pesu.pesudash.ui.theme.AccentPresets
import com.pesu.pesudash.ui.theme.AppTheme
import com.pesu.pesudash.ui.theme.toThemeMode
import com.pesu.pesudash.ui.today.TodayScreen
import com.pesu.pesudash.ui.today.TodayViewModel
import kotlinx.coroutines.launch

@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun MainScaffold(
    profile:          UserProfile,
    sessionStore:     SessionStore,
    repository:       PesuRepository,
    onLogout:         () -> Unit,
    onSessionExpired: () -> Unit
) {
    val c        = AppTheme.colors
    val scope    = rememberCoroutineScope()
    var currentTab  by remember { mutableStateOf(NavTab.HOME) }
    var previousTab by remember { mutableStateOf(NavTab.HOME) }
    val tabOrder = NavTab.entries

    val themeModeStr by sessionStore.themeMode.collectAsStateWithLifecycle(initialValue = "SYSTEM")
    val accentHex    by sessionStore.accentColor.collectAsStateWithLifecycle(initialValue = null)
    val themeMode    = themeModeStr.toThemeMode()
    val accentColor  = accentHex?.let {
        try { Color(java.lang.Long.parseLong(it, 16)) }
        catch (e: Exception) { AccentPresets.Blue }
    } ?: AccentPresets.Blue

    val todayVm = viewModel<TodayViewModel>(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TodayViewModel(repository) as T
            }
        }
    )

    val homeVm = viewModel<HomeViewModel>(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(repository) as T
            }
        }
    )

    val attendanceVm = viewModel<AttendanceViewModel>(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AttendanceViewModel(repository, sessionStore) as T
            }
        }
    )

    val resultsVm = viewModel<ResultsViewModel>(
        factory = ResultsViewModel.Factory(repository)
    )

    Scaffold(
        containerColor = c.background,
        bottomBar = {
            BottomNavBar(
                currentTab    = currentTab,
                onTabSelected = {
                    previousTab = currentTab
                    currentTab  = it
                }
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState   = currentTab,
            transitionSpec = {
                val fromIndex  = tabOrder.indexOf(initialState)
                val toIndex    = tabOrder.indexOf(targetState)
                val goingRight = toIndex > fromIndex

                (slideInHorizontally(
                    initialOffsetX = { if (goingRight) it / 6 else -it / 6 },
                    animationSpec  = tween(280)
                ) + fadeIn(tween(280))) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = { if (goingRight) -it / 6 else it / 6 },
                    animationSpec = tween(280)
                ) + fadeOut(tween(180)))
            },
            label = "tab_transition"
        ) { tab ->
            when (tab) {
                NavTab.HOME -> HomeScreen(
                    viewModel              = homeVm,
                    profile                = profile,
                    onNavigateToTimetable  = {
                        previousTab = currentTab
                        currentTab  = NavTab.TIMETABLE
                    },
                    onNavigateToAttendance = {
                        previousTab = currentTab
                        currentTab  = NavTab.ATTENDANCE
                    },
                    onNavigateToSettings   = {
                        previousTab = currentTab
                        currentTab  = NavTab.SETTINGS
                    },
                    modifier = Modifier.padding(padding)
                )

                NavTab.TIMETABLE -> TodayScreen(
                    viewModel = todayVm,
                    userId    = profile.userId,
                    modifier  = Modifier.padding(padding)
                )

                NavTab.ATTENDANCE -> AttendanceScreen(
                    viewModel = attendanceVm,
                    userId    = profile.userId,
                    modifier  = Modifier.padding(padding)
                )

                NavTab.SGPA -> ResultsScreen(
                    viewModel = resultsVm,
                    userId    = profile.userId,
                    usn       = profile.srn,
                    modifier  = Modifier.padding(padding)
                )

                NavTab.ABOUT -> AboutScreen(
                    sessionStore       = sessionStore,
                    currentVersionName = "1.1",
                    currentVersionCode = 2,
                    modifier           = Modifier.padding(padding)
                )

                NavTab.SETTINGS -> SettingsScreen(
                    currentTheme   = themeMode,
                    currentAccent  = accentColor,
                    onThemeChange  = { mode ->
                        scope.launch { sessionStore.saveThemeMode(mode.name) }
                    },
                    onAccentChange = { color ->
                        val hex = Integer.toHexString(color.toArgb())
                        scope.launch { sessionStore.saveAccentColor(hex) }
                    },
                    onLogout  = onLogout,
                    modifier  = Modifier.padding(padding)
                )
            }
        }
    }
}
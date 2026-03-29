package com.pesu.pesudash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pesu.pesudash.data.local.SessionStore
import com.pesu.pesudash.data.network.PesuApiClient
import com.pesu.pesudash.data.repository.PesuRepository
import com.pesu.pesudash.ui.login.LoginScreen
import com.pesu.pesudash.ui.login.LoginViewModel
import com.pesu.pesudash.ui.navigation.MainScaffold
import com.pesu.pesudash.ui.theme.AccentPresets
import com.pesu.pesudash.ui.theme.AppTheme
import com.pesu.pesudash.ui.theme.PesuDashTheme
import com.pesu.pesudash.ui.theme.toThemeMode
import com.pesu.pesudash.widget.PesuDashWidget
import com.pesu.pesudash.widget.WidgetRefreshReceiver
import com.pesu.pesudash.widget.WidgetStateStore
import com.pesu.pesudash.widget.WidgetUpdater
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var sessionStore: SessionStore
    private lateinit var repository: PesuRepository
    private lateinit var appViewModel: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sessionStore = SessionStore(applicationContext)
        repository   = PesuRepository(sessionStore = sessionStore)
        appViewModel = ViewModelProvider(
            this,
            AppViewModel.Factory(sessionStore, repository)
        )[AppViewModel::class.java]

        setContent {
            val themeModeStr by sessionStore.themeMode.collectAsStateWithLifecycle(
                initialValue = "SYSTEM"
            )
            val accentHex by sessionStore.accentColor.collectAsStateWithLifecycle(
                initialValue = null
            )

            val themeMode   = themeModeStr.toThemeMode()
            val accentColor = accentHex?.let {
                try { Color(java.lang.Long.parseLong(it, 16)) }
                catch (e: Exception) { AccentPresets.Blue }
            } ?: AccentPresets.Blue

            PesuDashTheme(
                themeMode   = themeMode,
                accentColor = accentColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = AppTheme.colors.background
                ) {
                    val appState by appViewModel.appState.collectAsStateWithLifecycle()

                    when (val state = appState) {

                        is AppStartState.Loading -> {
                            Box(
                                modifier         = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = AppTheme.colors.accent
                                )
                            }
                        }

                        is AppStartState.Unauthenticated -> {
                            val loginVm = viewModel<LoginViewModel>(
                                factory = LoginViewModel.Factory(sessionStore, repository)
                            )
                            LoginScreen(
                                viewModel      = loginVm,
                                onLoginSuccess = { newProfile ->
                                    appViewModel.onLoginSuccess(newProfile)
                                    triggerWidgetUpdate()
                                }
                            )
                        }

                        is AppStartState.Authenticated -> {
                            MainScaffold(
                                profile      = state.profile,
                                sessionStore = sessionStore,
                                repository   = repository,
                                onLogout     = { appViewModel.logout() },
                                onSessionExpired = { appViewModel.onSessionExpired() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun triggerWidgetUpdate() {
        WidgetRefreshReceiver.scheduleRepeating(applicationContext)
        lifecycleScope.launch {
            try {
                WidgetUpdater.fetchAndStore(applicationContext)
                val manager = GlanceAppWidgetManager(applicationContext)
                manager.getGlanceIds(PesuDashWidget::class.java).forEach { id ->
                    PesuDashWidget().update(applicationContext, id)
                }
            } catch (_: Exception) { }
        }
    }
}
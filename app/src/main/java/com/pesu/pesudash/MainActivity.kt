package com.pesu.pesudash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pesu.pesudash.data.local.SessionStore
import com.pesu.pesudash.data.model.UserProfile
import com.pesu.pesudash.data.network.PesuApiClient
import com.pesu.pesudash.data.repository.PesuRepository
import com.pesu.pesudash.ui.login.LoginScreen
import com.pesu.pesudash.ui.login.LoginViewModel
import com.pesu.pesudash.ui.navigation.MainScaffold
import com.pesu.pesudash.ui.theme.AccentPresets
import com.pesu.pesudash.ui.theme.AppTheme
import com.pesu.pesudash.ui.theme.PesuDashTheme
import com.pesu.pesudash.ui.theme.ThemeMode
import com.pesu.pesudash.ui.theme.toThemeMode
import com.pesu.pesudash.widget.PesuDashWidget
import com.pesu.pesudash.widget.WidgetRefreshReceiver
import com.pesu.pesudash.widget.WidgetStateStore
import com.pesu.pesudash.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionStore = SessionStore(applicationContext)
        val repository   = PesuRepository(sessionStore = sessionStore)

        val savedToken   = runBlocking { sessionStore.authToken.first() }
        val savedProfile = runBlocking { sessionStore.getProfile() }

        if (savedToken != null) repository.restoreToken(savedToken)

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
                    var profile by remember {
                        mutableStateOf(
                            if (savedToken != null) savedProfile else null
                        )
                    }

                    if (profile == null) {
                        val loginVm = viewModel<LoginViewModel>(
                            factory = object : ViewModelProvider.Factory {
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    @Suppress("UNCHECKED_CAST")
                                    return LoginViewModel(sessionStore, repository) as T
                                }
                            }
                        )
                        LoginScreen(
                            viewModel      = loginVm,
                            onLoginSuccess = { newProfile ->
                                profile = newProfile
                                WidgetRefreshReceiver.scheduleRepeating(applicationContext)
                                CoroutineScope(Dispatchers.IO).launch {
                                    WidgetUpdater.fetchAndStore(applicationContext)
                                    val manager = GlanceAppWidgetManager(applicationContext)
                                    val ids = manager.getGlanceIds(PesuDashWidget::class.java)
                                    ids.forEach { glanceId ->
                                        PesuDashWidget().update(applicationContext, glanceId)
                                    }
                                }
                            }
                        )
                    } else {
                        MainScaffold(
                            profile      = profile!!,
                            sessionStore = sessionStore,
                            repository   = repository,
                            onLogout     = {
                                repository.logout()
                                runBlocking { sessionStore.clear() }
                                PesuApiClient.clearSession()
                                WidgetRefreshReceiver.cancelRepeating(applicationContext)
                                WidgetStateStore.clear(applicationContext)
                                profile = null
                            }
                        )
                    }
                }
            }
        }
    }
}
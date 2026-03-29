// AppViewModel.kt
package com.pesu.pesudash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pesu.pesudash.data.local.SessionStore
import com.pesu.pesudash.data.model.UserProfile
import com.pesu.pesudash.data.network.PesuApiClient
import com.pesu.pesudash.data.repository.PesuRepository
import com.pesu.pesudash.widget.WidgetRefreshReceiver
import com.pesu.pesudash.widget.WidgetStateStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── App-level auth state ─────────────────────────────────────────────────────
sealed class AppStartState {
    object Loading : AppStartState()
    object Unauthenticated : AppStartState()
    data class Authenticated(val profile: UserProfile) : AppStartState()
}

class AppViewModel(
    private val sessionStore: SessionStore,
    private val repository: PesuRepository
) : ViewModel() {

    private val _appState = MutableStateFlow<AppStartState>(AppStartState.Loading)
    val appState: StateFlow<AppStartState> = _appState.asStateFlow()

    init {
        checkSession()
    }

    // ── Called once on start — no runBlocking ────────────────────────────────
    private fun checkSession() {
        viewModelScope.launch {
            try {
                val token   = sessionStore.authToken.first()
                val profile = sessionStore.getProfile()

                if (token != null && profile != null) {
                    // Restore token into API client
                    repository.restoreToken(token)
                    _appState.value = AppStartState.Authenticated(profile)
                } else {
                    _appState.value = AppStartState.Unauthenticated
                }
            } catch (e: Exception) {
                // If DataStore read fails, safest option is re-login
                _appState.value = AppStartState.Unauthenticated
            }
        }
    }

    // ── Called by LoginScreen on success ─────────────────────────────────────
    fun onLoginSuccess(profile: UserProfile) {
        _appState.value = AppStartState.Authenticated(profile)
    }

    // ── Called by any screen that gets a 401/session expired ─────────────────
    fun onSessionExpired() {
        viewModelScope.launch {
            clearSession()
            _appState.value = AppStartState.Unauthenticated
        }
    }

    // ── Called by logout button ──────────────────────────────────────────────
    fun logout() {
        viewModelScope.launch {
            clearSession()
            _appState.value = AppStartState.Unauthenticated
        }
    }

    // ── Single place that wipes everything ──────────────────────────────────
    private suspend fun clearSession() {
        repository.logout()
        sessionStore.clear()
        PesuApiClient.clearSession()
    }

    // ── Factory — no Hilt yet, clean manual DI ───────────────────────────────
    class Factory(
        private val sessionStore: SessionStore,
        private val repository: PesuRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AppViewModel(sessionStore, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
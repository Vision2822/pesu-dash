package com.pesu.pesudash.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pesu.pesudash.data.local.SessionStore
import com.pesu.pesudash.data.model.UserProfile
import com.pesu.pesudash.data.network.AuthException
import com.pesu.pesudash.data.network.PesuApiClient
import com.pesu.pesudash.data.repository.PesuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val profile: UserProfile) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(
    private val sessionStore: SessionStore,
    private val repository: PesuRepository = PesuRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state: StateFlow<LoginUiState> = _state

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _state.value = LoginUiState.Error("Enter your SRN and password")
            return
        }

        viewModelScope.launch {
            _state.value = LoginUiState.Loading
            try {
                val user  = repository.login(username, password)
                val token = PesuApiClient.authToken ?: ""

                val srn = user.departmentId
                    ?: user.srn
                    ?: username.uppercase()

                val branch = (user.branch ?: "")
                    .removePrefix("Branch:")
                    .trim()

                val profile = UserProfile(
                    userId    = user.userId,
                    name      = user.name ?: user.userName ?: "Student",
                    srn       = srn,
                    className = user.className ?: "",
                    branch    = branch,
                    program   = user.program ?: "",
                    photo     = user.photo
                )

                sessionStore.saveProfile(profile, token)
                _state.value = LoginUiState.Success(profile)

            } catch (e: AuthException) {
                _state.value = LoginUiState.Error(e.message ?: "Login failed")
            } catch (e: Exception) {
                _state.value = LoginUiState.Error("Network error. Check your connection.")
            }
        }
    }
}
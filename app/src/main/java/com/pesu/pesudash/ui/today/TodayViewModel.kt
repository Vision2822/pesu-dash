package com.pesu.pesudash.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pesu.pesudash.data.model.TodayClass
import com.pesu.pesudash.data.repository.PesuRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class TodayUiState {
    object Idle    : TodayUiState()
    object Weekend : TodayUiState()
    data class Holiday(val name: String, val eventType: String) : TodayUiState()
    data class Success(val classes: List<TodayClass>) : TodayUiState()
    data class Error(val message: String) : TodayUiState()
}

class TodayViewModel(
    private val repository: PesuRepository = PesuRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<TodayUiState>(TodayUiState.Idle)
    val state: StateFlow<TodayUiState> = _state

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private var fetchJob: Job? = null

    fun loadForDate(userId: String, date: Calendar, debounce: Boolean = true) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            if (debounce) delay(500L)
            _isRefreshing.value = true
            try {
                val schedule = repository.getTodayClasses(userId, date)
                _state.value = when {
                    schedule.holiday != null   -> TodayUiState.Holiday(
                        name      = schedule.holiday.name,
                        eventType = schedule.holiday.eventType
                    )
                    schedule.classes.isEmpty() -> TodayUiState.Weekend
                    else                       -> TodayUiState.Success(schedule.classes)
                }
            } catch (e: Exception) {
                if (_state.value is TodayUiState.Idle) {
                    _state.value = TodayUiState.Error(e.message ?: "Something went wrong")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refresh(userId: String, date: Calendar) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val schedule = repository.hardRefresh(userId, date)
                _state.value = when {
                    schedule.holiday != null   -> TodayUiState.Holiday(
                        name      = schedule.holiday.name,
                        eventType = schedule.holiday.eventType
                    )
                    schedule.classes.isEmpty() -> TodayUiState.Weekend
                    else                       -> TodayUiState.Success(schedule.classes)
                }
            } catch (e: Exception) {
                _state.value = TodayUiState.Error(e.message ?: "Something went wrong")
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
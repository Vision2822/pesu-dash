package com.pesu.pesudash.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pesu.pesudash.data.model.AttendanceSubject
import com.pesu.pesudash.data.model.SeatingInfo
import com.pesu.pesudash.data.model.TodayClass
import com.pesu.pesudash.data.repository.PesuRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val isLoading: Boolean = true,
    val todayClasses: List<TodayClass> = emptyList(),
    val holidayName: String? = null,
    val isWeekend: Boolean = false,
    val overallAttendance: Float = 0f,
    val totalSubjects: Int = 0,
    val attendanceSubjects: List<AttendanceSubject> = emptyList(),
    val seatingInfo: List<SeatingInfo> = emptyList(),
    val error: String? = null
)

class HomeViewModel(
    private val repository: PesuRepository = PesuRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    private var loaded = false

    fun loadHome(userId: String) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            _state.value = HomeUiState(isLoading = true)
            try {
                val today = Calendar.getInstance()

                val scheduleDeferred   = async { repository.getTodayClasses(userId, today) }
                val seatingDeferred    = async { repository.getSeatingInfo(userId) }
                val attendanceDeferred = async {
                    try { repository.getAttendanceSummary(userId) }
                    catch (_: Exception) { emptyList() }
                }

                val schedule  = scheduleDeferred.await()
                val seating   = seatingDeferred.await()
                val subjects  = attendanceDeferred.await()

                val holidayName = schedule.holiday?.name
                val isWeekend   = schedule.holiday == null && schedule.classes.isEmpty()

                val totalSubjects     = subjects.size
                val totalPct          = subjects.mapNotNull { it.percentage }.sum()
                val overallAttendance = if (subjects.isNotEmpty()) totalPct / subjects.size else 0f

                val relevantSeating = seating
                    .filter { it.isToday() || it.isTomorrow() || it.isOngoing() }
                    .sortedBy { it.testStartTime }

                _state.value = HomeUiState(
                    isLoading          = false,
                    todayClasses       = schedule.classes,
                    holidayName        = holidayName,
                    isWeekend          = isWeekend,
                    overallAttendance  = overallAttendance,
                    totalSubjects      = totalSubjects,
                    attendanceSubjects = subjects,
                    seatingInfo        = relevantSeating
                )
            } catch (e: Exception) {
                _state.value = HomeUiState(
                    isLoading = false,
                    error     = e.message ?: "Something went wrong"
                )
            }
        }
    }

    fun reload(userId: String) {
        loaded = false
        loadHome(userId)
    }
}
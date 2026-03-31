package com.pesu.pesudash.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.pesu.pesudash.data.local.SessionStore
import com.pesu.pesudash.data.model.AttendanceSemester
import com.pesu.pesudash.data.model.AttendanceSubject
import com.pesu.pesudash.data.model.CalendarEvent
import com.pesu.pesudash.data.model.TimetableEntry
import com.pesu.pesudash.data.repository.PesuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class AttendanceUiState(
    val isLoading: Boolean = true,
    val subjects: List<AttendanceSubject> = emptyList(),
    val overallPercentage: Float = 0f,
    val error: String? = null
)

class AttendanceViewModel(
    private val repository: PesuRepository = PesuRepository(),
    private val sessionStore: SessionStore? = null
) : ViewModel() {

    private val _state = MutableStateFlow(AttendanceUiState())
    val state: StateFlow<AttendanceUiState> = _state

    private val _targetPct = MutableStateFlow(75f)
    val targetPct: StateFlow<Float> = _targetPct

    private val _semEndDate = MutableStateFlow(0L)
    val semEndDate: StateFlow<Long> = _semEndDate

    private val _futureClasses = MutableStateFlow<Map<String, Int>>(emptyMap())
    val futureClasses: StateFlow<Map<String, Int>> = _futureClasses

    private val _availableSemesters = MutableStateFlow<List<AttendanceSemester>>(emptyList())
    val availableSemesters: StateFlow<List<AttendanceSemester>> = _availableSemesters

    private val _selectedSemester = MutableStateFlow<AttendanceSemester?>(null)
    val selectedSemester: StateFlow<AttendanceSemester?> = _selectedSemester

    private val gson = Gson()
    private var loaded = false
    private var userId = ""

    init {
        viewModelScope.launch {
            _targetPct.value  = sessionStore?.getTargetPct()  ?: 75f
            _semEndDate.value = sessionStore?.getSemEndDate() ?: 0L
        }
    }

    fun load(uid: String) {
        if (loaded) return
        loaded = true
        userId = uid
        viewModelScope.launch {
            _state.value = AttendanceUiState(isLoading = true)
            try {
                val allSemesters = repository.getAttendanceSemestersFull(uid)
                    .sortedByDescending { it.batchClassOrder }

                _availableSemesters.value = allSemesters

                val savedSem = loadSavedSemester(allSemesters)
                val activeSem = savedSem ?: allSemesters.firstOrNull()
                _selectedSemester.value = activeSem

                if (activeSem != null) {
                    fetchAttendanceForSemester(uid, activeSem)
                } else {
                    _state.value = AttendanceUiState(
                        isLoading = false,
                        error     = "No semesters found"
                    )
                }

            } catch (e: Exception) {
                _state.value = AttendanceUiState(isLoading = false, error = e.message)
            }
        }
    }

    fun selectSemester(semester: AttendanceSemester) {
        if (_selectedSemester.value?.batchClassId == semester.batchClassId) return
        _selectedSemester.value = semester
        viewModelScope.launch {
            sessionStore?.saveSelectedSemester(gson.toJson(semester))
            sessionStore?.clearAllCaches()
            fetchAttendanceForSemester(userId, semester)
        }
    }

    fun reload(uid: String) {
        loaded = false
        load(uid)
    }

    fun setTargetPct(pct: Float) {
        _targetPct.value = pct
        viewModelScope.launch { sessionStore?.saveTargetPct(pct) }
    }

    fun setSemEndDate(epochMs: Long) {
        _semEndDate.value = epochMs
        viewModelScope.launch {
            sessionStore?.saveSemEndDate(epochMs)
            recomputeFuture(userId)
        }
    }

    private suspend fun fetchAttendanceForSemester(uid: String, semester: AttendanceSemester) {
        _state.value = AttendanceUiState(isLoading = true)
        try {
            val subjects = repository.getAttendanceSummaryForSemester(uid, semester.batchClassId)
            val overall  = if (subjects.isNotEmpty())
                subjects.mapNotNull { it.percentage }.sum() / subjects.size
            else 0f

            _state.value = AttendanceUiState(
                isLoading         = false,
                subjects          = subjects.sortedBy { it.percentage },
                overallPercentage = overall
            )

            recomputeFuture(uid)

        } catch (e: Exception) {
            _state.value = AttendanceUiState(isLoading = false, error = e.message)
        }
    }

    private suspend fun loadSavedSemester(
        allSemesters: List<AttendanceSemester>
    ): AttendanceSemester? {
        val json = sessionStore?.getSelectedSemester() ?: return null
        return try {
            val saved = gson.fromJson(json, AttendanceSemester::class.java)
            allSemesters.find { it.batchClassId == saved.batchClassId }
        } catch (_: Exception) { null }
    }

    private suspend fun recomputeFuture(uid: String) {
        val endMs = _semEndDate.value
        if (endMs <= 0L || uid.isEmpty()) {
            _futureClasses.value = emptyMap()
            return
        }
        try {
            val calendarEvents = repository.getCalendarEventsPublic(uid)
            val timetable      = repository.getFullTimetable(uid)
            _futureClasses.value = computeFutureClasses(
                timetable      = timetable,
                calendarEvents = calendarEvents,
                semEndMs       = endMs
            )
        } catch (_: Exception) {
            _futureClasses.value = emptyMap()
        }
    }
}

fun computeFutureClasses(
    timetable:      List<TimetableEntry>,
    calendarEvents: List<CalendarEvent>,
    semEndMs:       Long
): Map<String, Int> {

    val timetableByDay = mutableMapOf<Int, MutableList<String>>()
    for (entry in timetable) {
        timetableByDay.getOrPut(entry.day) { mutableListOf() }
            .add(entry.subjectCode)
    }

    val holidayDates = mutableSetOf<String>()
    for (event in calendarEvents) {
        if (event.isHoliday != 1) continue
        val start = Calendar.getInstance().apply { timeInMillis = event.startDate }
        val end   = Calendar.getInstance().apply { timeInMillis = event.endDate }
        val cur   = start.clone() as Calendar
        while (!cur.after(end)) {
            holidayDates.add(calendarKey(cur))
            cur.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    val futureCount = mutableMapOf<String, Int>()
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    val endCal = Calendar.getInstance().apply { timeInMillis = semEndMs }

    val cur = Calendar.getInstance().apply {
        timeInMillis = today.timeInMillis
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    while (!cur.after(endCal)) {
        val dow = cur.get(Calendar.DAY_OF_WEEK)

        if (dow == Calendar.SUNDAY) {
            cur.add(Calendar.DAY_OF_YEAR, 1)
            continue
        }

        if (calendarKey(cur) in holidayDates) {
            cur.add(Calendar.DAY_OF_YEAR, 1)
            continue
        }

        val pesuDay = when (dow) {
            Calendar.MONDAY    -> 1
            Calendar.TUESDAY   -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY  -> 4
            Calendar.FRIDAY    -> 5
            Calendar.SATURDAY  -> 6
            else               -> -1
        }

        val subjectsToday = timetableByDay[pesuDay] ?: emptyList()
        for (code in subjectsToday) {
            futureCount[code] = (futureCount[code] ?: 0) + 1
        }

        cur.add(Calendar.DAY_OF_YEAR, 1)
    }

    return futureCount
}

private fun calendarKey(cal: Calendar): String {
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH) + 1
    val d = cal.get(Calendar.DAY_OF_MONTH)
    return "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
}
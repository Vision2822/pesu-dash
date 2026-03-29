package com.pesu.pesudash.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pesu.pesudash.data.local.SessionStore
import com.pesu.pesudash.data.model.*
import com.pesu.pesudash.data.network.PesuApiClient
import com.pesu.pesudash.data.network.PesuApiService
import com.pesu.pesudash.data.network.PesuError
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.*

class PesuRepository(
    private val api: PesuApiService = PesuApiService(),
    private val sessionStore: SessionStore? = null
) {

    private val gson        = Gson()
    private val istTimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    suspend fun login(username: String, password: String): LoginResponse {
        return api.login(username, password)
    }

    fun restoreToken(token: String) {
        PesuApiClient.authToken = token
    }

    fun logout() {
        PesuApiClient.clearSession()
    }

    suspend fun getSeatingInfo(userId: String): Result<List<SeatingInfo>> {
        return try {
            val cached = sessionStore?.getSeatingCache()
            if (cached != null) {
                val type = object : TypeToken<List<SeatingInfo>>() {}.type
                val list: List<SeatingInfo> = gson.fromJson(cached, type) ?: emptyList()
                if (list.isNotEmpty()) return Result.success(list)
            }
            val fresh = api.getSeatingInfo(userId)
            if (fresh.isNotEmpty()) sessionStore?.saveSeatingCache(fresh)
            Result.success(fresh)
        } catch (e: PesuError.SessionExpired) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(PesuError.Network(e))
        }
    }

    suspend fun getTodayClasses(
        userId: String,
        date: Calendar = Calendar.getInstance(),
        forceRefresh: Boolean = false
    ): TodaySchedule {

        val calendar = getCalendarEvents(userId, forceRefresh)
        val holiday  = isHolidayDate(calendar, date)
        if (holiday != null) return TodaySchedule(classes = emptyList(), holiday = holiday)

        val dayOfWeek = getPesuDayOfWeek(date)
        if (dayOfWeek == 0) return TodaySchedule(classes = emptyList())

        val timetable = getTimetable(userId, dayOfWeek, forceRefresh)
            .filter { it.day == dayOfWeek }
            .sortedBy { it.timeSlotOrder }

        if (timetable.isEmpty()) return TodaySchedule(classes = emptyList())

        val semester = getSemester(userId, forceRefresh)
            ?: return TodaySchedule(classes = timetable.map { it.toUpcoming() })

        val subjectInfoMap = getSubjectInfoMap(userId, semester, forceRefresh)

        val now     = Calendar.getInstance()
        val isToday = isSameDay(date, now)
        val sdf     = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = istTimeZone
        }
        val dateStr = sdf.format(date.time)

        val attendanceCache = sessionStore?.getAttendanceCache()?.toMutableMap()
            ?: mutableMapOf()

        val endedEntries = timetable.filter { entry ->
            val startCal = parseTime(entry.startTime, date)
            val endCal   = parseTime(entry.endTime, date)
            when {
                !isToday                                   -> false
                now.before(startCal)                       -> false
                now.after(startCal) && now.before(endCal)  -> false
                else                                       -> true
            }
        }

        val detailResults: Map<String, List<AttendanceDetail>> = coroutineScope {
            endedEntries
                .mapNotNull { entry ->
                    val info     = subjectInfoMap[entry.subjectCode] ?: return@mapNotNull null
                    if (info.idType == null) return@mapNotNull null
                    val cacheKey = "${entry.subjectCode}_$dateStr"
                    val cached   = if (!forceRefresh) attendanceCache[cacheKey] else null
                    if (cached?.status == ClassStatus.ATTENDED) return@mapNotNull null
                    async {
                        entry.subjectCode to api.getAttendanceDetail(
                            userId              = userId,
                            subjectId           = info.subjectId,
                            idType              = info.idType,
                            batchClassId        = info.batchClassId,
                            classBatchSectionId = info.classBatchSectionId,
                            attended            = null,
                            total               = null,
                            subjectCode         = info.subjectCode,
                            subjectName         = entry.subjectName,
                            percentage          = null
                        )
                    }
                }
                .awaitAll()
                .toMap()
        }

        val result  = mutableListOf<TodayClass>()
        val updated = mutableMapOf<String, CachedAttendanceRecord>()

        for (entry in timetable) {
            val startCal = parseTime(entry.startTime, date)
            val endCal   = parseTime(entry.endTime, date)

            val classEnded = when {
                !isToday                                   -> false
                now.before(startCal)                       -> false
                now.after(startCal) && now.before(endCal)  -> false
                else                                       -> true
            }

            val info     = subjectInfoMap[entry.subjectCode]
            val cacheKey = "${entry.subjectCode}_$dateStr"
            val cached   = if (!forceRefresh) attendanceCache[cacheKey] else null

            val status: ClassStatus
            var attended = 0
            var total    = 0
            var pct      = 0f

            when {
                !classEnded && isToday && now.before(startCal) -> {
                    status = ClassStatus.UPCOMING
                }
                !classEnded && isToday && now.after(startCal) && now.before(endCal) -> {
                    status = ClassStatus.ONGOING
                }
                !classEnded -> {
                    status = ClassStatus.UPCOMING
                }
                cached?.status == ClassStatus.ATTENDED -> {
                    status   = ClassStatus.ATTENDED
                    attended = cached.attendedCount
                    total    = cached.totalCount
                    pct      = cached.percentage
                }
                info == null || info.idType == null -> {
                    status = ClassStatus.NOT_MARKED
                }
                else -> {
                    val details = detailResults[entry.subjectCode] ?: emptyList()
                    val record  = details.find { detail ->
                        val d = sdf.format(Date(detail.dateOfAttendance))
                        d == dateStr
                    }

                    attended = details.count { it.status == 1 }
                    total    = details.size
                    pct      = if (total > 0) (attended.toFloat() / total) * 100f else 0f

                    status = when {
                        record == null     -> ClassStatus.NOT_MARKED
                        record.status == 1 -> ClassStatus.ATTENDED
                        else               -> ClassStatus.BUNKED
                    }

                    if (status == ClassStatus.ATTENDED) {
                        updated[cacheKey] = CachedAttendanceRecord(
                            subjectCode   = entry.subjectCode,
                            dateStr       = dateStr,
                            status        = status,
                            attendedCount = attended,
                            totalCount    = total,
                            percentage    = pct
                        )
                    }
                }
            }

            result.add(
                TodayClass(
                    subjectName   = entry.subjectName,
                    subjectCode   = entry.subjectCode,
                    roomName      = entry.roomName,
                    startTime     = entry.startTime,
                    endTime       = entry.endTime,
                    timeSlotOrder = entry.timeSlotOrder,
                    status        = status,
                    attendedCount = attended,
                    totalCount    = total,
                    percentage    = pct
                )
            )
        }

        if (updated.isNotEmpty() && sessionStore != null) {
            val merged = attendanceCache.toMutableMap()
            merged.putAll(updated)
            sessionStore.saveAttendanceCache(merged)
        }

        return TodaySchedule(classes = result)
    }

    suspend fun getCalendarEventsPublic(userId: String): List<CalendarEvent> =
        getCalendarEvents(userId, forceRefresh = false)

    suspend fun getFullTimetable(userId: String): List<TimetableEntry> =
        getTimetable(userId, dayOfWeek = 1, forceRefresh = false)

    suspend fun getAttendanceSummary(userId: String): List<AttendanceSubject> {
        val semester = getSemester(userId, forceRefresh = false) ?: return emptyList()
        getSubjectInfoMap(userId, semester, forceRefresh = false)
        val summary = api.getAttendanceSummary(userId, semester.batchClassId)
        return summary.values.toList()
    }

    suspend fun getAttendanceSemestersFull(userId: String): List<AttendanceSemester> {
        return api.getAttendanceSemestersFull(userId)
    }

    suspend fun getResultSemesters(usn: String): Pair<List<StudentSemester>, Boolean> {
        return api.getResultSemesters(usn)
    }

    suspend fun getIsaMarks(
        userId: String,
        batchClassId: Int,
        classBatchSectionId: Int
    ): Map<String, List<IsaMarkEntry>> {
        return api.getIsaMarks(userId, batchClassId, classBatchSectionId)
    }

    suspend fun getEsaResults(usn: String): List<EsaResultEntry> {
        return api.getEsaResults(usn)
    }

    suspend fun getSemesterGrades(
        batchClassId: Int,
        classesId: Int,
        userId: String,
        usn: String,
        className: String,
        isFinalised: Boolean
    ): Pair<List<SemesterGradeResult>, CgpaSemesterWise?> {
        return api.getSemesterGrades(batchClassId, classesId, userId, usn, className, isFinalised)
    }

    private suspend fun getCalendarEvents(
        userId: String,
        forceRefresh: Boolean
    ): List<CalendarEvent> {
        if (!forceRefresh && sessionStore != null) {
            val cached = sessionStore.getCalendarCache()
            if (cached != null) {
                return try {
                    val type = object : TypeToken<List<CalendarEvent>>() {}.type
                    gson.fromJson(cached, type) ?: emptyList()
                } catch (e: Exception) { emptyList() }
            }
        }
        return try {
            val events = api.getCalendarEvents(userId)
            sessionStore?.saveCalendarCache(gson.toJson(events))
            events
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun getTimetable(
        userId: String,
        dayOfWeek: Int,
        forceRefresh: Boolean
    ): List<TimetableEntry> {
        if (!forceRefresh && sessionStore != null) {
            val cached = sessionStore.getTimetableCache()
            if (cached != null) {
                return try {
                    val type = object : TypeToken<List<TimetableEntry>>() {}.type
                    val all: List<TimetableEntry> = gson.fromJson(cached, type) ?: emptyList()
                    if (all.isNotEmpty()) return all
                    emptyList()
                } catch (e: Exception) { emptyList() }
            }
        }
        val entries = api.getTimetable(userId, dayOfWeek)
        sessionStore?.saveTimetableCache(gson.toJson(entries))
        return entries
    }

    private suspend fun getSemester(
        userId: String,
        forceRefresh: Boolean
    ): AttendanceSemester? {
        if (!forceRefresh && sessionStore != null) {
            val cached = sessionStore.getSemesterCache()
            if (cached != null) {
                return try {
                    gson.fromJson(cached, AttendanceSemester::class.java)
                } catch (e: Exception) { null }
            }
        }
        val semesters = api.getAttendanceSemesters(userId)
        val current   = semesters.firstOrNull() ?: return null
        sessionStore?.saveSemesterCache(gson.toJson(current))
        return current
    }

    private suspend fun getSubjectInfoMap(
        userId: String,
        semester: AttendanceSemester,
        forceRefresh: Boolean
    ): Map<String, CachedSubjectInfo> {
        if (!forceRefresh && sessionStore != null) {
            val cached = sessionStore.getSubjectsCache()
            if (!cached.isNullOrEmpty()) return cached
        }
        val summary = api.getAttendanceSummary(userId, semester.batchClassId)
        val map = summary.values.associate { subject ->
            subject.subjectCode to CachedSubjectInfo(
                subjectId           = subject.subjectId,
                subjectCode         = subject.subjectCode,
                subjectName         = subject.subjectName,
                idType              = subject.idType,
                batchClassId        = semester.batchClassId,
                classBatchSectionId = semester.classBatchSectionId
            )
        }
        sessionStore?.saveSubjectsCache(map)
        return map
    }

    suspend fun hardRefresh(userId: String, date: Calendar): TodaySchedule {
        sessionStore?.clearAllCaches()
        return getTodayClasses(userId, date, forceRefresh = true)
    }

    private fun isHolidayDate(events: List<CalendarEvent>, date: Calendar): CalendarEvent? {
        val dayStart = (date.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = dayStart + 24 * 60 * 60 * 1000L
        return events.firstOrNull {
            it.isHolidayDay() && it.startDate < dayEnd && it.endDate > dayStart
        }
    }

    private fun getPesuDayOfWeek(cal: Calendar): Int = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY    -> 1
        Calendar.TUESDAY   -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY  -> 4
        Calendar.FRIDAY    -> 5
        Calendar.SATURDAY  -> 6
        else               -> 0
    }

    private fun parseTime(timeStr: String, onDate: Calendar): Calendar {
        val parts = timeStr.split(":")
        return (onDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            set(Calendar.MINUTE, parts[1].toInt())
            set(Calendar.SECOND, parts.getOrElse(2) { "0" }.toInt())
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun TimetableEntry.toUpcoming() = TodayClass(
        subjectName   = subjectName,
        subjectCode   = subjectCode,
        roomName      = roomName,
        startTime     = startTime,
        endTime       = endTime,
        timeSlotOrder = timeSlotOrder,
        status        = ClassStatus.UPCOMING
    )
}
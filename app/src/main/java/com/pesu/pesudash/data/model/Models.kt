package com.pesu.pesudash.data.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val login: String,
    val userId: String,
    val userName: String? = null,
    val name: String? = null,
    val srn: String? = null,
    val departmentId: String? = null,
    val loginId: String? = null,
    val className: String? = null,
    val branch: String? = null,
    val program: String? = null,
    val sectionName: String? = null,
    val photo: String? = null,
    val batchClass: String? = null,
    val classBatchSection: String? = null,
    val programId: Int? = null,
    val classId: Int? = null,
    val instId: Int? = null,
    val phone: String? = null,
    val email: String? = null
)

data class TimetableEntry(
    val timeTableId: String,
    val subjectId: Int,
    val subjectName: String,
    val subjectCode: String,
    val roomName: String,
    val day: Int,
    val startTime: String,
    val endTime: String,
    val timeSlotOrder: Int,
    val classBatchSectionId: Int,
    val userId: String
)

data class AttendanceSemester(
    val batchClassId: Int,
    val classBatchSectionId: Int,
    val className: String
)

data class AttendanceSubject(
    @SerializedName("SubjectId")          val subjectId: Int,
    @SerializedName("SubjectCode")        val subjectCode: String,
    @SerializedName("SubjectName")        val subjectName: String,
    @SerializedName("AttendedClasses")    val attended: Float?,
    @SerializedName("TotalClasses")       val total: Int?,
    @SerializedName("AttendancePercenrage") val percentage: Float?,
    @SerializedName("IdType")             val idType: Int?
)

data class AttendanceDetail(
    @SerializedName("DateOfAttendance") val dateOfAttendance: Long,
    @SerializedName("Status")           val status: Int,
    @SerializedName("StartTime")        val startTime: String?,
    @SerializedName("EndTime")          val endTime: String?
)

enum class ClassStatus {
    ATTENDED,
    BUNKED,
    UPCOMING,
    ONGOING,
    NOT_MARKED
}

data class TodayClass(
    val subjectName: String,
    val subjectCode: String,
    val roomName: String,
    val startTime: String,
    val endTime: String,
    val timeSlotOrder: Int,
    val status: ClassStatus,
    val attendedCount: Int = 0,
    val totalCount: Int = 0,
    val percentage: Float = 0f
)

data class CalendarEvent(
    val calendarEventDetailId: Int,
    val name: String,
    val description: String,
    val eventType: String,
    val startDate: Long,
    val endDate: Long,
    val isHoliday: Int,
    val isClass: Int
) {
    fun isHolidayDay(): Boolean = isHoliday == 1
}

data class TodaySchedule(
    val classes: List<TodayClass>,
    val holiday: CalendarEvent? = null
)

data class CachedSubjectInfo(
    val subjectId: Int,
    val subjectCode: String,
    val subjectName: String,
    val idType: Int?,
    val batchClassId: Int,
    val classBatchSectionId: Int
)

data class CachedAttendanceRecord(
    val subjectCode: String,
    val dateStr: String,
    val status: ClassStatus,
    val attendedCount: Int,
    val totalCount: Int,
    val percentage: Float
)

data class UserProfile(
    val userId: String,
    val name: String,
    val srn: String,
    val className: String,
    val branch: String,
    val program: String,
    val photo: String?
)

data class SeatingInfo(
    @SerializedName("StudentId")      val studentId: String,
    @SerializedName("Status")         val status: Int,
    @SerializedName("SubjectName")    val subjectName: String,
    @SerializedName("AssessmentName") val assessmentName: String,
    @SerializedName("TerminalName")   val terminalName: String,
    @SerializedName("AssessmentId")   val assessmentId: Int,
    @SerializedName("TestEndTime")    val testEndTime: Long,
    @SerializedName("TestStartTime")  val testStartTime: Long,
    @SerializedName("RoomName")       val roomName: String,
    @SerializedName("SubjectCode")    val subjectCode: String
)
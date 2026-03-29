package com.pesu.pesudash.data.network

import com.pesu.pesudash.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.pesu.pesudash.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class PesuApiService {

    private val gson   = Gson()
    private val client = PesuApiClient.client

    companion object {
        private const val TAG = "PesuApi"
    }

    suspend fun initSession() = withContext(Dispatchers.IO) {
        val req = PesuApiClient.buildDispatcherRequest(
            mapOf(
                "action"     to "20",
                "mode"       to "5",
                "callMethod" to "background",
                "minLimit"   to "0",
                "limit"      to "10"
            )
        )
        val resp = client.newCall(req).execute()
        resp.close()
    }

    suspend fun login(username: String, password: String): LoginResponse =
        withContext(Dispatchers.IO) {
            initSession()

            val loginReq       = PesuApiClient.buildLoginRequest(username, password)
            val loginResp      = PesuApiClient.loginClient.newCall(loginReq).execute()
            val statusCode     = loginResp.code
            val locationHeader = loginResp.header("Location") ?: ""
            loginResp.close()

            if (statusCode != 302 || locationHeader.contains("login", ignoreCase = true)) {
                throw PesuError.Auth("Invalid credentials")
            }

            val redirResp = PesuApiClient.loginClient.newCall(
                PesuApiClient.buildGetRequest("a/0")
            ).execute()
            redirResp.close()

            val successResp = client.newCall(
                PesuApiClient.buildGetRequest("mobile/mobileAppLoginSuccess")
            ).execute()

            val token = successResp.header("mobileAppAuthenticationToken")
            val body  = successResp.body?.string() ?: throw PesuError.Auth("Empty response")
            successResp.close()

            val user = try {
                gson.fromJson(body, LoginResponse::class.java)
            } catch (e: Exception) {
                throw PesuError.Parse(e)
            }

            if (user.login != "SUCCESS") throw PesuError.Auth("Login failed: ${user.login}")

            PesuApiClient.authToken = token
            user
        }

    suspend fun getTimetable(userId: String, dayOfWeek: Int): List<TimetableEntry> =
        withContext(Dispatchers.IO) {
            val req = PesuApiClient.buildDispatcherRequest(
                mapOf(
                    "action"              to "16",
                    "mode"                to "1",
                    "userId"              to userId,
                    "refreshTimetableDay" to dayOfWeek.toString()
                )
            )
            val resp = client.newCall(req).execute()
            checkForSessionExpiry(resp.code)
            val body = resp.body?.string() ?: "[]"
            resp.close()
            val type = object : TypeToken<List<TimetableEntry>>() {}.type
            gson.fromJson(body, type) ?: emptyList()
        }

    suspend fun getAttendanceSemesters(userId: String): List<AttendanceSemester> =
        withContext(Dispatchers.IO) {
            val req = PesuApiClient.buildDispatcherRequest(
                mapOf(
                    "action"        to "18",
                    "mode"          to "1",
                    "whichObjectId" to "clickHome_pesuacademy_attendance",
                    "title"         to "Attendance",
                    "userId"        to userId,
                    "deviceType"    to "1",
                    "serverMode"    to "0",
                    "programId"     to "0",
                    "redirectValue" to "redirect:/a/ad"
                )
            )
            val resp = client.newCall(req).execute()
            checkForSessionExpiry(resp.code)
            val body = resp.body?.string() ?: "[]"
            resp.close()
            val type = object : TypeToken<List<AttendanceSemester>>() {}.type
            gson.fromJson(body, type) ?: emptyList()
        }

    suspend fun getAttendanceSummary(
        userId: String,
        batchClassId: Int
    ): Map<String, AttendanceSubject> = withContext(Dispatchers.IO) {
        val req = PesuApiClient.buildDispatcherRequest(
            mapOf(
                "action"       to "18",
                "mode"         to "6",
                "batchClassId" to batchClassId.toString(),
                "userId"       to userId,
                "semIndexVal"  to "0"
            )
        )
        val resp = client.newCall(req).execute()
        checkForSessionExpiry(resp.code)
        val body = resp.body?.string() ?: "{}"
        resp.close()

        val json = gson.fromJson(body, JsonObject::class.java)
        val list = json.getAsJsonArray("ATTENDANCE_LIST") ?: return@withContext emptyMap()
        val type = object : TypeToken<List<AttendanceSubject>>() {}.type
        val subjects: List<AttendanceSubject> = gson.fromJson(list, type) ?: emptyList()
        subjects.associateBy { it.subjectCode }
    }

    suspend fun getAttendanceDetail(
        userId: String,
        subjectId: Int,
        idType: Int?,
        batchClassId: Int,
        classBatchSectionId: Int,
        attended: Float?,
        total: Int?,
        subjectCode: String,
        subjectName: String,
        percentage: Float?
    ): List<AttendanceDetail> = withContext(Dispatchers.IO) {

        fun s(v: Any?): String = when {
            v == null -> "null"
            v is Float && v == v.toInt().toFloat() -> v.toInt().toString()
            else -> v.toString()
        }

        val subjectInfo = listOf(
            "null", "null",
            s(attended), s(total),
            subjectCode.ifEmpty { "null" },
            subjectName.ifEmpty { "null" },
            s(percentage)
        ).joinToString("&&")

        val req = PesuApiClient.buildDispatcherRequest(
            mapOf(
                "action"              to "18",
                "mode"                to "16",
                "subjectId"           to subjectId.toString(),
                "idType"              to (idType?.toString() ?: ""),
                "userId"              to userId,
                "batchClassId"        to batchClassId.toString(),
                "classBatchSectionId" to classBatchSectionId.toString(),
                "subjectInfo"         to subjectInfo
            )
        )
        val resp = client.newCall(req).execute()
        checkForSessionExpiry(resp.code)
        val body = resp.body?.string() ?: "[]"
        resp.close()
        val type = object : TypeToken<List<AttendanceDetail>>() {}.type
        gson.fromJson(body, type) ?: emptyList()
    }

    suspend fun getCalendarEvents(userId: String): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            val req = PesuApiClient.buildDispatcherRequest(
                mapOf(
                    "action"            to "14",
                    "mode"              to "1",
                    "usn"               to userId,
                    "isCalendarRefresh" to "true"
                )
            )
            val resp = client.newCall(req).execute()
            checkForSessionExpiry(resp.code)
            val body = resp.body?.string() ?: "[]"
            resp.close()
            val type = object : TypeToken<List<CalendarEvent>>() {}.type
            gson.fromJson(body, type) ?: emptyList()
        }

    suspend fun getSeatingInfo(userId: String): List<SeatingInfo> =
        withContext(Dispatchers.IO) {
            val req = PesuApiClient.buildDispatcherRequest(
                mapOf(
                    "action"        to "13",
                    "mode"          to "3",
                    "whichObjectId" to "clickHome_pesuacademy_seatinfo",
                    "title"         to "Seating Info",
                    "userId"        to userId,
                    "deviceType"    to "1",
                    "serverMode"    to "0",
                    "redirectValue" to "redirect:/a/ad"
                )
            )
            val resp = client.newCall(req).execute()
            checkForSessionExpiry(resp.code)
            val body = resp.body?.string() ?: "[]"
            resp.close()
            if (BuildConfig.DEBUG) Log.d(TAG, "SeatingInfo response: $body")
            val type = object : TypeToken<List<SeatingInfo>>() {}.type
            gson.fromJson<List<SeatingInfo>>(body, type) ?: emptyList()
        }

    private fun checkForSessionExpiry(code: Int) {
        if (code == 401 || code == 403) throw PesuError.SessionExpired
    }
}
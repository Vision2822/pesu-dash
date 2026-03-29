package com.pesu.pesudash.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pesu.pesudash.data.model.CachedAttendanceRecord
import com.pesu.pesudash.data.model.CachedSubjectInfo
import com.pesu.pesudash.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session")

class SessionStore(private val context: Context) {

    private val gson = Gson()

    companion object {
        private const val CURRENT_CACHE_VERSION = 1

        private val KEY_CACHE_VERSION = intPreferencesKey("cache_version")
        private val KEY_TOKEN         = stringPreferencesKey("auth_token")
        private val KEY_USER_ID       = stringPreferencesKey("user_id")
        private val KEY_NAME          = stringPreferencesKey("user_name")
        private val KEY_SRN           = stringPreferencesKey("user_srn")
        private val KEY_CLASS_NAME    = stringPreferencesKey("class_name")
        private val KEY_BRANCH        = stringPreferencesKey("branch")
        private val KEY_PROGRAM       = stringPreferencesKey("program")
        private val KEY_PHOTO         = stringPreferencesKey("photo")
        private val KEY_TIMETABLE     = stringPreferencesKey("timetable_cache")
        private val KEY_SEMESTER      = stringPreferencesKey("semester_cache")
        private val KEY_SUBJECTS      = stringPreferencesKey("subjects_cache")
        private val KEY_CALENDAR      = stringPreferencesKey("calendar_cache")
        private val KEY_ATTENDANCE    = stringPreferencesKey("attendance_cache")
        private val KEY_TARGET_PCT    = stringPreferencesKey("target_pct")
        private val KEY_SEM_END_DATE  = stringPreferencesKey("sem_end_date")
        private val KEY_THEME         = stringPreferencesKey("theme_mode")
        private val KEY_ACCENT        = stringPreferencesKey("accent_color")
        private val KEY_AVATAR_CACHE  = stringPreferencesKey("avatar_cache")
        private val KEY_SEATING_CACHE = stringPreferencesKey("seating_cache")
        private val KEY_OFFLINE_MODE  = stringPreferencesKey("offline_mode")
    }

    val authToken:  Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val userId:     Flow<String?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val userName:   Flow<String?> = context.dataStore.data.map { it[KEY_NAME] }
    val srn:        Flow<String?> = context.dataStore.data.map { it[KEY_SRN] }
    val className:  Flow<String?> = context.dataStore.data.map { it[KEY_CLASS_NAME] }
    val branch:     Flow<String?> = context.dataStore.data.map { it[KEY_BRANCH] }
    val program:    Flow<String?> = context.dataStore.data.map { it[KEY_PROGRAM] }
    val photo:      Flow<String?> = context.dataStore.data.map { it[KEY_PHOTO] }
    val themeMode:  Flow<String>  = context.dataStore.data.map { it[KEY_THEME] ?: "SYSTEM" }
    val accentColor: Flow<String?> = context.dataStore.data.map { it[KEY_ACCENT] }

    suspend fun validateCacheVersion() {
        val version = context.dataStore.data.first()[KEY_CACHE_VERSION] ?: 0
        if (version < CURRENT_CACHE_VERSION) {
            clearAllCaches()
            context.dataStore.edit { it[KEY_CACHE_VERSION] = CURRENT_CACHE_VERSION }
        }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME] = mode }
    }

    suspend fun saveAccentColor(argbHex: String) {
        context.dataStore.edit { it[KEY_ACCENT] = argbHex }
    }

    suspend fun getAvatarCache(): Map<String, String> {
        val json = context.dataStore.data.first()[KEY_AVATAR_CACHE] ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) { emptyMap() }
    }

    suspend fun saveAvatarCache(cache: Map<String, String>) {
        context.dataStore.edit { it[KEY_AVATAR_CACHE] = gson.toJson(cache) }
    }

    suspend fun clearAvatarCache() {
        context.dataStore.edit { it.remove(KEY_AVATAR_CACHE) }
    }

    suspend fun saveProfile(profile: UserProfile, token: String) {
        context.dataStore.edit {
            it[KEY_TOKEN]      = token
            it[KEY_USER_ID]    = profile.userId
            it[KEY_NAME]       = profile.name
            it[KEY_SRN]        = profile.srn
            it[KEY_CLASS_NAME] = profile.className
            it[KEY_BRANCH]     = profile.branch
            it[KEY_PROGRAM]    = profile.program
            if (profile.photo != null) it[KEY_PHOTO] = profile.photo
        }
    }

    suspend fun save(token: String, userId: String, userName: String, srn: String = "") {
        context.dataStore.edit {
            it[KEY_TOKEN]   = token
            it[KEY_USER_ID] = userId
            it[KEY_NAME]    = userName
            it[KEY_SRN]     = srn
        }
    }

    suspend fun getProfile(): UserProfile? {
        val prefs = context.dataStore.data.first()
        val uid   = prefs[KEY_USER_ID] ?: return null
        return UserProfile(
            userId    = uid,
            name      = prefs[KEY_NAME]      ?: "Student",
            srn       = prefs[KEY_SRN]       ?: "",
            className = prefs[KEY_CLASS_NAME] ?: "",
            branch    = prefs[KEY_BRANCH]    ?: "",
            program   = prefs[KEY_PROGRAM]   ?: "",
            photo     = prefs[KEY_PHOTO]
        )
    }

    suspend fun saveTimetableCache(json: String) {
        context.dataStore.edit { it[KEY_TIMETABLE] = json }
    }

    suspend fun getTimetableCache(): String? =
        context.dataStore.data.first()[KEY_TIMETABLE]

    suspend fun saveSemesterCache(json: String) {
        context.dataStore.edit { it[KEY_SEMESTER] = json }
    }

    suspend fun getSemesterCache(): String? =
        context.dataStore.data.first()[KEY_SEMESTER]

    suspend fun saveSubjectsCache(subjects: Map<String, CachedSubjectInfo>) {
        context.dataStore.edit { it[KEY_SUBJECTS] = gson.toJson(subjects) }
    }

    suspend fun getSubjectsCache(): Map<String, CachedSubjectInfo>? {
        val json = context.dataStore.data.first()[KEY_SUBJECTS] ?: return null
        return try {
            val type = object : TypeToken<Map<String, CachedSubjectInfo>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) { null }
    }

    suspend fun saveCalendarCache(json: String) {
        context.dataStore.edit { it[KEY_CALENDAR] = json }
    }

    suspend fun getCalendarCache(): String? =
        context.dataStore.data.first()[KEY_CALENDAR]

    suspend fun getAttendanceCache(): MutableMap<String, CachedAttendanceRecord> {
        val json = context.dataStore.data.first()[KEY_ATTENDANCE] ?: return mutableMapOf()
        return try {
            val type = object : TypeToken<MutableMap<String, CachedAttendanceRecord>>() {}.type
            gson.fromJson(json, type) ?: mutableMapOf()
        } catch (e: Exception) { mutableMapOf() }
    }

    suspend fun saveAttendanceCache(cache: Map<String, CachedAttendanceRecord>) {
        context.dataStore.edit { it[KEY_ATTENDANCE] = gson.toJson(cache) }
    }

    suspend fun saveSeatingCache(list: List<Any>) {
        context.dataStore.edit { it[KEY_SEATING_CACHE] = gson.toJson(list) }
    }

    suspend fun getSeatingCache(): String? =
        context.dataStore.data.first()[KEY_SEATING_CACHE]

    suspend fun clearSeatingCache() {
        context.dataStore.edit { it.remove(KEY_SEATING_CACHE) }
    }

    suspend fun getTargetPct(): Float {
        val raw = context.dataStore.data.first()[KEY_TARGET_PCT] ?: return 75f
        return raw.toFloatOrNull() ?: 75f
    }

    suspend fun saveTargetPct(pct: Float) {
        context.dataStore.edit { it[KEY_TARGET_PCT] = pct.toString() }
    }

    suspend fun getSemEndDate(): Long {
        val raw = context.dataStore.data.first()[KEY_SEM_END_DATE] ?: return 0L
        return raw.toLongOrNull() ?: 0L
    }

    suspend fun saveSemEndDate(epochMs: Long) {
        context.dataStore.edit { it[KEY_SEM_END_DATE] = epochMs.toString() }
    }

    suspend fun isOffline(): Boolean {
        return context.dataStore.data.first()[KEY_OFFLINE_MODE] == "true"
    }

    suspend fun clearAttendanceCache() {
        context.dataStore.edit { it.remove(KEY_ATTENDANCE) }
    }

    suspend fun clearAllCaches() {
        context.dataStore.edit {
            it.remove(KEY_TIMETABLE)
            it.remove(KEY_SEMESTER)
            it.remove(KEY_SUBJECTS)
            it.remove(KEY_CALENDAR)
            it.remove(KEY_ATTENDANCE)
            it.remove(KEY_SEATING_CACHE)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
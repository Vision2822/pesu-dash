package com.pesu.pesudash.widget

import android.content.Context
import android.util.Log
import com.pesu.pesudash.data.local.SessionStore
import com.pesu.pesudash.data.model.ClassStatus
import com.pesu.pesudash.data.network.PesuApiClient
import com.pesu.pesudash.data.repository.PesuRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

object WidgetUpdater {

    private const val TAG = "WidgetUpdater"

    suspend fun fetchAndStore(context: Context, forceRefresh: Boolean = false) {
        val sessionStore = SessionStore(context)
        val token  = sessionStore.authToken.first()
        val userId = sessionStore.userId.first()

        if (token == null || userId == null) {
            Log.d(TAG, "Not logged in - skipping widget update")
            WidgetStateStore.save(context, WidgetData(isLoggedOut = true))
            return
        }

        PesuApiClient.authToken = token
        val repo = PesuRepository(sessionStore = sessionStore)

        try {
            val schedule = if (forceRefresh) {
                repo.hardRefresh(userId, Calendar.getInstance())
            } else {
                repo.getTodayClasses(userId, Calendar.getInstance())
            }

            val classes = schedule.classes

            val widgetClasses = classes.map {
                WidgetClassItem(
                    subjectName = it.subjectName,
                    subjectCode = it.subjectCode,
                    startTime   = it.startTime,
                    endTime     = it.endTime,
                    roomName    = it.roomName,
                    status      = it.status
                )
            }

            val now = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

            WidgetStateStore.save(
                context,
                WidgetData(
                    classes     = widgetClasses,
                    attended    = classes.count { it.status == ClassStatus.ATTENDED },
                    upcoming    = classes.count {
                        it.status == ClassStatus.UPCOMING || it.status == ClassStatus.ONGOING
                    },
                    bunked      = classes.count { it.status == ClassStatus.BUNKED },
                    notMarked   = classes.count { it.status == ClassStatus.NOT_MARKED },
                    lastUpdated = now,
                    isLoading   = false,
                    error       = null,
                    isLoggedOut = false
                )
            )
            Log.d(TAG, "Widget data saved: ${widgetClasses.size} classes")

        } catch (e: Exception) {
            Log.e(TAG, "Widget fetch failed", e)
            val old = WidgetStateStore.load(context)
            WidgetStateStore.save(context, old.copy(error = "Failed to refresh", isLoading = false))
        }
    }
}
package com.pesu.pesudash.widget

import android.content.Context
import com.google.gson.Gson
import com.pesu.pesudash.data.model.ClassStatus

object WidgetStateStore {

    private const val PREFS = "widget_state"
    private const val KEY_DATA = "widget_data"
    private val gson = Gson()

    fun save(context: Context, data: WidgetData) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DATA, gson.toJson(data))
            .apply()
    }

    fun load(context: Context): WidgetData {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_DATA, null) ?: return WidgetData(isLoading = true)
        return try {
            gson.fromJson(json, WidgetData::class.java)
        } catch (e: Exception) {
            WidgetData(isLoading = true)
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}
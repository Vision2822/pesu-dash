package com.pesu.pesudash.widget

import com.pesu.pesudash.data.model.ClassStatus

data class WidgetClassItem(
    val subjectName: String,
    val subjectCode: String,
    val startTime: String,
    val endTime: String,
    val roomName: String,
    val status: ClassStatus
)

data class WidgetData(
    val classes: List<WidgetClassItem> = emptyList(),
    val attended: Int = 0,
    val upcoming: Int = 0,
    val bunked: Int = 0,
    val notMarked: Int = 0,
    val lastUpdated: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedOut: Boolean = false
)
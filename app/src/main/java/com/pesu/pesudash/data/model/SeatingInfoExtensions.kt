package com.pesu.pesudash.data.model

import java.util.Calendar

fun SeatingInfo.isToday(): Boolean {
    val now     = System.currentTimeMillis()
    val examCal = Calendar.getInstance().apply { timeInMillis = testStartTime }
    val nowCal  = Calendar.getInstance()
    val sameDay = nowCal.get(Calendar.YEAR) == examCal.get(Calendar.YEAR) &&
            nowCal.get(Calendar.DAY_OF_YEAR) == examCal.get(Calendar.DAY_OF_YEAR)
    return sameDay && now < testEndTime
}

fun SeatingInfo.isTomorrow(): Boolean {
    val now      = System.currentTimeMillis()
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val examCal  = Calendar.getInstance().apply { timeInMillis = testStartTime }
    return tomorrow.get(Calendar.YEAR) == examCal.get(Calendar.YEAR) &&
            tomorrow.get(Calendar.DAY_OF_YEAR) == examCal.get(Calendar.DAY_OF_YEAR) &&
            now < testEndTime
}

fun SeatingInfo.isOngoing(): Boolean {
    val now = System.currentTimeMillis()
    return now in testStartTime..testEndTime
}

fun SeatingInfo.isUpcoming(): Boolean = testStartTime > System.currentTimeMillis()
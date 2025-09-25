package com.tuopacchetto.data

import java.util.Date

data class CalendarEvent(
    val title: String,
    val startTime: Date,
    val endTime: Date? = null,
    val location: String? = null,
    val description: String? = null
) {
    fun isToday(): Boolean {
        val now = Date()
        val today = java.util.Calendar.getInstance().apply {
            time = now
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.time
        
        val tomorrow = java.util.Calendar.getInstance().apply {
            time = today
            add(java.util.Calendar.DAY_OF_MONTH, 1)
        }.time
        
        return startTime >= today && startTime < tomorrow
    }
    
    fun isPast(): Boolean {
        val now = Date()
        return startTime < now
    }
    
    fun getStatusType(): EventStatus {
        return when {
            isPast() -> EventStatus.PAST
            isToday() -> EventStatus.TODAY
            else -> EventStatus.UPCOMING
        }
    }
}

enum class EventStatus {
    TODAY, PAST, UPCOMING
}
package com.tuopacchetto

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.component.VEvent
import java.net.URL
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent

data class WidgetEvent(val text: String, val startDate: Date)

private fun getIcsEvents(): List<WidgetEvent> {
    val url = URL("https://outlook.office365.com/owa/calendar/c05135b8a3904b118721bb88f16e180c@siaksistemi.com/15296e171a174bd69fe09a8ee790bec09509691657482763908/calendar.ics")
    val connection = url.openConnection()
    connection.connectTimeout = 5000
    connection.readTimeout = 5000
    val inputStream = connection.getInputStream()
    val builder = CalendarBuilder()
    val calendar = builder.build(inputStream)
    val eventsList = mutableListOf<WidgetEvent>()

    val inputFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.getDefault())
    val outputFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val now = Date()

    for (component in calendar.components) {
        if (component.name == Component.VEVENT) {
            val event = component as VEvent
            val summary = event.summary.value
            val startDateStr = event.startDate.value
            val startDate = inputFormat.parse(startDateStr)

            if (startDate != null) {
                val formattedDate = outputFormat.format(startDate)
                eventsList.add(WidgetEvent("$formattedDate - $summary", startDate))
            }
        }
    }
    return eventsList
}

class PwaWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    companion object {
         private fun scheduleNextUpdate(context: Context, appWidgetId: Int) {
        val intent = Intent(context, PwaWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intervalMillis = 1000 * 60 * 60 * 8 // 8 ore
        val nextUpdate = System.currentTimeMillis() + intervalMillis

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextUpdate,
            pendingIntent
        )
    }
    fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        // Lanciamo il lavoro in background
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val events = getIcsEvents() // scarica in background

            // Ora aggiorniamo il widget sul main thread
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                val views = RemoteViews(context.packageName, R.layout.pwa_widget_layout)

                views.removeAllViews(R.id.widget_events)
                for (event in events) {
    val eventView = RemoteViews(context.packageName, R.layout.widget_event_item)
    eventView.setTextViewText(R.id.event_title, event.text)

    // Se l'evento è già passato, rendilo opacizzato
    if (event.startDate.before(Date())) {
        eventView.setInt(R.id.event_title, "setAlpha", 100) // 0-255, più basso = più trasparente
    }

    views.addView(R.id.widget_events, eventView)
}

                appWidgetManager.updateAppWidget(appWidgetId, views)

                // Pianifica il prossimo aggiornamento
                PwaWidget().scheduleNextUpdate(context, appWidgetId)
            }
        }
    }
}


package com.tuopacchetto

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.component.VEvent
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import kotlinx.coroutines.*

data class WidgetEvent(val text: String, val startDate: Date)

private fun getIcsEvents(): List<WidgetEvent> {
    return try {
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

        for (component in calendar.components) {
            if (component.name == Component.VEVENT) {
                val event = component as VEvent
                val summary = event.summary?.value ?: "Nessun titolo"
                val startDateStr = event.startDate?.value ?: continue
                val startDate = inputFormat.parse(startDateStr) ?: continue

                val formattedDate = outputFormat.format(startDate)
                eventsList.add(WidgetEvent("$formattedDate - $summary", startDate))
            }
        }
        
        // Ordina gli eventi per data
        eventsList.sortedBy { it.startDate }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
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

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextUpdate,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Fallback per dispositivi che non supportano allarmi esatti
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextUpdate, pendingIntent)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        // Lanciamo il lavoro in background
        CoroutineScope(Dispatchers.IO).launch {
            val events = getIcsEvents() // scarica in background

            // Ora aggiorniamo il widget sul main thread
            withContext(Dispatchers.Main) {
                val views = RemoteViews(context.packageName, R.layout.pwa_widget_layout)

                views.removeAllViews(R.id.widget_events)
                
                if (events.isEmpty()) {
                    // Mostra un messaggio se non ci sono eventi
                    val noEventsView = RemoteViews(context.packageName, R.layout.widget_event_item)
                    noEventsView.setTextViewText(R.id.event_title, "Nessun evento trovato")
                    views.addView(R.id.widget_events, noEventsView)
                } else {
                    for (event in events.take(10)) { // Limita a 10 eventi per evitare overflow
                        val eventView = RemoteViews(context.packageName, R.layout.widget_event_item)
                        eventView.setTextViewText(R.id.event_title, event.text)

                        // Se l'evento è già passato, rendilo opacizzato
                        if (event.startDate.before(Date())) {
                            eventView.setInt(R.id.event_title, "setAlpha", 100) // 0-255, più basso = più trasparente
                        }

                        views.addView(R.id.widget_events, eventView)
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)

                // Pianifica il prossimo aggiornamento
                scheduleNextUpdate(context, appWidgetId)
            }
        }
    }
}

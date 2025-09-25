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
import android.util.Log
import kotlinx.coroutines.*

data class WidgetEvent(val text: String, val startDate: Date)

private fun getIcsEvents(): List<WidgetEvent> {
    return try {
        Log.d("PwaWidget", "Iniziando download ICS...")
        val url = URL("https://outlook.office365.com/owa/calendar/c05135b8a3904b118721bb88f16e180c@siaksistemi.com/15296e171a174bd69fe09a8ee790bec09509691657482763908/calendar.ics")
        val connection = url.openConnection()
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
        
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
                val startDate = try {
                    inputFormat.parse(startDateStr)
                } catch (e: Exception) {
                    // Prova altri formati
                    try {
                        SimpleDateFormat("yyyyMMdd", Locale.getDefault()).parse(startDateStr)
                    } catch (e2: Exception) {
                        continue
                    }
                } ?: continue

                val formattedDate = outputFormat.format(startDate)
                eventsList.add(WidgetEvent("$formattedDate - $summary", startDate))
            }
        }
        
        Log.d("PwaWidget", "Eventi trovati: ${eventsList.size}")
        // Ordina gli eventi per data
        eventsList.sortedBy { it.startDate }
    } catch (e: Exception) {
        Log.e("PwaWidget", "Errore nel caricamento eventi", e)
        emptyList()
    }
}

class PwaWidget : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d("PwaWidget", "Widget abilitato")
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("PwaWidget", "onUpdate chiamato per ${appWidgetIds.size} widget")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d("PwaWidget", "onReceive: ${intent.action}")
        
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE == intent.action) {
            val widgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            if (widgetIds != null) {
                onUpdate(context, AppWidgetManager.getInstance(context), widgetIds)
            }
        }
    }

    private fun scheduleNextUpdate(context: Context, appWidgetId: Int) {
        try {
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
            val intervalMillis = 1000 * 60 * 60 * 2 // 2 ore invece di 8
            val nextUpdate = System.currentTimeMillis() + intervalMillis

            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextUpdate,
                    pendingIntent
                )
                Log.d("PwaWidget", "Prossimo aggiornamento schedulato")
            } catch (e: SecurityException) {
                Log.w("PwaWidget", "Fallback per allarmi esatti")
                alarmManager.set(AlarmManager.RTC_WAKEUP, nextUpdate, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("PwaWidget", "Errore nello scheduling", e)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        Log.d("PwaWidget", "Aggiornamento widget $appWidgetId")
        
        // Mostra subito lo stato di caricamento
        val views = RemoteViews(context.packageName, R.layout.pwa_widget_layout)
        views.removeAllViews(R.id.widget_events)
        
        val loadingView = RemoteViews(context.packageName, R.layout.widget_event_item)
        loadingView.setTextViewText(R.id.event_title, "Caricamento eventi...")
        views.addView(R.id.widget_events, loadingView)
        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Lanciamo il lavoro in background
        CoroutineScope(Dispatchers.IO).launch {
            val events = getIcsEvents()

            // Aggiorniamo il widget sul main thread
            withContext(Dispatchers.Main) {
                try {
                    val updatedViews = RemoteViews(context.packageName, R.layout.pwa_widget_layout)
                    updatedViews.removeAllViews(R.id.widget_events)
                    
                    if (events.isEmpty()) {
                        val noEventsView = RemoteViews(context.packageName, R.layout.widget_event_item)
                        noEventsView.setTextViewText(R.id.event_title, "Nessun evento trovato")
                        updatedViews.addView(R.id.widget_events, noEventsView)
                    } else {
                        val currentTime = Date()
                        for (event in events.take(8)) { // Limita a 8 eventi
                            val eventView = RemoteViews(context.packageName, R.layout.widget_event_item)
                            eventView.setTextViewText(R.id.event_title, event.text)

                            // Se l'evento è già passato, rendilo opacizzato
                            if (event.startDate.before(currentTime)) {
                                eventView.setInt(R.id.event_title, "setAlpha", 100)
                            } else {
                                eventView.setInt(R.id.event_title, "setAlpha", 255)
                            }

                            updatedViews.addView(R.id.widget_events, eventView)
                        }
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, updatedViews)
                    Log.d("PwaWidget", "Widget $appWidgetId aggiornato con successo")

                    // Pianifica il prossimo aggiornamento
                    scheduleNextUpdate(context, appWidgetId)
                } catch (e: Exception) {
                    Log.e("PwaWidget", "Errore nell'aggiornamento UI", e)
                }
            }
        }
    }
}

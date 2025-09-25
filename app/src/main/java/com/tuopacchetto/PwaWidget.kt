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
import java.util.concurrent.Executors
import android.os.Handler
import android.os.Looper

data class WidgetEvent(val text: String, val startDate: Date)

class PwaWidget : AppWidgetProvider() {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

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

    private fun getIcsEvents(): List<WidgetEvent> {
        return try {
            Log.d("PwaWidget", "Iniziando download ICS...")
            val url = URL("https://outlook.office365.com/owa/calendar/c05135b8a3904b118721bb88f16e180c@siaksistemi.com/15296e171a174bd69fe09a8ee790bec09509691657482763908/calendar.ics")
            val connection = url.openConnection()
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            connection.setRequestProperty("Accept", "text/calendar,*/*")
            
            Log.d("PwaWidget", "Connessione aperta, response code: ${(connection as java.net.HttpURLConnection).responseCode}")
            
            val inputStream = connection.getInputStream()
            val builder = CalendarBuilder()
            val calendar = builder.build(inputStream)
            val eventsList = mutableListOf<WidgetEvent>()

            Log.d("PwaWidget", "Calendario caricato, componenti: ${calendar.components.size}")

            val inputFormats = arrayOf(
                SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.getDefault()),
                SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault()),
                SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            )
            val outputFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

            for (component in calendar.components) {
                if (component.name == Component.VEVENT) {
                    val event = component as VEvent
                    val summary = event.summary?.value ?: "Nessun titolo"
                    val startDateStr = event.startDate?.value ?: continue
                    
                    Log.d("PwaWidget", "Evento trovato: $summary, data: $startDateStr")
                    
                    var startDate: Date? = null
                    for (format in inputFormats) {
                        try {
                            startDate = format.parse(startDateStr)
                            break
                        } catch (e: Exception) {
                            continue
                        }
                    }
                    
                    if (startDate == null) {
                        Log.w("PwaWidget", "Impossibile parsare la data: $startDateStr")
                        continue
                    }

                    val formattedDate = outputFormat.format(startDate)
                    eventsList.add(WidgetEvent("$formattedDate - $summary", startDate))
                }
            }
            
            Log.d("PwaWidget", "Eventi trovati: ${eventsList.size}")
            // Ordina gli eventi per data e filtra solo quelli futuri o di oggi
            val now = Calendar.getInstance().apply { 
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            eventsList.filter { it.startDate >= now }
                      .sortedBy { it.startDate }
                      .take(10) // Limita a 10 eventi
        } catch (e: Exception) {
            Log.e("PwaWidget", "Errore nel caricamento eventi", e)
            emptyList()
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
            val intervalMillis = 1000 * 60 * 60 * 1 // 1 ora
            val nextUpdate = System.currentTimeMillis() + intervalMillis

            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextUpdate,
                    pendingIntent
                )
                Log.d("PwaWidget", "Prossimo aggiornamento schedulato in 1 ora")
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
        loadingView.setTextViewText(R.id.event_title, "📅 Caricamento eventi...")
        views.addView(R.id.widget_events, loadingView)
        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Esegui il download in background
        executor.execute {
            Log.d("PwaWidget", "Inizio download dati...")
            val events = getIcsEvents()
            Log.d("PwaWidget", "Download completato, eventi: ${events.size}")

            // Aggiorna il widget sul thread principale
            mainHandler.post {
                try {
                    val updatedViews = RemoteViews(context.packageName, R.layout.pwa_widget_layout)
                    updatedViews.removeAllViews(R.id.widget_events)
                    
                    if (events.isEmpty()) {
                        val noEventsView = RemoteViews(context.packageName, R.layout.widget_event_item)
                        noEventsView.setTextViewText(R.id.event_title, "❌ Nessun evento trovato")
                        updatedViews.addView(R.id.widget_events, noEventsView)
                        
                        // Aggiungi info di debug
                        val debugView = RemoteViews(context.packageName, R.layout.widget_event_item)
                        debugView.setTextViewText(R.id.event_title, "ℹ️ Ultima sincronizzazione: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}")
                        updatedViews.addView(R.id.widget_events, debugView)
                    } else {
                        // Aggiungi header con info
                        val headerView = RemoteViews(context.packageName, R.layout.widget_event_item)
                        headerView.setTextViewText(R.id.event_title, "📅 CALENDARIO SIAK (${events.size} eventi)")
                        updatedViews.addView(R.id.widget_events, headerView)
                        
                        val currentTime = Date()
                        for (event in events) {
                            val eventView = RemoteViews(context.packageName, R.layout.widget_event_item)
                            eventView.setTextViewText(R.id.event_title, event.text)

                            // Se l'evento è già passato, rendilo opacizzato
                            if (event.startDate.before(currentTime)) {
                                eventView.setInt(R.id.event_title, "setAlpha", 128)
                            } else {
                                eventView.setInt(R.id.event_title, "setAlpha", 255)
                            }

                            updatedViews.addView(R.id.widget_events, eventView)
                        }
                        
                        // Aggiungi footer con timestamp
                        val footerView = RemoteViews(context.packageName, R.layout.widget_event_item)
                        footerView.setTextViewText(R.id.event_title, "🔄 Aggiornato: ${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())}")
                        updatedViews.addView(R.id.widget_events, footerView)
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, updatedViews)
                    Log.d("PwaWidget", "Widget $appWidgetId aggiornato con successo")

                    // Pianifica il prossimo aggiornamento
                    scheduleNextUpdate(context, appWidgetId)
                } catch (e: Exception) {
                    Log.e("PwaWidget", "Errore nell'aggiornamento UI", e)
                    
                    // Mostra errore nel widget
                    val errorViews = RemoteViews(context.packageName, R.layout.pwa_widget_layout)
                    errorViews.removeAllViews(R.id.widget_events)
                    val errorView = RemoteViews(context.packageName, R.layout.widget_event_item)
                    errorView.setTextViewText(R.id.event_title, "❌ Errore: ${e.message}")
                    errorViews.addView(R.id.widget_events, errorView)
                    appWidgetManager.updateAppWidget(appWidgetId, errorViews)
                }
            }
        }
    }
}

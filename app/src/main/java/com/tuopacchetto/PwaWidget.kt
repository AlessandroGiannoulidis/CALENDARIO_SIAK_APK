package com.tuopacchetto

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import it.alessandrogiannoulidis.calendariosiak.utils.ToastUtils
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.component.VEvent
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import android.os.Handler
import android.os.Looper

class PwaWidget : AppWidgetProvider() {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d("PwaWidget", "Widget abilitato")
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
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

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        views.setTextViewText(R.id.widgetTitle, context.getString(R.string.widget_loading))
        appWidgetManager.updateAppWidget(appWidgetId, views)

        Thread {
            val events = fetchCalendarEvents(context)
            mainHandler.post {
                if (events.isEmpty()) {
                    views.setTextViewText(R.id.widgetTitle, context.getString(R.string.widget_no_events))
                } else {
                    val header = context.getString(R.string.widget_header_events, events.size)
                    views.setTextViewText(R.id.widgetTitle, header)
                    val content = events.joinToString("\n") { it }
                    views.setTextViewText(R.id.widgetContent, content)
                }
                val updated = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(System.currentTimeMillis())
                views.setTextViewText(R.id.widgetFooter, context.getString(R.string.widget_updated_footer, updated))
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }.start()
    }

    private fun fetchCalendarEvents(context: Context): List<String> {
        return try {
            Log.d("PwaWidget", "Iniziando download ICS...")
            val url = URL("https://outlook.office365.com/owa/calendar/c05135b8a3904b118721bb88f16e180c@siaksistemi.com/15296e171a174bd69fe09a8ee790bec09509691657482763908/calendar.ics")
            val connection = url.openConnection() as HttpURLConnection
            // Configurazione connessione per maggiore robustezza
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            connection.setRequestProperty("Accept", "text/calendar,*/*")
            connection.setRequestProperty("Cache-Control", "no-cache")

            // Verifica response code
            val responseCode = connection.responseCode
            Log.d("PwaWidget", "Connessione aperta, response code: $responseCode")
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w("PwaWidget", "HTTP response non OK: $responseCode")
                return emptyList()
            }

            val inputStream = connection.inputStream
            val builder = CalendarBuilder()
            val calendar = builder.build(inputStream)
            inputStream.close()
            connection.disconnect()
            Log.d("PwaWidget", "Calendario caricato, componenti: ${calendar.components.size}")

            val inputFormats = arrayOf(
                SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.getDefault()),
                SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault()),
                SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            )
            val outputFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

            val eventsList = mutableListOf<String>()
            for (component in calendar.components) {
                if (component.name == Component.VEVENT) {
                    val event = component as VEvent
                    val summary = event.summary?.value ?: context.getString(R.string.event_no_title)
                    val start = event.startDate?.value
                    var formattedStart = ""
                    if (start != null) {
                        for (fmt in inputFormats) {
                            try {
                                val parsed = fmt.parse(start)
                                if (parsed != null) {
                                    formattedStart = outputFormat.format(parsed)
                                    break
                                }
                            } catch (_: Exception) {}
                        }
                    }
                    eventsList.add("$formattedStart - $summary")
                }
            }
            if (eventsList.isEmpty()) {
                listOf(context.getString(R.string.widget_no_events))
            } else {
                eventsList
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("PwaWidget", "Timeout di rete", e)
            listOf(context.getString(R.string.widget_network_timeout))
        } catch (e: java.net.UnknownHostException) {
            Log.e("PwaWidget", "Server non raggiungibile", e)
            listOf(context.getString(R.string.widget_network_unreachable))
        } catch (e: Exception) {
            Log.e("PwaWidget", "Errore generico", e)
            listOf(context.getString(R.string.widget_network_error))
        }
    }
}

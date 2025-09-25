package com.tuopacchetto

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.tuopacchetto.data.CalendarEvent
import com.tuopacchetto.data.EventStatus
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
import android.app.PendingIntent
import java.util.Date

class PwaWidget : AppWidgetProvider() {
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        const val ACTION_RESET = "com.tuopacchetto.ACTION_RESET"
        const val ACTION_REFRESH = "com.tuopacchetto.ACTION_REFRESH"
    }

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

        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val widgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                if (widgetIds != null) {
                    onUpdate(context, AppWidgetManager.getInstance(context), widgetIds)
                }
            }
            ACTION_RESET -> {
                Log.d("PwaWidget", "Reset action triggered")
                // Clear any cached data if needed
                refreshAllWidgets(context)
            }
            ACTION_REFRESH -> {
                Log.d("PwaWidget", "Refresh action triggered")
                refreshAllWidgets(context)
            }
        }
    }

    private fun refreshAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, PwaWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        onUpdate(context, appWidgetManager, widgetIds)
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Setup button click listeners
        setupButtonClickListeners(context, views, appWidgetId)

        // Show loading state
        views.setTextViewText(R.id.widgetFooter, context.getString(R.string.widget_loading))
        views.setTextViewText(R.id.eventCount, "...")
        views.setViewVisibility(R.id.emptyState, 4) // INVISIBLE
        views.setViewVisibility(R.id.eventsContainer, 0) // VISIBLE

        appWidgetManager.updateAppWidget(appWidgetId, views)

        Thread {
            val events = fetchCalendarEvents(context)
            mainHandler.post {
                populateWidget(context, appWidgetManager, appWidgetId, views, events)
            }
        }.start()
    }

    private fun setupButtonClickListeners(context: Context, views: RemoteViews, appWidgetId: Int) {
        // Reset button
        val resetIntent = Intent(context, PwaWidget::class.java).apply {
            action = ACTION_RESET
        }
        val resetPendingIntent = PendingIntent.getBroadcast(
            context, 0, resetIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.resetButton, resetPendingIntent)

        // Refresh button
        val refreshIntent = Intent(context, PwaWidget::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, 1, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.refreshButton, refreshPendingIntent)
    }

    private fun populateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        views: RemoteViews,
        events: List<CalendarEvent>
    ) {
        // Clear the events container
        views.removeAllViews(R.id.eventsContainer)

        if (events.isEmpty()) {
            // Show empty state
            views.setViewVisibility(R.id.emptyState, 0) // VISIBLE
            views.setViewVisibility(R.id.eventsContainer, 4) // INVISIBLE
            views.setTextViewText(R.id.eventCount, "0 eventi")
        } else {
            // Hide empty state and populate events
            views.setViewVisibility(R.id.emptyState, 4) // INVISIBLE
            views.setViewVisibility(R.id.eventsContainer, 0) // VISIBLE
            views.setTextViewText(R.id.eventCount, "${events.size} eventi")

            // Add each event as a separate RemoteViews
            for (event in events.take(5)) { // Limit to 5 events for widget size
                val eventView = createEventView(context, event)
                views.addView(R.id.eventsContainer, eventView)
            }
        }

        // Update footer with current time
        val updated = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(System.currentTimeMillis())
        views.setTextViewText(R.id.widgetFooter, context.getString(R.string.widget_updated_footer, updated))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun createEventView(context: Context, event: CalendarEvent): RemoteViews {
        val preferences = context.getSharedPreferences("widget_settings", Context.MODE_PRIVATE)
        val isCompact = preferences.getBoolean("compact_mode", false)

        val eventView = RemoteViews(context.packageName, R.layout.widget_event_item)

        // Set event title
        eventView.setTextViewText(R.id.eventTitle, event.title)

        // Set event time
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        eventView.setTextViewText(R.id.eventTime, timeFormat.format(event.startTime))

        // Set location if available
        if (!event.location.isNullOrEmpty()) {
            eventView.setTextViewText(R.id.eventLocation, event.location)
            eventView.setViewVisibility(R.id.eventLocation, 0) // VISIBLE
            eventView.setViewVisibility(R.id.eventSeparator, 0) // VISIBLE
        } else {
            eventView.setViewVisibility(R.id.eventLocation, 8) // GONE
            eventView.setViewVisibility(R.id.eventSeparator, 8) // GONE
        }

        // Apply compact mode styling
        if (isCompact) {
            // Reduce text sizes for compact mode
            eventView.setTextViewTextSize(R.id.eventTitle, android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
            eventView.setTextViewTextSize(R.id.eventTime, android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
            if (!event.location.isNullOrEmpty()) {
                eventView.setTextViewTextSize(R.id.eventLocation, android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
            }
            // Make badge smaller in compact mode
            eventView.setTextViewTextSize(R.id.statusBadge, android.util.TypedValue.COMPLEX_UNIT_SP, 7f)
        }

        // Set status border and badge based on event status
        val status = event.getStatusType()
        when (status) {
            EventStatus.TODAY -> {
                eventView.setInt(R.id.statusBorder, "setBackgroundResource", R.drawable.border_today)
                eventView.setTextViewText(R.id.statusBadge, context.getString(R.string.badge_today))
                eventView.setInt(R.id.statusBadge, "setBackgroundResource", R.drawable.badge_background)
                eventView.setViewVisibility(R.id.statusBadge, 0) // VISIBLE
                // Full opacity for today events
                eventView.setTextColor(R.id.eventTitle, context.resources.getColor(R.color.widget_text_primary, null))
                eventView.setTextColor(R.id.eventTime, context.resources.getColor(R.color.widget_text_secondary, null))
            }
            EventStatus.PAST -> {
                eventView.setInt(R.id.statusBorder, "setBackgroundResource", R.drawable.border_past)
                eventView.setTextViewText(R.id.statusBadge, context.getString(R.string.badge_past))
                eventView.setInt(R.id.statusBadge, "setBackgroundResource", R.drawable.badge_past_background)
                eventView.setViewVisibility(R.id.statusBadge, 0) // VISIBLE
                // Reduced opacity for past events
                eventView.setTextColor(R.id.eventTitle, context.resources.getColor(R.color.widget_text_past, null))
                eventView.setTextColor(R.id.eventTime, context.resources.getColor(R.color.widget_text_past, null))
            }
            EventStatus.UPCOMING -> {
                eventView.setInt(R.id.statusBorder, "setBackgroundResource", R.drawable.border_upcoming)
                eventView.setViewVisibility(R.id.statusBadge, 8) // GONE
                // Full opacity for upcoming events
                eventView.setTextColor(R.id.eventTitle, context.resources.getColor(R.color.widget_text_primary, null))
                eventView.setTextColor(R.id.eventTime, context.resources.getColor(R.color.widget_text_secondary, null))
            }
        }

        return eventView
    }

    private fun fetchCalendarEvents(context: Context): List<CalendarEvent> {
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

            val eventsList = mutableListOf<CalendarEvent>()
            for (component in calendar.components) {
                if (component.name == Component.VEVENT) {
                    val event = component as VEvent
                    val summary = event.summary?.value ?: context.getString(R.string.event_no_title)
                    val startDateString = event.startDate?.value
                    val location = event.location?.value

                    var startDate: Date? = null
                    if (startDateString != null) {
                        for (fmt in inputFormats) {
                            try {
                                val parsed = fmt.parse(startDateString)
                                if (parsed != null) {
                                    startDate = parsed
                                    break
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    if (startDate != null) {
                        val calendarEvent = CalendarEvent(
                            title = summary,
                            startTime = startDate,
                            location = location
                        )
                        eventsList.add(calendarEvent)
                    }
                }
            }

            // Sort events by start time
            eventsList.sortBy { it.startTime }

            if (eventsList.isEmpty()) {
                emptyList()
            } else {
                eventsList
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("PwaWidget", "Timeout di rete", e)
            emptyList()
        } catch (e: java.net.UnknownHostException) {
            Log.e("PwaWidget", "Server non raggiungibile", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("PwaWidget", "Errore generico", e)
            emptyList()
        }
    }
}

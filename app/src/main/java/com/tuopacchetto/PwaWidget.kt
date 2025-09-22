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

private fun getIcsEvents(): List<String> {
    val url = URL("https://outlook.office365.com/owa/calendar/c05135b8a3904b118721bb88f16e180c@siaksistemi.com/15296e171a174bd69fe09a8ee790bec09509691657482763908/calendar.ics")
    val connection = url.openConnection()
    connection.connectTimeout = 5000
    connection.readTimeout = 5000
    val inputStream = connection.getInputStream()
    val builder = CalendarBuilder()
    val calendar = builder.build(inputStream)
    val eventsList = mutableListOf<String>()

    // Formato leggibile
    val inputFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.getDefault())
    val outputFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val now = Date()

    for (component in calendar.components) {
        if (component.name == Component.VEVENT) {
            val event = component as VEvent
            val summary = event.summary.value

            // Prendi la data di inizio e trasformala
            val startDateStr = event.startDate.value
            val startDate = inputFormat.parse(startDateStr)

            // Salta eventi già passati
            if (startDate != null && startDate.after(now)) {
                val formattedDate = outputFormat.format(startDate)
                eventsList.add("$formattedDate - $summary")
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
   fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    // Crea la RemoteViews associata al layout del widget
    val views = RemoteViews(context.packageName, R.layout.pwa_widget_layout)

    // Leggi gli eventi direttamente dal file .ics
    val events = getIcsEvents()

    // Pulisci eventuali vecchie viste
    views.removeAllViews(R.id.widget_events)

    // Aggiungi ogni evento come un item del widget
    for (event in events) {
        val eventView = RemoteViews(context.packageName, R.layout.widget_event_item)
        eventView.setTextViewText(R.id.event_title, event)
        views.addView(R.id.widget_events, eventView)
    }

    // Aggiorna il widget sullo schermo
    appWidgetManager.updateAppWidget(appWidgetId, views)
}
}

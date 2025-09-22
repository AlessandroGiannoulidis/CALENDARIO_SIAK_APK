package com.tuopacchetto

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

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
    fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Step 1: Lista di eventi da mostrare
        val events = listOf("Evento 1", "Evento 2", "Evento 3") // QUI metterai i dati reali

        // Step 2: Ottieni il LinearLayout dove inserire gli eventi
        val eventsLayoutId = R.id.widget_events

        // Step 3: Pulire eventuali vecchi eventi
        views.removeAllViews(eventsLayoutId)

        // Step 4: Aggiungere ogni evento
        for (event in events) {
            val eventView = RemoteViews(context.packageName, R.layout.widget_event_item)
            eventView.setTextViewText(R.id.event_title, event)
            views.addView(eventsLayoutId, eventView)
        }

        // Step 5: Aggiornare il widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

}

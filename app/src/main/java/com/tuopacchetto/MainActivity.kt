package com.tuopacchetto

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val titleText = findViewById<TextView>(R.id.titleText)
        val statusText = findViewById<TextView>(R.id.statusText)
        val refreshButton = findViewById<Button>(R.id.refreshButton)
        val addWidgetButton = findViewById<Button>(R.id.addWidgetButton)

        titleText.text = "Widget Calendario SIAK"
        statusText.text = "App installata correttamente. Ora puoi aggiungere il widget alla schermata principale."

        refreshButton.setOnClickListener {
            refreshAllWidgets()
            Toast.makeText(this, "Widget aggiornati", Toast.LENGTH_SHORT).show()
        }

        addWidgetButton.setOnClickListener {
            requestPinWidget()
        }
    }

    private fun refreshAllWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, PwaWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        
        val intent = Intent(this, PwaWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        }
        
        sendBroadcast(intent)
    }

    private fun requestPinWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, PwaWidget::class.java)
        
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(componentName, null, null)
        } else {
            Toast.makeText(
                this, 
                "Aggiungi manualmente il widget: tieni premuto sulla schermata principale > Widget > Trova 'Widget Calendario SIAK'", 
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

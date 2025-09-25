package com.tuopacchetto

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import it.alessandrogiannoulidis.calendariosiak.utils.ToastUtils

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val titleText = findViewById<TextView>(R.id.titleText)
        val statusText = findViewById<TextView>(R.id.statusText)
        val refreshButton = findViewById<Button>(R.id.refreshButton)
        val addWidgetButton = findViewById<Button>(R.id.addWidgetButton)

        titleText.text = getString(R.string.main_title)
        statusText.text = getString(R.string.main_status_text)

        refreshButton.setOnClickListener {
            refreshAllWidgets()
            ToastUtils.showToast(this, R.string.widget_updated)
        }

        addWidgetButton.setOnClickListener {
            requestPinWidget()
            ToastUtils.showToast(this, R.string.manual_widget_instruction, isLong = true)
        }
        
        // Add settings button (if it exists in the layout)
        findViewById<Button>(R.id.settingsButton)?.setOnClickListener {
            val intent = Intent(this, WidgetSettingsActivity::class.java)
            startActivity(intent)
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
            // Mostra istruzioni manuali già gestite sopra con Toast
        }
    }
}

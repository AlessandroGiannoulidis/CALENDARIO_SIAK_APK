package com.tuopacchetto

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
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

        titleText.text = getString(R.string.title_main)
        statusText.text = getString(R.string.status_main)

        refreshButton.setOnClickListener {
            refreshAllWidgets()
            ToastUtils.showToast(this, getString(R.string.widgets_updated))
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

        try {
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                appWidgetManager.requestPinAppWidget(componentName, null, null)
            } else {
                ToastUtils.showToast(
                    this,
                    getString(R.string.add_widget_manually),
                    isLong = true
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            ToastUtils.showToast(
                this,
                getString(R.string.error_message, e.message ?: "Unknown error"),
                isLong = true
            )
        }
    }
}

package com.tuopacchetto

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.*
import android.content.SharedPreferences

class WidgetSettingsActivity : AppCompatActivity() {
    
    private lateinit var preferences: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_settings)
        
        preferences = getSharedPreferences("widget_settings", MODE_PRIVATE)
        
        setupUI()
    }
    
    private fun setupUI() {
        val compactModeSwitch = findViewById<Switch>(R.id.compactModeSwitch)
        val saveButton = findViewById<Button>(R.id.saveButton)
        
        // Load current settings
        val isCompact = preferences.getBoolean("compact_mode", false)
        compactModeSwitch.isChecked = isCompact
        
        saveButton.setOnClickListener {
            val editor = preferences.edit()
            editor.putBoolean("compact_mode", compactModeSwitch.isChecked)
            editor.apply()
            
            // Refresh all widgets
            refreshAllWidgets()
            
            Toast.makeText(this, "Impostazioni salvate", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun refreshAllWidgets() {
        // Same logic as MainActivity
        val intent = android.content.Intent(this, PwaWidget::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        sendBroadcast(intent)
    }
}
package com.tuopacchetto.widget

import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.text.Text
import androidx.glance.unit.dp

class NextEventWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: android.content.Context, id: GlanceId) {
        provideContent { NextEventWidgetContent() }
    }
}

@Composable
private fun NextEventWidgetContent() {
    Column(modifier = GlanceModifier.fillMaxSize().appWidgetBackground()) {
        Text("Prossimo evento")
        Spacer(GlanceModifier.height(8.dp))
        Text("Nessun evento imminente (placeholder)")
    }
}
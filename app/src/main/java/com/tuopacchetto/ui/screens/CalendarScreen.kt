package com.tuopacchetto.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CalendarScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Vista Calendario (placeholder)")
        Spacer(Modifier.height(8.dp))
        Text("Mostra mese/settimana con selezione giorno e indicatori eventi")
    }
}
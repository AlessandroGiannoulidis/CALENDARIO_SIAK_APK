package com.tuopacchetto.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EventDetailScreen(eventId: String?) {
    Column(Modifier.padding(16.dp)) {
        Text("Dettaglio evento $eventId (placeholder)")
        Row(Modifier.padding(top = 16.dp)) {
            Button(onClick = { /* TODO: add to system calendar */ }) { Text("Aggiungi al calendario") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { /* TODO: share */ }) { Text("Condividi") }
        }
    }
}
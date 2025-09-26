package com.tuopacchetto.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tuopacchetto.data.model.Event

@Composable
fun AgendaScreen() {
    val items = listOf(
        Event(id = "1", title = "Lezione Analisi", location = "Aula 101", start = "09:00", end = "11:00", type = "Lezione"),
        Event(id = "2", title = "Laboratorio", location = "Lab A", start = "12:00", end = "13:30", type = "Lab")
    )
    LazyColumn(Modifier.padding(16.dp)) {
        items(items) { e ->
            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(e.title)
                    Spacer(Modifier.height(4.dp))
                    Text("${e.start} - ${e.end} • ${e.location}")
                    Spacer(Modifier.height(2.dp))
                    Text(e.type)
                }
            }
        }
    }
}
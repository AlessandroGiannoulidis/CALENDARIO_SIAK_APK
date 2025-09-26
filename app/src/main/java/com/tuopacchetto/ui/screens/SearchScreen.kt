package com.tuopacchetto.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*;
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SearchScreen() {
    var q by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = q,
            onValueChange = { q = it },
            label = { Text("Cerca corso/docente/aula") },
            modifier = Modifier
                .padding(bottom = 16.dp)
        )
        Text("Risultati (placeholder)")
    }
}
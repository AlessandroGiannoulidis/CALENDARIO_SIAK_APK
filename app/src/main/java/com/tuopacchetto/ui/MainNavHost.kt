package com.tuopacchetto.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tuopacchetto.ui.screens.AgendaScreen
import com.tuopacchetto.ui.screens.CalendarScreen
import com.tuopacchetto.ui.screens.EventDetailScreen
import com.tuopacchetto.ui.screens.SearchScreen
import com.tuopacchetto.ui.screens.SettingsScreen

@Composable
fun MainNavHost() {
    val navController = rememberNavController()
    var current by remember { mutableStateOf("calendar") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Calendario SIAK") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = current == "calendar",
                    onClick = { current = "calendar"; navController.navigate("calendar") },
                    icon = { Icon(Icons.Filled.Event, contentDescription = null) },
                    label = { Text("Calendario") }
                )
                NavigationBarItem(
                    selected = current == "agenda",
                    onClick = { current = "agenda"; navController.navigate("agenda") },
                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
                    label = { Text("Agenda") }
                )
                NavigationBarItem(
                    selected = current == "search",
                    onClick = { current = "search"; navController.navigate("search") },
                    icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text("Ricerca") }
                )
                NavigationBarItem(
                    selected = current == "settings",
                    onClick = { current = "settings"; navController.navigate("settings") },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Impostazioni") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // TODO: comportamento "vai a oggi"
            }) {
                Icon(Icons.Filled.Today, contentDescription = "Vai a oggi")
            }
        }
    ) { _ ->
        NavGraph(navController = navController)
    }
}

@Composable
private fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "calendar") {
        composable("calendar") { CalendarScreen() }
        composable("agenda") { AgendaScreen() }
        composable("search") { SearchScreen() }
        composable("settings") { SettingsScreen() }
        composable("event/{id}") { backStack ->
            val id = backStack.arguments?.getString("id")
            EventDetailScreen(eventId = id)
        }
    }
}
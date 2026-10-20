package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.viewmodel.GameViewModel

@Composable
fun BaseManagementScreen(viewModel: GameViewModel) {
    val buildings by viewModel.getBuildings().collectAsState(initial = emptyList())
    val profile by viewModel.playerProfile.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Base Management", style = MaterialTheme.typography.headlineMedium)
        profile?.let {
            Text("Coins: ${it.coins} | Metal: ${it.metal}")
        }
        LazyColumn {
            items(buildings) { building ->
                Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(building.type, style = MaterialTheme.typography.titleMedium)
                        Text("Level: ${building.level}")
                        Text("Cost: ${building.level * 100} Coins, ${building.level * 50} Metal")
                        Button(onClick = { viewModel.upgradeBuilding(building.id) }) {
                            Text("Upgrade")
                        }
                    }
                }
            }
        }
    }
}

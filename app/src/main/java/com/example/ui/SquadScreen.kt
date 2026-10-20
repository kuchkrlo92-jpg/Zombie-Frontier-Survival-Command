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
fun SquadScreen(viewModel: GameViewModel) {
    val survivors by viewModel.getSurvivors().collectAsState(initial = emptyList())
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Squad Management", style = MaterialTheme.typography.headlineMedium)
        LazyColumn {
            items(survivors) { survivor ->
                Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(survivor.survivorClass, style = MaterialTheme.typography.titleMedium)
                        Text("Level: ${survivor.level} | Health: ${survivor.health} | Damage: ${survivor.damage}")
                        if (!survivor.isRecruited) {
                            Button(onClick = { viewModel.recruitSurvivor(survivor.id) }) { Text("Recruit (500 Coins, 200 Food)") }
                        } else {
                            Button(onClick = { viewModel.upgradeSurvivor(survivor.id) }) { Text("Upgrade (${survivor.level * 200} Coins)") }
                        }
                    }
                }
            }
        }
    }
}

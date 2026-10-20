package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun MainMenuScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ZOMBIE FRONTIER", style = MaterialTheme.typography.headlineLarge)
        Text("SURVIVAL COMMAND", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { navController.navigate("mission_select") }) { Text("PLAY MISSION") }
        Button(onClick = { navController.navigate("base") }) { Text("BASE") }
        Button(onClick = { navController.navigate("squad") }) { Text("SQUAD") }
        Button(onClick = { navController.navigate("armory") }) { Text("ARMORY") }
        Button(onClick = { navController.navigate("missions") }) { Text("MISSIONS") }
        Button(onClick = { navController.navigate("settings") }) { Text("SETTINGS") }
    }
}

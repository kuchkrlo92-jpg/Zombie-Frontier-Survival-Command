package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.navigation.NavController
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.delay

@Composable
fun CombatScreen(navController: NavController, viewModel: GameViewModel) {
    var combatState by remember { mutableStateOf(CombatState()) }

    LaunchedEffect(Unit) {
        while (!combatState.isGameOver && !combatState.isVictory) {
            if (combatState.isPaused) { delay(100); continue }
            delay(16)
            
            val nextPos = combatState.player.position + combatState.joystickInput * 5f
            combatState = combatState.copy(player = combatState.player.copy(position = Offset(nextPos.x.coerceIn(0f, 1000f), nextPos.y.coerceIn(200f, 800f))))
            
            if (combatState.killCount < 15 && (0..100).random() < 5) {
                val rand = (0..100).random()
                val zombie = when {
                    rand < 60 -> Zombie((0..10000).random(), Offset(1200f, (200..800).random().toFloat()), 100, "Walker", 2f, 10, 15f)
                    rand < 90 -> Zombie((0..10000).random(), Offset(1200f, (200..800).random().toFloat()), 50, "Runner", 4f, 5, 10f)
                    else -> Zombie((0..10000).random(), Offset(1200f, (200..800).random().toFloat()), 300, "Brute", 1f, 30, 25f)
                }
                combatState = combatState.copy(zombies = combatState.zombies + zombie)
            }
            
            combatState = combatState.copy(
                zombies = combatState.zombies.map { it.copy(position = it.position.copy(x = it.position.x - it.speed)) },
                projectiles = combatState.projectiles.map { it.copy(position = it.position + it.velocity) }.filter { it.position.x < 1200f },
                effects = combatState.effects.map { it.copy(ttl = it.ttl - 1) }.filter { it.ttl > 0 }
            )
            
            val newZombies = combatState.zombies.toMutableList()
            val newEffects = combatState.effects.toMutableList()
            newZombies.removeAll { zombie ->
                var hit = false
                combatState.projectiles.forEach { if ((zombie.position - it.position).getDistance() < 30f) hit = true }
                if (hit) {
                    zombie.health -= 20
                    newEffects.add(Effect(zombie.position, "Impact"))
                }
                if (zombie.health <= 0) {
                    newEffects.add(Effect(zombie.position, "Death"))
                    true
                } else false
            }
            
            combatState = combatState.copy(
                zombies = newZombies,
                effects = newEffects,
                killCount = combatState.killCount + (combatState.zombies.size - newZombies.size)
            )

            combatState.zombies.forEach { if ((it.position - combatState.player.position).getDistance() < 40f) combatState = combatState.copy(player = combatState.player.copy(health = combatState.player.health - it.damage)) }
            if (combatState.player.health <= 0) combatState = combatState.copy(isGameOver = true)
            if (combatState.killCount >= 15 && combatState.player.position.x > 900f) {
                combatState = combatState.copy(isVictory = true)
                viewModel.completeMission(1, combatState.killCount.toLong())
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures(
                onDrag = { _, dragAmount -> combatState = combatState.copy(joystickInput = dragAmount / 10f) },
                onDragEnd = { combatState = combatState.copy(joystickInput = Offset.Zero) }
            )
        }) {
            drawRect(Color.DarkGray)
            if (combatState.killCount >= 15) drawRect(Color.Green.copy(alpha = 0.3f), topLeft = Offset(900f, 200f), size = androidx.compose.ui.geometry.Size(100f, 600f))
            drawCircle(Color.Blue, radius = 20f, center = combatState.player.position)
            combatState.zombies.forEach { drawCircle(if (it.health < 100) Color.LightGray else Color.Red, radius = it.size, center = it.position) }
            combatState.projectiles.forEach { drawCircle(Color.Yellow, radius = 5f, center = it.position) }
            combatState.effects.forEach { drawCircle(if (it.type == "Death") Color.Magenta else Color.White, radius = 10f, center = it.position) }
        }
        
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Health: ${combatState.player.health}", color = Color.White)
            Text("Zombies: ${combatState.killCount}/15", color = Color.White)
            if (combatState.killCount >= 15) Text("EXTRACTION AVAILABLE!", color = Color.Green)
        }
        
        if (combatState.isGameOver) {
            AlertDialog(onDismissRequest = {}, title = { Text("MISSION FAILED") }, text = { Text("Final Kills: ${combatState.killCount}.") }, confirmButton = { Button(onClick = { combatState = CombatState() }) { Text("RETRY") } }, dismissButton = { Button(onClick = { navController.popBackStack() }) { Text("BASE") } })
        }
        
        if (combatState.isVictory) {
            AlertDialog(onDismissRequest = {}, title = { Text("MISSION COMPLETE") }, text = { Text("Kills: ${combatState.killCount}. Rewards: 250 Coins, 100 Food!") }, confirmButton = { Button(onClick = { combatState = CombatState() }) { Text("RETRY") } }, dismissButton = { Button(onClick = { navController.popBackStack() }) { Text("BASE") } })
        }
        
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.BottomEnd) {
            Button(onClick = { 
                if (!combatState.isGameOver && !combatState.isVictory) {
                    combatState = combatState.copy(projectiles = combatState.projectiles + Projectile(combatState.player.position, Offset(10f, 0f)), effects = combatState.effects + Effect(combatState.player.position, "Flash"))
                }
            }) { Text("FIRE") }
        }
    }
}

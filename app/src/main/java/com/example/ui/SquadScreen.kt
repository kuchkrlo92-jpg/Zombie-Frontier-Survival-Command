package com.example.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.Survivor
import com.example.ui.components.TopResourceBar
import com.example.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquadScreen(
    navController: NavController,
    viewModel: GameViewModel
) {
    val survivors by viewModel.getSurvivors().collectAsState(initial = emptyList())
    val profile by viewModel.playerProfile.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            actionMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SQUAD COMMAND // SURVIVORS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFFFFB300)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF14171A)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0F1215)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TopResourceBar(profile = profile)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(survivors) { survivor ->
                    SurvivorCard(
                        survivor = survivor,
                        playerCoins = profile?.coins ?: 0L,
                        playerFood = profile?.food ?: 0L,
                        onRecruit = {
                            if ((profile?.coins ?: 0L) >= 500L && (profile?.food ?: 0L) >= 200L) {
                                viewModel.recruitSurvivor(survivor.id)
                                actionMessage = "Recruited ${survivor.survivorClass} into squad!"
                            } else {
                                actionMessage = "Insufficient Coins or Food to recruit!"
                            }
                        },
                        onUpgrade = {
                            val cost = survivor.level * 200L
                            if ((profile?.coins ?: 0L) >= cost) {
                                viewModel.upgradeSurvivor(survivor.id)
                                actionMessage = "Upgraded ${survivor.survivorClass} to LVL ${survivor.level + 1}!"
                            } else {
                                actionMessage = "Insufficient Coins for squad upgrade!"
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SurvivorCard(
    survivor: Survivor,
    playerCoins: Long,
    playerFood: Long,
    onRecruit: () -> Unit,
    onUpgrade: () -> Unit
) {
    val upgradeCost = survivor.level * 200L
    val canAffordUpgrade = playerCoins >= upgradeCost
    val canAffordRecruit = playerCoins >= 500L && playerFood >= 200L

    val roleDescription = when (survivor.survivorClass) {
        "Assault Soldier" -> "Balanced infantry combatant specializing in mid-range suppressive fire."
        "Heavy Gunner" -> "High armor frontline juggernaut with elevated health reserves and high-caliber punch."
        "Medic" -> "Field combat physician providing squad resuscitation and emergency field bandaging."
        "Scout" -> "Rapid reconnaissance operative with quick agility and elevated critical-strike probability."
        else -> "Tactical squad specialist."
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191E24)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (survivor.isRecruited) Color(0xFF2E7D32) else Color(0xFF2C353F),
                RoundedCornerShape(8.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = survivor.survivorClass.uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (survivor.isRecruited) "ACTIVE OPERATIVE" else "AVAILABLE RECRUIT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (survivor.isRecruited) Color(0xFF81C784) else Color(0xFFFFB300)
                    )
                }

                Surface(
                    color = if (survivor.isRecruited) Color(0xFF1B5E20) else Color(0xFF263238),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "LVL ${survivor.level}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = roleDescription,
                fontSize = 12.sp,
                color = Color(0xFF90A4AE),
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(10.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("❤️ HP:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF5350))
                    Spacer(Modifier.width(4.dp))
                    Text("${survivor.health}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚔️ DMG:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                    Spacer(Modifier.width(4.dp))
                    Text("${survivor.damage}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!survivor.isRecruited) {
                Button(
                    onClick = onRecruit,
                    enabled = canAffordRecruit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF8F00),
                        disabledContainerColor = Color(0xFF3E2723)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (canAffordRecruit) Color.Black else Color(0xFF8D6E63))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "RECRUIT (500🪙 + 200🍞)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (canAffordRecruit) Color.Black else Color(0xFF8D6E63)
                    )
                }
            } else {
                Button(
                    onClick = onUpgrade,
                    enabled = canAffordUpgrade,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0277BD),
                        disabledContainerColor = Color(0xFF1C2733)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Upgrade, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "UPGRADE TO LVL ${survivor.level + 1} (${upgradeCost}🪙)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (canAffordUpgrade) Color.White else Color(0xFF546E7A)
                    )
                }
            }
        }
    }
}

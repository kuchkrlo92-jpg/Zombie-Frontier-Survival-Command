package com.example.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.BaseBuilding
import com.example.ui.components.TopResourceBar
import com.example.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseManagementScreen(
    navController: NavController,
    viewModel: GameViewModel
) {
    val buildings by viewModel.getBuildings().collectAsState(initial = emptyList())
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
                        "SURVIVOR BASE // INFRASTRUCTURE",
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
                items(buildings) { building ->
                    BuildingCard(
                        building = building,
                        playerCoins = profile?.coins ?: 0L,
                        playerMetal = profile?.metal ?: 0L,
                        onUpgrade = {
                            val costCoins = building.level * 100L
                            val costMetal = building.level * 50L
                            if ((profile?.coins ?: 0L) >= costCoins && (profile?.metal ?: 0L) >= costMetal) {
                                viewModel.upgradeBuilding(building.id)
                                actionMessage = "Upgraded ${building.type} to LVL ${building.level + 1}!"
                            } else {
                                actionMessage = "Insufficient resources to upgrade ${building.type}!"
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BuildingCard(
    building: BaseBuilding,
    playerCoins: Long,
    playerMetal: Long,
    onUpgrade: () -> Unit
) {
    val costCoins = building.level * 100L
    val costMetal = building.level * 50L
    val canAfford = playerCoins >= costCoins && playerMetal >= costMetal

    val benefit = when (building.id) {
        1 -> "Command Center: Increases structural durability and overall base tier."
        2 -> "Resource Generator: Generates +${building.level * 50} coins & metal reserves per operational cycle."
        3 -> "Barracks: Expands maximum survivor squad combat capacity to ${building.level + 2} slots."
        4 -> "Weapon Workshop: Enhances weapon crafting efficiency and recoil control."
        5 -> "Medical Station: Boosts squad health restoration and field revival rate."
        else -> "Facility expansion."
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191E24)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2C353F), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = building.type.uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Surface(
                    color = Color(0xFF2E7D32),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "LVL ${building.level}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = benefit,
                fontSize = 12.sp,
                color = Color(0xFF90A4AE),
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("UPGRADE COST:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78909C))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$costCoins🪙",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (playerCoins >= costCoins) Color(0xFFFFB300) else Color(0xFFE53935)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "$costMetal🔩",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (playerMetal >= costMetal) Color(0xFF90A4AE) else Color(0xFFE53935)
                        )
                    }
                }

                Button(
                    onClick = onUpgrade,
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0277BD),
                        disabledContainerColor = Color(0xFF1C2733)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Upgrade, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "UPGRADE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (canAfford) Color.White else Color(0xFF546E7A)
                    )
                }
            }
        }
    }
}

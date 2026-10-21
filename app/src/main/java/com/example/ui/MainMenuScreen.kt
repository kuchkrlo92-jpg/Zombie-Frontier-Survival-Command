package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.components.TopResourceBar
import com.example.viewmodel.GameViewModel

@Composable
fun MainMenuScreen(
    navController: NavController,
    viewModel: GameViewModel
) {
    val profile by viewModel.playerProfile.collectAsState()
    val equippedWeaponData by viewModel.equippedWeapon.collectAsState()
    val (weaponDef, weaponLevel) = equippedWeaponData
    val missionsList by viewModel.missions.collectAsState()
    val completedCount = missionsList.count { it.isCompleted && it.missionId in 1..4 }

    Scaffold(
        containerColor = Color(0xFF0F1215)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            TopResourceBar(profile = profile)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(10.dp))

                // Tactical Alert Tag
                Surface(
                    color = Color(0xFF33200F),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(1.dp, Color(0xFFFF9800), RoundedCornerShape(4.dp))
                ) {
                    Text(
                        text = "ALERT LEVEL: CRITICAL // OUTBREAK ACTIVE",
                        color = Color(0xFFFFB300),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        letterSpacing = 1.sp
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "ZOMBIE FRONTIER",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "SURVIVAL COMMAND",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF8F00),
                    letterSpacing = 3.sp
                )

                Spacer(Modifier.height(20.dp))

                // Status Card
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF181C21)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF283038), RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("COMMANDER STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90A4AE))
                            Text("HQ Active • Sector 4", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("CAMPAIGN PROGRESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90A4AE))
                            Text("$completedCount/4 Missions Cleared", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // PRIMARY DEPLOY BUTTON
                Button(
                    onClick = { navController.navigate("missions") },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "DEPLOY OPERATION // MISSIONS",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color.Black,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(Modifier.height(18.dp))

                // 2x2 Tactical Grid Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TacticalNavCard(
                        icon = Icons.Default.Shield,
                        title = "SURVIVOR BASE",
                        subtitle = "Facility upgrades",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("base") }
                    )
                    TacticalNavCard(
                        icon = Icons.Default.Groups,
                        title = "SQUAD ROSTER",
                        subtitle = "Recruit soldiers",
                        color = Color(0xFF29B6F6),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("squad") }
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TacticalNavCard(
                        icon = Icons.Default.MilitaryTech,
                        title = "WEAPON ARMORY",
                        subtitle = "${weaponDef.codeName} LVL $weaponLevel",
                        color = Color(weaponDef.colorHex),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("armory") }
                    )
                    TacticalNavCard(
                        icon = Icons.Default.Settings,
                        title = "SETTINGS",
                        subtitle = "Audio & sensitivity",
                        color = Color(0xFFB0BEC5),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("settings") }
                    )
                }
            }
        }
    }
}

@Composable
private fun TacticalNavCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191E24)),
        modifier = modifier
            .height(96.dp)
            .border(1.dp, Color(0xFF2D353F), RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                Text(subtitle, fontSize = 10.sp, color = Color(0xFF90A4AE), maxLines = 1)
            }
        }
    }
}

package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.MissionProgress
import com.example.ui.components.TopResourceBar
import com.example.viewmodel.GameViewModel

data class MissionInfo(
    val id: Int,
    val title: String,
    val subtitle: String,
    val description: String,
    val objective: String,
    val difficulty: String,
    val difficultyColor: Color,
    val coinReward: Long,
    val foodReward: Long,
    val unlockRequirement: String
)

val MISSION_CATALOG = listOf(
    MissionInfo(
        id = 1,
        title = "Abandoned Highway",
        subtitle = "ZONE A // HIGHWAY 101",
        description = "A collapsed transport corridor choked with burning wreckage and infested with fast-moving infected hordes.",
        objective = "Eliminate 15 zombies and reach the Extraction Gate.",
        difficulty = "NORMAL",
        difficultyColor = Color(0xFF4CAF50),
        coinReward = 250L,
        foodReward = 100L,
        unlockRequirement = "Available from Start"
    ),
    MissionInfo(
        id = 2,
        title = "Ruined City",
        subtitle = "ZONE B // METROPOLIS RUINS",
        description = "Crumbling urban high-rises and dark alleyways. Higher zombie density with armored Brute encounters.",
        objective = "Eliminate 25 zombies and reach the Safehouse Helipad.",
        difficulty = "HARD",
        difficultyColor = Color(0xFFFF9800),
        coinReward = 500L,
        foodReward = 200L,
        unlockRequirement = "Complete Mission 1 (Abandoned Highway)"
    ),
    MissionInfo(
        id = 3,
        title = "Military Outpost",
        subtitle = "ZONE C // FORWARD BASE BRAVO",
        description = "An overrun military defense checkpoint. Heavy swarms of Runners and massive Brutes assault the perimeter.",
        objective = "Eliminate 35 zombies and extract via armored transport.",
        difficulty = "NIGHTMARE",
        difficultyColor = Color(0xFFE53935),
        coinReward = 1000L,
        foodReward = 400L,
        unlockRequirement = "Complete Mission 2 (Ruined City)"
    ),
    MissionInfo(
        id = 4,
        title = "Containment Breach",
        subtitle = "ZONE D // BIO-HAZARD LABS",
        description = "Underground research facility overrun by an apex mutant. Slay THE ABOMINATION to prevent citywide infection.",
        objective = "Eliminate the Boss: THE ABOMINATION.",
        difficulty = "BOSS RAID",
        difficultyColor = Color(0xFFE040FB),
        coinReward = 1500L,
        foodReward = 600L,
        unlockRequirement = "Complete Mission 3 (Military Outpost)"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsScreen(
    navController: NavController,
    viewModel: GameViewModel
) {
    val profile by viewModel.playerProfile.collectAsState()
    val missionsList by viewModel.missions.collectAsState()
    val survivalRecord = missionsList.find { it.missionId == 100 }
        ?: MissionProgress(100, isUnlocked = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "TACTICAL OPERATIONS // MISSIONS",
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // SPECIAL OPERATION: WAVE SURVIVAL CARD
                item {
                    SurvivalModeCard(
                        survivalRecord = survivalRecord,
                        onPlay = { navController.navigate("combat/100") }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🛡️", fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "CAMPAIGN MISSIONS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB300),
                            letterSpacing = 1.sp
                        )
                    }
                }

                items(MISSION_CATALOG) { info ->
                    val progress = missionsList.find { it.missionId == info.id }
                        ?: MissionProgress(info.id, isUnlocked = (info.id == 1))

                    MissionCard(
                        info = info,
                        progress = progress,
                        onPlay = {
                            navController.navigate("combat/${info.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SurvivalModeCard(
    survivalRecord: MissionProgress,
    onPlay: () -> Unit
) {
    val bestWave = survivalRecord.bestScore.toInt()

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1824)),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color(0xFF9C27B0), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💀", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "SPECIAL OPERATION: WAVE SURVIVAL",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "ENDLESS SIEGE // 5 ESCALATING WAVES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCE93D8)
                        )
                    }
                }

                Surface(
                    color = Color(0x33E040FB),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(1.dp, Color(0xFFE040FB), RoundedCornerShape(4.dp))
                ) {
                    Text(
                        text = "ENDLESS",
                        color = Color(0xFFE040FB),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Hold the perimeter against relentless zombie swarms with escalating difficulty. Defeat Walkers, Runners, and Brutes across 5 waves, culminating in a climactic showdown against THE ABOMINATION!",
                fontSize = 12.sp,
                color = Color(0xFFD1C4E9),
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(10.dp))

            // Objective and Best Wave Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF120E18), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("REWARDS PER WAVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90A4AE))
                    Text("Up to +2,900🪙  +1,150🍞", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                }
                Surface(
                    color = Color(0xFF2E1938),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (bestWave > 0) "BEST: WAVE $bestWave/5" else "NOT ATTEMPTED",
                        color = if (bestWave >= 5) Color(0xFF4CAF50) else Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAB47BC)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "DEPLOY SURVIVAL MODE",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun MissionCard(
    info: MissionInfo,
    progress: MissionProgress,
    onPlay: () -> Unit
) {
    val isCompleted = progress.isCompleted
    val isUnlocked = progress.isUnlocked

    val borderColor = when {
        isCompleted -> Color(0xFF2E7D32)
        isUnlocked -> Color(0xFFFFB300)
        else -> Color(0xFF2A313A)
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191D22)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Mission Title + Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MISSION 0${info.id}: ${info.title.uppercase()}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = info.subtitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF78909C)
                    )
                }

                // Difficulty badge
                Surface(
                    color = info.difficultyColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(1.dp, info.difficultyColor, RoundedCornerShape(4.dp))
                ) {
                    Text(
                        text = info.difficulty,
                        color = info.difficultyColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = info.description,
                fontSize = 12.sp,
                color = Color(0xFFB0BEC5),
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(8.dp))

            // Objective Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121519), RoundedCornerShape(4.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎯", fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("PRIMARY OBJECTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90A4AE))
                    Text(info.objective, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Rewards & Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("REWARDS:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78909C))
                    Spacer(Modifier.width(6.dp))
                    Text("+${info.coinReward}🪙", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                    Spacer(Modifier.width(6.dp))
                    Text("+${info.foodReward}🍞", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                }

                if (progress.bestScore > 0) {
                    Text("BEST: ${progress.bestScore} KILLS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4DD0E1))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Launch Button or Locked Status
            if (isUnlocked) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) Color(0xFF2E7D32) else Color(0xFFFF8F00)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isCompleted) Color.White else Color.Black
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isCompleted) "REPLAY MISSION" else "DEPLOY SQUAD // LAUNCH",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isCompleted) Color.White else Color.Black
                    )
                }
            } else {
                Surface(
                    color = Color(0xFF21262D),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF30363D), RoundedCornerShape(6.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "LOCKED — ${info.unlockRequirement}",
                            color = Color(0xFFE0E0E0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

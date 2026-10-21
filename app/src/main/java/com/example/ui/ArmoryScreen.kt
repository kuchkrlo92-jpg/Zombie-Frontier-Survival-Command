package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
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
import com.example.data.WeaponCatalog
import com.example.data.WeaponDef
import com.example.data.WeaponProgress
import com.example.ui.components.TopResourceBar
import com.example.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArmoryScreen(
    navController: NavController,
    viewModel: GameViewModel
) {
    val profile by viewModel.playerProfile.collectAsState()
    val weaponsProgress by viewModel.weaponProgress.collectAsState()
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
                        "ARMORY // WEAPON SYSTEMS",
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
                items(WeaponCatalog.WEAPONS) { def ->
                    val progress = weaponsProgress.find { it.weaponId == def.id }
                        ?: WeaponProgress(def.id, 1, isUnlocked = (def.id == 1), isEquipped = (def.id == 1))

                    WeaponCard(
                        weapon = def,
                        progress = progress,
                        playerCoins = profile?.coins ?: 0L,
                        onUnlock = {
                            if ((profile?.coins ?: 0L) >= def.unlockCost) {
                                viewModel.unlockWeapon(def.id)
                                actionMessage = "Unlocked ${def.name}!"
                            } else {
                                actionMessage = "Insufficient Coins! Need ${def.unlockCost} Coins."
                            }
                        },
                        onUpgrade = {
                            val cost = def.calculateUpgradeCost(progress.level)
                            if ((profile?.coins ?: 0L) >= cost) {
                                viewModel.upgradeWeapon(def.id)
                                actionMessage = "Upgraded ${def.name} to LVL ${progress.level + 1}!"
                            } else {
                                actionMessage = "Insufficient Coins for upgrade!"
                            }
                        },
                        onEquip = {
                            viewModel.equipWeapon(def.id)
                            actionMessage = "Equipped ${def.name} as primary weapon!"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeaponCard(
    weapon: WeaponDef,
    progress: WeaponProgress,
    playerCoins: Long,
    onUnlock: () -> Unit,
    onUpgrade: () -> Unit,
    onEquip: () -> Unit
) {
    val currentDamage = weapon.calculateDamage(progress.level)
    val nextDamage = weapon.calculateDamage(progress.level + 1)
    val upgradeCost = weapon.calculateUpgradeCost(progress.level)
    val isEquipped = progress.isEquipped
    val isUnlocked = progress.isUnlocked

    val cardBorder = when {
        isEquipped -> Color(0xFF4CAF50)
        isUnlocked -> Color(0xFF3B4854)
        else -> Color(0xFF262D34)
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191D22)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, cardBorder, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = weapon.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${weapon.codeName} • ${weapon.type}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(weapon.colorHex)
                    )
                }

                // Status Badge
                when {
                    isEquipped -> {
                        Surface(
                            color = Color(0xFF1B5E20),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("EQUIPPED", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    isUnlocked -> {
                        Surface(
                            color = Color(0xFF263238),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.border(1.dp, Color(0xFF546E7A), RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                "OWNED",
                                color = Color(0xFFB0BEC5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    else -> {
                        Surface(
                            color = Color(0xFF371E20),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.border(1.dp, Color(0xFFD32F2F), RoundedCornerShape(4.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("LOCKED", color = Color(0xFFEF5350), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = weapon.description,
                fontSize = 12.sp,
                color = Color(0xFF90A4AE),
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(10.dp))

            // Stat Bars
            StatBar(label = "DAMAGE", value = "$currentDamage", nextValue = if (isUnlocked) "+${nextDamage - currentDamage}" else null, ratio = (currentDamage / 150f).coerceIn(0.1f, 1f), color = Color(0xFFEF5350))
            Spacer(Modifier.height(4.dp))
            StatBar(label = "FIRE RATE", value = "${1000 / weapon.fireRateMs}/sec", nextValue = null, ratio = (600f / weapon.fireRateMs).coerceIn(0.1f, 1f), color = Color(0xFFFFB300))
            Spacer(Modifier.height(4.dp))
            StatBar(label = "RANGE", value = "${weapon.range.toInt()}m", nextValue = null, ratio = (weapon.range / 1000f).coerceIn(0.1f, 1f), color = Color(0xFF42A5F5))

            Spacer(Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isUnlocked) {
                    val canAfford = playerCoins >= weapon.unlockCost
                    Button(
                        onClick = onUnlock,
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF8F00),
                            disabledContainerColor = Color(0xFF3E2723)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "UNLOCK FOR ${weapon.unlockCost} COINS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (canAfford) Color.Black else Color(0xFF8D6E63)
                        )
                    }
                } else {
                    // Equip button
                    Button(
                        onClick = onEquip,
                        enabled = !isEquipped,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            disabledContainerColor = Color(0xFF1B2E1D)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isEquipped) "IN USE" else "EQUIP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isEquipped) Color(0xFF81C784) else Color.White
                        )
                    }

                    // Upgrade button
                    val canAffordUpgrade = playerCoins >= upgradeCost
                    Button(
                        onClick = onUpgrade,
                        enabled = canAffordUpgrade,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0277BD),
                            disabledContainerColor = Color(0xFF1A2A38)
                        ),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.Upgrade, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "LVL ${progress.level + 1} (${upgradeCost}🪙)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (canAffordUpgrade) Color.White else Color(0xFF546E7A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBar(
    label: String,
    value: String,
    nextValue: String?,
    ratio: Float,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF78909C),
            modifier = Modifier.width(68.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .background(Color(0xFF263238), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(ratio)
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (nextValue != null) {
                Text(
                    text = " ($nextValue)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF81C784)
                )
            }
        }
    }
}

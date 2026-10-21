package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.GameSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var soundEnabled by remember { mutableStateOf(GameSettings.isSoundEnabled(context)) }
    var musicEnabled by remember { mutableStateOf(GameSettings.isMusicEnabled(context)) }
    var vibrationEnabled by remember { mutableStateOf(GameSettings.isVibrationEnabled(context)) }
    var graphicsQuality by remember { mutableStateOf(GameSettings.getGraphicsQuality(context)) }
    var damageNumbersEnabled by remember { mutableStateOf(GameSettings.isShowDamageNumbers(context)) }
    var joystickSensitivity by remember { mutableFloatStateOf(GameSettings.getJoystickSensitivity(context)) }
    var difficulty by remember { mutableStateOf(GameSettings.getDifficulty(context)) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SYSTEM COMMAND // SETTINGS",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Audio Section
            SectionHeader(icon = "🔊", title = "AUDIO & FEEDBACK")
            SettingCard {
                SettingSwitchRow(
                    title = "Sound Effects",
                    subtitle = "Gunfire, zombie audio, impact cues, and reload sounds",
                    checked = soundEnabled,
                    onCheckedChange = {
                        soundEnabled = it
                        GameSettings.setSoundEnabled(context, it)
                    }
                )
                Divider(color = Color(0xFF263238), thickness = 0.5.dp)
                SettingSwitchRow(
                    title = "Background Music",
                    subtitle = "Ambient apocalyptic soundtrack during menus and tactical gameplay",
                    checked = musicEnabled,
                    onCheckedChange = {
                        musicEnabled = it
                        GameSettings.setMusicEnabled(context, it)
                    }
                )
                Divider(color = Color(0xFF263238), thickness = 0.5.dp)
                SettingSwitchRow(
                    title = "Haptic Vibration",
                    subtitle = "Vibrate on weapon recoil, player damage, and zombie strikes",
                    checked = vibrationEnabled,
                    onCheckedChange = {
                        vibrationEnabled = it
                        GameSettings.setVibrationEnabled(context, it)
                        if (it) GameSettings.triggerVibration(context, 50)
                    }
                )
            }

            // Visuals Section
            SectionHeader(icon = "👁️", title = "GRAPHICS & COMBAT DISPLAY")
            SettingCard {
                // Graphics Quality Selector
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Graphics Quality", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Text("Controls particle counts, muzzle blast glow, and environmental wreckage detail", fontSize = 11.sp, color = Color(0xFF90A4AE))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("LOW", "MEDIUM", "HIGH").forEach { q ->
                            val isSelected = graphicsQuality == q
                            OutlinedButton(
                                onClick = {
                                    graphicsQuality = q
                                    GameSettings.setGraphicsQuality(context, q)
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) Color(0xFF2E7D32) else Color(0xFF14191E)
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    width = 1.dp,
                                    brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) Color(0xFF4CAF50) else Color(0xFF37474F))
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    q,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFFB0BEC5)
                                )
                            }
                        }
                    }
                }

                Divider(color = Color(0xFF263238), thickness = 0.5.dp)

                SettingSwitchRow(
                    title = "Floating Damage Numbers",
                    subtitle = "Display animated floating damage indicators over zombies when struck",
                    checked = damageNumbersEnabled,
                    onCheckedChange = {
                        damageNumbersEnabled = it
                        GameSettings.setShowDamageNumbers(context, it)
                    }
                )
            }

            // Controls Section
            SectionHeader(icon = "🕹️", title = "TACTICAL CONTROLS")
            SettingCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Joystick Sensitivity", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        Text(
                            "${String.format("%.1f", joystickSensitivity)}x",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFFFFB300)
                        )
                    }
                    Text("Adjust squad movement responsiveness and drag speed", fontSize = 11.sp, color = Color(0xFF90A4AE))
                    Spacer(Modifier.height(4.dp))
                    Slider(
                        value = joystickSensitivity,
                        onValueChange = {
                            joystickSensitivity = it
                            GameSettings.setJoystickSensitivity(context, it)
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFB300),
                            activeTrackColor = Color(0xFFFF8F00),
                            inactiveTrackColor = Color(0xFF37474F)
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Game Difficulty", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val diffs = listOf("EASY", "NORMAL", "HARD")
                        diffs.forEach { d ->
                            Button(
                                onClick = {
                                    difficulty = d
                                    GameSettings.setDifficulty(context, d)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (difficulty == d) Color(0xFFFFB300) else Color(0xFF263238)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(d, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Maintenance / Reset
            SectionHeader(icon = "⚙️", title = "PREFERENCES MAINTENANCE")
            Button(
                onClick = { showRestoreDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFFFB300))
                Spacer(Modifier.width(8.dp))
                Text("RESTORE DEFAULT SETTINGS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            }

            // About Box
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14181C)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF263238), RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF4DD0E1), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ZOMBIE FRONTIER: SURVIVAL COMMAND", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Build: v1.0.0-Release • Engine: Jetpack Compose + Native Canvas • Local Storage: Android Room DB",
                        fontSize = 11.sp,
                        color = Color(0xFF78909C)
                    )
                }
            }
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Default Settings?", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "This will restore all audio, graphics, control, and display preferences back to default values. Your coins, building levels, survivors, and weapon upgrades will NOT be deleted.",
                    color = Color(0xFFB0BEC5),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        GameSettings.restoreDefaults(context)
                        soundEnabled = GameSettings.isSoundEnabled(context)
                        musicEnabled = GameSettings.isMusicEnabled(context)
                        vibrationEnabled = GameSettings.isVibrationEnabled(context)
                        graphicsQuality = GameSettings.getGraphicsQuality(context)
                        damageNumbersEnabled = GameSettings.isShowDamageNumbers(context)
                        joystickSensitivity = GameSettings.getJoystickSensitivity(context)
                        showRestoreDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("RESTORE DEFAULTS", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("CANCEL", color = Color(0xFF90A4AE))
                }
            },
            containerColor = Color(0xFF1A1F24),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun SectionHeader(icon: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFB300),
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191D22)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2C3238), RoundedCornerShape(8.dp)),
        content = content
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF90A4AE), lineHeight = 15.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2E7D32),
                uncheckedThumbColor = Color(0xFF78909C),
                uncheckedTrackColor = Color(0xFF263238)
            )
        )
    }
}

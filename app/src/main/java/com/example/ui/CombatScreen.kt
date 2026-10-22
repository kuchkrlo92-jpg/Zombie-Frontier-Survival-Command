package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import com.example.data.GameSettings
import com.example.data.WeaponCatalog
import com.example.data.WeaponDef
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

@Composable
fun CombatScreen(
    navController: NavController,
    viewModel: GameViewModel,
    missionId: Int = 1
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        SoundFx.init(context)
    }

    val equippedWeaponData by viewModel.equippedWeapon.collectAsState()
    val (weaponDef, weaponLevel) = equippedWeaponData
    val missionsList by viewModel.missions.collectAsState()
    val survivalRecord = missionsList.find { it.missionId == 100 }
    val bestSavedWave = survivalRecord?.bestScore?.toInt() ?: 0

    // Settings
    val showDamageNumbers = remember { GameSettings.isShowDamageNumbers(context) }
    val graphicsQuality = remember { GameSettings.getGraphicsQuality(context) }
    val isSurvivalMode = (missionId == 100)
    val isBossMission = (missionId == 4)

    val missionTitle = when (missionId) {
        1 -> "Abandoned Highway"
        2 -> "Ruined City"
        3 -> "Military Outpost"
        4 -> "Containment Breach"
        100 -> "Endless Siege"
        else -> "Abandoned Highway"
    }

    val targetDistance = when (missionId) {
        1 -> 500f
        2 -> 700f
        3 -> 900f
        4 -> 750f
        else -> 500f
    }

    val targetKills = when (missionId) {
        1 -> 20
        2 -> 35
        3 -> 50
        4 -> 1 // Defeat The Abomination
        else -> 20
    }

    val (rewardCoins, rewardFood) = when (missionId) {
        1 -> Pair(250L, 100L)
        2 -> Pair(500L, 200L)
        3 -> Pair(1000L, 400L)
        4 -> Pair(1500L, 600L)
        else -> Pair(250L, 100L)
    }

    val magazineCapacity = when (weaponDef.id) {
        1 -> 30 // M4A1 Sentinel
        2 -> 45 // Vector Venom
        3 -> 8  // Breacher 12G
        4 -> 5  // Barrett Reaper
        else -> 30
    }

    val reloadDurationMs = when (weaponDef.id) {
        1 -> 1200L
        2 -> 950L
        3 -> 1600L
        4 -> 1900L
        else -> 1200L
    }

    fun getWaveTotalEnemies(wave: Int): Int = when (wave) {
        1 -> 15
        2 -> 25
        3 -> 35
        4 -> 45
        5 -> 8 // Boss (The Abomination) + 7 escorts
        else -> 15 + wave * 8
    }

    fun calculateSurvivalRewards(fromWave: Int, toWave: Int): Pair<Long, Long> {
        var c = 0L
        var f = 0L
        if (toWave > fromWave) {
            for (w in (fromWave + 1)..toWave) {
                val (wc, wf) = when (w) {
                    1 -> Pair(150L, 50L)
                    2 -> Pair(300L, 100L)
                    3 -> Pair(500L, 200L)
                    4 -> Pair(750L, 300L)
                    5 -> Pair(1200L, 500L)
                    else -> Pair(200L, 50L)
                }
                c += wc
                f += wf
            }
        }
        return Pair(c, f)
    }

    // Pre-generate balanced tactical gates along the highway
    fun generateTacticalGates(totalDist: Float): List<TacticalGate> {
        val gates = mutableListOf<TacticalGate>()
        var gateId = 1
        var dist = 80f
        while (dist < totalDist - 50f) {
            when (gateId % 5) {
                1 -> {
                    // Left: +5 Survivors vs Right: DMG +25%
                    gates.add(TacticalGate(gateId++, 0, dist, GateType.RECRUIT_SURVIVORS, "+5 SURVIVORS", 5, true))
                    gates.add(TacticalGate(gateId++, 2, dist, GateType.DAMAGE_BOOST, "DMG +25%", 25, true))
                }
                2 -> {
                    // Left: +30 Ammo vs Center: +10 Riflemen vs Right: Ammo Leak
                    gates.add(TacticalGate(gateId++, 0, dist, GateType.AMMO_CACHE, "+30 AMMO", 30, true))
                    gates.add(TacticalGate(gateId++, 1, dist, GateType.RECRUIT_RIFLEMEN, "+10 RIFLEMEN", 10, true))
                    gates.add(TacticalGate(gateId++, 2, dist, GateType.AMMO_LEAK, "-15 AMMO", 15, false))
                }
                3 -> {
                    // Left: +200 Metal vs Right: Health +40
                    gates.add(TacticalGate(gateId++, 0, dist, GateType.SUPPLY_METAL, "+200 METAL", 200, true))
                    gates.add(TacticalGate(gateId++, 2, dist, GateType.FIELD_MEDIC, "HEALTH +40", 40, true))
                }
                4 -> {
                    // Center: Fire Rate +20% vs Right: +300 Coins
                    gates.add(TacticalGate(gateId++, 1, dist, GateType.FIRE_RATE_BOOST, "RATE +20%", 20, true))
                    gates.add(TacticalGate(gateId++, 2, dist, GateType.SUPPLY_COINS, "+300 COINS", 300, true))
                }
                else -> {
                    // Left: Ambush vs Right: +5 Survivors
                    gates.add(TacticalGate(gateId++, 0, dist, GateType.ZOMBIE_AMBUSH, "AMBUSH!", 0, false))
                    gates.add(TacticalGate(gateId++, 2, dist, GateType.RECRUIT_SURVIVORS, "+5 SURVIVORS", 5, true))
                }
            }
            dist += 110f
        }
        return gates
    }

    // Pre-generate road obstacles
    fun generateObstacles(totalDist: Float): List<LaneObstacle> {
        val obstacles = mutableListOf<LaneObstacle>()
        var obsId = 1
        var dist = 50f
        while (dist < totalDist - 30f) {
            val lane = (obsId * 7) % 3
            val type = when (obsId % 4) {
                0 -> LaneObstacleType.WRECKED_CAR
                1 -> LaneObstacleType.BURNING_BARRIER
                2 -> LaneObstacleType.TOXIC_BARRELS
                else -> LaneObstacleType.CONCRETE_BLOCK
            }
            obstacles.add(LaneObstacle(obsId++, lane, dist, type, type.name))
            dist += 75f
        }
        return obstacles
    }

    fun createInitialState(): CombatState {
        val gates = generateTacticalGates(if (isSurvivalMode) 3000f else targetDistance)
        val obstacles = generateObstacles(if (isSurvivalMode) 3000f else targetDistance)

        val initialBoss = if (isBossMission || isSurvivalMode || missionId == 3) {
            val type = when {
                isSurvivalMode -> BossType.PLAGUE_QUEEN
                missionId == 3 -> BossType.IRON_TYRANT
                else -> BossType.ABOMINATION
            }
            BossState(
                name = when(type) { BossType.IRON_TYRANT -> "THE IRON TYRANT"; BossType.PLAGUE_QUEEN -> "THE PLAGUE QUEEN"; else -> "THE ABOMINATION" },
                bossType = type,
                health = 1500,
                maxHealth = 1500,
                distanceZ = 140f,
                lane = 1,
                targetLane = 1,
                telegraphLane = 1,
                visualLaneX = 1f
            )
        } else null

        val selectedDifficulty = GameSettings.getDifficulty(context)
        val initialSpeedMult = when (selectedDifficulty) { "EASY" -> 0.8f; "HARD" -> 1.25f; else -> 1.0f }
        val initialDamageMult = when (selectedDifficulty) { "EASY" -> 0.7f; "HARD" -> 1.35f; else -> 1.0f }

        // Initial pack of oncoming zombies
        val initialZombies = mutableListOf<Zombie>()
        var zId = 1
        for (i in 0 until 8) {
            val zLane = (i % 3)
            val zDist = 120f + i * 25f
            val zType = if (i % 3 == 0) "Runner" else "Walker"
            val hp = if (zType == "Runner") 30 else 50
            val speed = (if (zType == "Runner") 2.4f else 1.2f) * initialSpeedMult
            val damage = ((if (zType == "Runner") 12 else 10) * initialDamageMult).toInt()
            initialZombies.add(
                Zombie(
                    id = zId++,
                    lane = zLane,
                    distanceZ = zDist,
                    visualLaneX = zLane.toFloat(),
                    health = hp,
                    maxHealth = hp,
                    type = zType,
                    speed = speed,
                    damage = damage
                )
            )
        }

        return CombatState(
            missionId = missionId,
            targetKills = targetKills,
            missionTitle = missionTitle,
            weaponDef = weaponDef,
            weaponLevel = weaponLevel,
            tacticalGates = gates,
            laneObstacles = obstacles,
            zombies = initialZombies,
            difficulty = selectedDifficulty,
            isSurvivalMode = isSurvivalMode,
            currentWave = 1,
            maxWaves = 5,
            waveEnemiesTotal = if (isSurvivalMode) getWaveTotalEnemies(1) else targetKills,
            waveEnemiesSpawned = if (isSurvivalMode) 8 else 8,
            waveAnnouncement = if (isSurvivalMode) "WAVE 1 // SURVIVE THE HIGHWAY" else null,
            waveAnnouncementTtl = if (isSurvivalMode) 60 else 0,
            boss = initialBoss,
            player = PlayerState(
                currentLane = 1,
                visualLaneX = 1f,
                squadCount = 1,
                runDistance = 0f,
                targetDistance = targetDistance,
                health = 100,
                maxHealth = 100,
                ammoCurrent = magazineCapacity,
                ammoMax = magazineCapacity,
                reloadDurationMs = reloadDurationMs,
                damageMultiplier = 1.0f,
                fireRateMultiplier = 1.0f
            )
        )
    }

    var sessionKey by remember { mutableIntStateOf(0) }
    var combatState by remember { mutableStateOf(createInitialState()) }
    var isHoldingFire by remember { mutableStateOf(false) }

    // Suspend and pause gameplay on Android Lifecycle backgrounding
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        if (!combatState.isGameOver && !combatState.isVictory) {
            combatState = combatState.copy(isPaused = true)
            isHoldingFire = false
        }
    }

    // Intercept back button to pause cleanly
    BackHandler {
        if (!combatState.isPaused && !combatState.isGameOver && !combatState.isVictory) {
            combatState = combatState.copy(isPaused = true)
            isHoldingFire = false
        } else {
            navController.popBackStack()
        }
    }

    // Movement: Lane Switch Left
    fun switchLaneLeft() {
        if (combatState.isGameOver || combatState.isVictory || combatState.isPaused) return
        if (combatState.player.currentLane > 0) {
            GameSettings.triggerVibration(context, 15)
            combatState = combatState.copy(
                player = combatState.player.copy(currentLane = combatState.player.currentLane - 1)
            )
        }
    }

    // Movement: Lane Switch Right
    fun switchLaneRight() {
        if (combatState.isGameOver || combatState.isVictory || combatState.isPaused) return
        if (combatState.player.currentLane < 2) {
            GameSettings.triggerVibration(context, 15)
            combatState = combatState.copy(
                player = combatState.player.copy(currentLane = combatState.player.currentLane + 1)
            )
        }
    }

    // Reload Action
    fun triggerReload() {
        if (combatState.isGameOver || combatState.isVictory || combatState.isPaused) return
        if (combatState.player.isReloading || combatState.player.ammoCurrent == combatState.player.ammoMax) return
        val now = System.currentTimeMillis()
        SoundFx.playReload(context)
        GameSettings.triggerVibration(context, 25)
        combatState = combatState.copy(
            player = combatState.player.copy(
                isReloading = true,
                reloadTimerMs = now
            )
        )
    }

    // Fire Weapon Action
    fun fireWeapon(force: Boolean = false) {
        if (combatState.isGameOver || combatState.isVictory || combatState.isPaused) return
        val player = combatState.player

        if (player.isReloading) return
        if (player.ammoCurrent <= 0) {
            triggerReload()
            return
        }

        val now = System.currentTimeMillis()
        val effectiveFireRate = (weaponDef.fireRateMs / player.fireRateMultiplier).toLong()
        if (now - player.lastFireTimeMs < effectiveFireRate && !force) return

        val baseDamage = weaponDef.calculateDamage(weaponLevel)
        val scaledDamage = (baseDamage * player.damageMultiplier).toInt()

        // Create new lane bullets
        val newBullets = combatState.laneBullets.toMutableList()
        val pLane = player.visualLaneX
        val isCrit = Random.nextFloat() < 0.15f
        val finalDmg = if (isCrit) (scaledDamage * 1.5f).toInt() else scaledDamage
        val bulletColor = Color(weaponDef.colorHex)

        // Weapon distinct firing behavior
        when (weaponDef.id) {
            3 -> {
                // Breacher 12G: Fan spread across the current lane
                for (p in 0 until weaponDef.pellets) {
                    val spreadLaneOffset = (-0.25f + p * 0.16f)
                    newBullets.add(
                        LaneBullet(
                            id = (0..100000).random(),
                            startLaneX = pLane + spreadLaneOffset,
                            targetLane = player.currentLane,
                            distanceZ = 4f,
                            targetDistanceZ = 160f,
                            speedZ = 24f,
                            damage = finalDmg,
                            color = bulletColor,
                            isCritical = isCrit
                        )
                    )
                }
            }
            4 -> {
                // Barrett Reaper: Heavy armor-piercing round traveling far
                newBullets.add(
                    LaneBullet(
                        id = (0..100000).random(),
                        startLaneX = pLane,
                        targetLane = player.currentLane,
                        distanceZ = 6f,
                        targetDistanceZ = 350f,
                        speedZ = 32f,
                        damage = finalDmg,
                        color = Color(0xFF00E5FF),
                        isCritical = true
                    )
                )
            }
            else -> {
                // M4A1 Sentinel / Vector Venom
                newBullets.add(
                    LaneBullet(
                        id = (0..100000).random(),
                        startLaneX = pLane,
                        targetLane = player.currentLane,
                        distanceZ = 5f,
                        targetDistanceZ = 220f,
                        speedZ = 22f,
                        damage = finalDmg,
                        color = bulletColor,
                        isCritical = isCrit
                    )
                )
            }
        }

        // Support Fire from recruited squad members
        if (player.squadCount > 1) {
            val supportCount = minOf(6, player.squadCount / 2)
            for (s in 0 until supportCount) {
                val squadOffset = if (s % 2 == 0) -0.3f - (s * 0.1f) else 0.3f + (s * 0.1f)
                newBullets.add(
                    LaneBullet(
                        id = (0..100000).random(),
                        startLaneX = pLane + squadOffset,
                        targetLane = player.currentLane,
                        distanceZ = 2f,
                        targetDistanceZ = 200f,
                        speedZ = 20f,
                        damage = maxOf(10, (scaledDamage * 0.45f).toInt()),
                        color = Color(0xFF81D4FA),
                        isSquadBullet = true
                    )
                )
            }
        }

        SoundFx.playGunshot(context, weaponDef.name)
        GameSettings.triggerVibration(context, 10)

        combatState = combatState.copy(
            player = player.copy(
                ammoCurrent = player.ammoCurrent - 1,
                lastFireTimeMs = now
            ),
            laneBullets = newBullets,
            totalShotsFired = combatState.totalShotsFired + 1
        )
    }

    // Main 60FPS Game Loop
    LaunchedEffect(missionId, weaponDef, weaponLevel, sessionKey) {
        var lastSpawnTime = System.currentTimeMillis()
        var pauseStartTime = 0L

        while (!combatState.isGameOver && !combatState.isVictory) {
            if (combatState.isPaused) {
                if (pauseStartTime == 0L) {
                    pauseStartTime = System.currentTimeMillis()
                }
                delay(100)
                continue
            }

            if (pauseStartTime != 0L) {
                val pausedDuration = System.currentTimeMillis() - pauseStartTime
                lastSpawnTime += pausedDuration
                pauseStartTime = 0L
            }

            val now = System.currentTimeMillis()
            var state = combatState

            // 1. Reload timer update
            if (state.player.isReloading) {
                if (now - state.player.reloadTimerMs >= state.player.reloadDurationMs) {
                    state = state.copy(
                        player = state.player.copy(
                            isReloading = false,
                            ammoCurrent = state.player.ammoMax
                        )
                    )
                }
            }

            // 2. Smooth lane movement
            val currentLaneX = state.player.currentLane.toFloat()
            val newVisualX = state.player.visualLaneX + (currentLaneX - state.player.visualLaneX) * 0.22f
            val newRunDist = state.player.runDistance + (state.player.runSpeed * 0.48f)
            val newWalkPhase = (state.player.walkAnimPhase + 0.15f) % 6.28f

            var newPlayer = state.player.copy(
                visualLaneX = newVisualX,
                runDistance = newRunDist,
                walkAnimPhase = newWalkPhase
            )

            // 3. Tactical Gate Triggering
            var banner = state.comboBanner
            val updatedGates = state.tacticalGates.map { gate ->
                val relZ = gate.distanceZ - (newRunDist - state.player.runDistance)
                if (!gate.isPassed && relZ in -6f..3f && abs(newVisualX - gate.lane) < 0.65f) {
                    // Collect gate!
                    SoundFx.playCombo(context)
                    GameSettings.triggerVibration(context, 40)
                    when (gate.type) {
                        GateType.RECRUIT_SURVIVORS -> {
                            newPlayer = newPlayer.copy(squadCount = minOf(30, newPlayer.squadCount + 5))
                            banner = ComboBanner("RECRUITS JOINED!", "+5 SQUAD SURVIVORS", Color(0xFF00E676))
                        }
                        GateType.RECRUIT_RIFLEMEN -> {
                            newPlayer = newPlayer.copy(squadCount = minOf(30, newPlayer.squadCount + 10))
                            banner = ComboBanner("RIFLEMEN ATTACHED!", "+10 FIREPOWER SQUAD", Color(0xFF00E5FF))
                        }
                        GateType.DAMAGE_BOOST -> {
                            newPlayer = newPlayer.copy(damageMultiplier = newPlayer.damageMultiplier + 0.25f)
                            banner = ComboBanner("WEAPON OVERCLOCK!", "DAMAGE +25%", Color(0xFFFFD54F))
                        }
                        GateType.FIRE_RATE_BOOST -> {
                            newPlayer = newPlayer.copy(fireRateMultiplier = newPlayer.fireRateMultiplier + 0.20f)
                            banner = ComboBanner("RAPID TRIGGER!", "FIRE RATE +20%", Color(0xFFFF9100))
                        }
                        GateType.AMMO_CACHE -> {
                            newPlayer = newPlayer.copy(ammoCurrent = minOf(newPlayer.ammoMax * 2, newPlayer.ammoCurrent + 30))
                            banner = ComboBanner("AMMO CRATE!", "+30 ROUNDS CACHED", Color(0xFF76FF03))
                        }
                        GateType.SUPPLY_METAL -> {
                            newPlayer = newPlayer.copy(bonusMetalEarned = newPlayer.bonusMetalEarned + 200)
                            banner = ComboBanner("SUPPLY RUN!", "+200 METAL RECOVERED", Color(0xFF90A4AE))
                        }
                        GateType.SUPPLY_COINS -> {
                            newPlayer = newPlayer.copy(bonusCoinsEarned = newPlayer.bonusCoinsEarned + 300)
                            banner = ComboBanner("BOUNTY SECURED!", "+300 COINS LOOTED", Color(0xFFFFB300))
                        }
                        GateType.FIELD_MEDIC -> {
                            newPlayer = newPlayer.copy(health = minOf(newPlayer.maxHealth, newPlayer.health + 40))
                            banner = ComboBanner("FIELD TRIAGE!", "+40 HP RESTORED", Color(0xFF00E676))
                        }
                        GateType.SQUAD_HAZARD -> {
                            newPlayer = newPlayer.copy(squadCount = maxOf(1, newPlayer.squadCount - 3))
                            banner = ComboBanner("CASUALTIES!", "-3 SQUAD LOST", Color(0xFFFF1744))
                        }
                        GateType.ACID_SPILL -> {
                            newPlayer = newPlayer.copy(health = maxOf(10, newPlayer.health - 20))
                            banner = ComboBanner("TOXIC SPILL!", "-20 HP HAZARD", Color(0xFFFF1744))
                        }
                        GateType.AMMO_LEAK -> {
                            newPlayer = newPlayer.copy(ammoCurrent = maxOf(0, newPlayer.ammoCurrent - 15))
                            banner = ComboBanner("MAG DAMAGED!", "-15 AMMO LEAK", Color(0xFFFF5252))
                        }
                        GateType.ZOMBIE_AMBUSH -> {
                            banner = ComboBanner("AMBUSH!", "RUNNERS SPOTTED!", Color(0xFFFF1744))
                        }
                    }
                    gate.copy(distanceZ = relZ, isPassed = true)
                } else {
                    gate.copy(distanceZ = relZ)
                }
            }

            // 4. Obstacle Collision
            var screenShakeIntensity = state.screenShakeIntensity * 0.85f
            val updatedObstacles = state.laneObstacles.map { obs ->
                val relZ = obs.distanceZ - (newRunDist - state.player.runDistance)
                if (!obs.isHit && relZ in -3f..2f && abs(newVisualX - obs.lane) < 0.6f) {
                    SoundFx.playPlayerHurt(context)
                    GameSettings.triggerVibration(context, 35)
                    screenShakeIntensity = 12f
                    if (newPlayer.squadCount > 1) {
                        newPlayer = newPlayer.copy(squadCount = newPlayer.squadCount - 1)
                    } else {
                        newPlayer = newPlayer.copy(health = maxOf(0, newPlayer.health - 15))
                    }
                    obs.copy(distanceZ = relZ, isHit = true)
                } else {
                    obs.copy(distanceZ = relZ)
                }
            }

            // 5. Boss State Machine & Attacks
            var updatedBoss = state.boss
            val newShockwaves = state.laneShockwaves.map { it.copy(distanceZ = it.distanceZ - it.speedZ) }.toMutableList()

            updatedBoss?.let { boss ->
                var bHealth = maxOf(0, boss.health)
                var bPattern = boss.currentPattern
                var bTimer = boss.patternTimer + 1
                var bSlamCd = maxOf(0, boss.slamCooldown - 1)
                var bChargeCd = maxOf(0, boss.chargeCooldown - 1)
                var bSummonCd = maxOf(0, boss.summonCooldown - 1)
                var bIsDying = boss.isDying
                var bIsDefeated = boss.isDefeated
                var bDeathTimer = boss.deathTimer
                var bHasDamagedInCharge = boss.hasDamagedInCharge
                var bTelegraphLane = boss.telegraphLane
                var bLaneX = boss.visualLaneX
                var bDistZ = boss.distanceZ

                // Reliable death transition trigger: strictly happens once
                if (bHealth <= 0 && !bIsDying && !bIsDefeated) {
                    bIsDying = true
                    bDeathTimer = 0
                    bPattern = BossPattern.IDLE
                    bHasDamagedInCharge = true
                    newShockwaves.clear()
                }

                if (bIsDying || bIsDefeated) {
                    // Boss is dead or dying: immediately clear all attack shockwaves, cancel attacks, block charge damage
                    newShockwaves.clear()
                    bPattern = BossPattern.IDLE
                    bHasDamagedInCharge = true

                    if (bIsDying) {
                        bDeathTimer++
                        if (bDeathTimer >= boss.deathDuration) {
                            bIsDying = false
                            bIsDefeated = true
                        }
                    }
                } else {
                    when (bPattern) {
                        BossPattern.IDLE -> {
                            bHasDamagedInCharge = false
                            bDistZ = 130f
                            // Choose next attack pattern based on cooldowns
                            if (bSlamCd == 0) {
                                bPattern = BossPattern.TELEGRAPH_SLAM
                                bTimer = 0
                                bTelegraphLane = state.player.currentLane
                                SoundFx.playBossRoar(context)
                            } else if (bChargeCd == 0) {
                                bPattern = BossPattern.TELEGRAPH_CHARGE
                                bTimer = 0
                                bTelegraphLane = state.player.currentLane
                                SoundFx.playBossRoar(context)
                            } else if (bSummonCd == 0) {
                                bPattern = BossPattern.TELEGRAPH_SUMMON
                                bTimer = 0
                                SoundFx.playBossRoar(context)
                            }
                        }
                        BossPattern.TELEGRAPH_SLAM -> {
                            if (bTimer > 32) {
                                bPattern = BossPattern.EXECUTE_SLAM
                                bTimer = 0
                                bSlamCd = 110
                                SoundFx.playBossSlam(context)
                                GameSettings.triggerVibration(context, 45)
                                screenShakeIntensity = 18f
                                newShockwaves.add(LaneShockwave(lane = bTelegraphLane, distanceZ = bDistZ))
                            }
                        }
                        BossPattern.EXECUTE_SLAM -> {
                            if (bTimer > 15) {
                                bPattern = BossPattern.IDLE
                                bTimer = 0
                            }
                        }
                        BossPattern.TELEGRAPH_CHARGE -> {
                            bLaneX += (bTelegraphLane - bLaneX) * 0.15f
                            if (bTimer > 35) {
                                bPattern = BossPattern.EXECUTE_CHARGE
                                bTimer = 0
                                bChargeCd = 160
                                bHasDamagedInCharge = false
                            }
                        }
                        BossPattern.EXECUTE_CHARGE -> {
                            bDistZ -= 7.5f * state.zombieSpeedMult
                            if (!bHasDamagedInCharge && bDistZ <= 6f && abs(newVisualX - bTelegraphLane) < 0.65f) {
                                SoundFx.playPlayerHurt(context)
                                GameSettings.triggerVibration(context, 60)
                                screenShakeIntensity = 22f
                                bHasDamagedInCharge = true
                                val bossChargeDamage = (35 * state.zombieDamageMult).toInt()
                                if (newPlayer.squadCount > 2) {
                                    newPlayer = newPlayer.copy(squadCount = maxOf(1, newPlayer.squadCount - 2))
                                } else {
                                    newPlayer = newPlayer.copy(health = maxOf(0, newPlayer.health - bossChargeDamage))
                                }
                            }
                            if (bDistZ < -15f || bTimer > 40) {
                                bPattern = BossPattern.IDLE
                                bDistZ = 130f
                                bTimer = 0
                            }
                        }
                        BossPattern.TELEGRAPH_SUMMON -> {
                            if (bTimer > 25) {
                                bPattern = BossPattern.EXECUTE_SUMMON
                                bTimer = 0
                                bSummonCd = 200
                            }
                        }
                        BossPattern.EXECUTE_SUMMON -> {
                            bPattern = BossPattern.IDLE
                            bTimer = 0
                        }
                    }
                }

                updatedBoss = boss.copy(
                    health = bHealth,
                    currentPattern = bPattern,
                    patternTimer = bTimer,
                    slamCooldown = bSlamCd,
                    chargeCooldown = bChargeCd,
                    summonCooldown = bSummonCd,
                    hasDamagedInCharge = bHasDamagedInCharge,
                    telegraphLane = bTelegraphLane,
                    visualLaneX = bLaneX,
                    distanceZ = bDistZ,
                    hitFlash = maxOf(0, boss.hitFlash - 1),
                    isDying = bIsDying,
                    isDefeated = bIsDefeated,
                    deathTimer = bDeathTimer
                )
            }

            // Shockwave collision with player
            newShockwaves.removeAll { wave ->
                if (!wave.hasHitPlayer && wave.distanceZ in -4f..2f && abs(newVisualX - wave.lane) < 0.65f) {
                    SoundFx.playPlayerHurt(context)
                    GameSettings.triggerVibration(context, 40)
                    screenShakeIntensity = 15f
                    val shockwaveDamage = (25 * state.zombieDamageMult).toInt()
                    if (newPlayer.squadCount > 1) {
                        newPlayer = newPlayer.copy(squadCount = newPlayer.squadCount - 1)
                    } else {
                        newPlayer = newPlayer.copy(health = maxOf(0, newPlayer.health - shockwaveDamage))
                    }
                    wave.hasHitPlayer = true
                    true
                } else {
                    wave.distanceZ < -15f
                }
            }

            // 6. Zombie Spawning & Movement
            val newZombies = state.zombies.toMutableList()

            // Spawn additional zombies ahead
            val activeZombiesCount = newZombies.count { it.distanceZ > 0f && !it.isDying }
            if (activeZombiesCount < 8 && now - lastSpawnTime > 1400) {
                lastSpawnTime = now
                val zId = (1000..99999).random()
                val lane = (0..2).random()
                val isRunner = Random.nextFloat() < 0.35f
                val isBrute = Random.nextFloat() < 0.15f
                val isSpitter = Random.nextFloat() < 0.10f
                val type = when {
                    isBrute -> "Brute"
                    isRunner -> "Runner"
                    isSpitter -> "ToxicSpitter"
                    else -> "Walker"
                }
                val hp = when (type) {
                    "Brute" -> 220
                    "Runner" -> 30
                    "ToxicSpitter" -> 70
                    else -> 50
                }
                val spd = (when (type) {
                    "Brute" -> 0.8f
                    "Runner" -> 2.6f
                    "ToxicSpitter" -> 1.0f
                    else -> 1.3f
                }) * state.zombieSpeedMult
                val dmg = ((when (type) {
                    "Brute" -> 20
                    "Runner" -> 12
                    "ToxicSpitter" -> 15
                    else -> 10
                }) * state.zombieDamageMult).toInt()

                newZombies.add(
                    Zombie(
                        id = zId,
                        lane = lane,
                        visualLaneX = lane.toFloat(),
                        distanceZ = 300f + Random.nextFloat() * 50f,
                        health = hp,
                        maxHealth = hp,
                        type = type,
                        speed = spd,
                        damage = dmg
                    )
                )
            }

            // Move zombies & check collision with player
            val updatedZombies = mutableListOf<Zombie>()
            for (z in newZombies) {
                if (z.isDying) {
                    val dt = z.deathTimer + 1
                    if (dt < z.deathDuration) {
                        updatedZombies.add(z.copy(deathTimer = dt))
                    }
                    continue
                }

                val newZDist = z.distanceZ - (state.player.runSpeed + z.speed) * 0.48f
                val newZAnim = (z.animPhase + 0.12f) % 6.28f

                // Zombie hits the squad
                if (newZDist in -4f..1.5f && abs(newVisualX - z.visualLaneX) < 0.65f) {
                    SoundFx.playPlayerHurt(context)
                    GameSettings.triggerVibration(context, 30)
                    screenShakeIntensity = 10f
                    if (newPlayer.squadCount > 1) {
                        newPlayer = newPlayer.copy(squadCount = newPlayer.squadCount - 1)
                    } else {
                        newPlayer = newPlayer.copy(health = maxOf(0, newPlayer.health - z.damage))
                    }
                    // Zombie dies on impact with squad
                    updatedZombies.add(z.copy(isDying = true, deathTimer = 0, distanceZ = -5f))
                } else if (newZDist > -15f) {
                    updatedZombies.add(
                        z.copy(
                            distanceZ = newZDist,
                            animPhase = newZAnim,
                            hitFlash = maxOf(0, z.hitFlash - 1)
                        )
                    )
                }
            }

            // 7. Auto-Shooting targeting logic
            val closestZombie = updatedZombies
                .filter { it.distanceZ > 5f && !it.isDying }
                .minByOrNull {
                    // Prioritize zombies in player's current lane, then nearest distance
                    val laneDiff = abs(it.visualLaneX - newPlayer.currentLane)
                    it.distanceZ + laneDiff * 60f
                }

            val hasTarget = (closestZombie != null && closestZombie.distanceZ < 260f) || 
                (updatedBoss != null && updatedBoss!!.canAttack && updatedBoss!!.distanceZ < 240f)
            if (hasTarget || isHoldingFire) {
                fireWeapon()
            }

            // 8. Bullet flight and hit registration
            val newDamages = state.floatingDamages.toMutableList()
            var newKills = state.killCount
            val remainingBullets = mutableListOf<LaneBullet>()

            for (bullet in state.laneBullets) {
                val newBulletDist = bullet.distanceZ + bullet.speedZ
                var bulletConsumed = false

                // Check hit against boss
                updatedBoss?.let { boss ->
                    if (!bulletConsumed && !boss.isDead && abs(bullet.startLaneX - boss.visualLaneX) < 0.7f && abs(newBulletDist - boss.distanceZ) < 18f) {
                        bulletConsumed = true
                        val (damagedBoss, justDied) = boss.applyDamage(bullet.damage)
                        updatedBoss = damagedBoss
                        SoundFx.playZombieHit(context)
                        newDamages.add(
                            FloatingDamage(
                                text = "-${bullet.damage}",
                                position = Offset(500f, 320f),
                                isCritical = bullet.isCritical
                            )
                        )
                        if (justDied) {
                            SoundFx.playZombieDeath(context)
                            newKills++
                            newShockwaves.clear()
                        }
                    }
                }

                // Check hit against zombies
                if (!bulletConsumed) {
                    for (i in updatedZombies.indices) {
                        val z = updatedZombies[i]
                        if (!z.isDying && abs(bullet.startLaneX - z.visualLaneX) < 0.65f && abs(newBulletDist - z.distanceZ) < 16f) {
                            bulletConsumed = true
                            val newZHp = maxOf(0, z.health - bullet.damage)
                            SoundFx.playZombieHit(context)
                            newDamages.add(
                                FloatingDamage(
                                    text = "-${bullet.damage}",
                                    position = Offset(400f + z.visualLaneX * 100f, 400f),
                                    isCritical = bullet.isCritical
                                )
                            )
                            if (newZHp <= 0) {
                                SoundFx.playZombieDeath(context)
                                updatedZombies[i] = z.copy(health = 0, isDying = true, deathTimer = 0)
                                newKills++
                            } else {
                                updatedZombies[i] = z.copy(health = newZHp, hitFlash = 3)
                            }
                            break
                        }
                    }
                }

                if (!bulletConsumed && newBulletDist < bullet.targetDistanceZ) {
                    remainingBullets.add(bullet.copy(distanceZ = newBulletDist))
                }
            }

            // Update floating damages
            val survivingDamages = newDamages.mapNotNull {
                if (it.ttl > 1) it.copy(ttl = it.ttl - 1, position = Offset(it.position.x, it.position.y - 2f)) else null
            }

            // 9. Wave Progression (Survival Mode)
            var curWave = state.currentWave
            var waveNotice = state.waveAnnouncement
            var waveNoticeTtl = maxOf(0, state.waveAnnouncementTtl - 1)
            var wavesCleared = state.wavesClearedInRun

            if (state.isSurvivalMode) {
                val waveTotal = getWaveTotalEnemies(curWave)
                if (newKills >= waveTotal * curWave) {
                    if (curWave < state.maxWaves) {
                        curWave++
                        wavesCleared = curWave - 1
                        waveNotice = "WAVE $curWave // SURVIVE THE HIGHWAY"
                        waveNoticeTtl = 60
                        SoundFx.playCombo(context)
                        // Supply Drop Heal between waves
                        newPlayer = newPlayer.copy(
                            health = minOf(newPlayer.maxHealth, newPlayer.health + 30),
                            ammoCurrent = newPlayer.ammoMax
                        )
                    }
                }
            }

            // 10. Victory and Game Over Evaluation
            val isGameOver = (newPlayer.health <= 0)
            val isBossDead = (updatedBoss != null && (updatedBoss!!.health <= 0 || updatedBoss!!.isDefeated)) ||
                (state.boss != null && (state.boss!!.health <= 0 || state.boss!!.isDefeated))
            val isCampaignDistanceReached = (!state.isSurvivalMode && newPlayer.runDistance >= newPlayer.targetDistance)
            val isSurvivalWavesComplete = (state.isSurvivalMode && curWave >= 5 && (updatedBoss == null || updatedBoss!!.isDead) && newKills >= 60)

            val isVictory = if (isGameOver) false
            else if (state.isVictory) true
            else if (isBossMission) (isBossDead && (updatedBoss?.isDefeated == true || state.boss?.isDefeated == true))
            else if (state.isSurvivalMode) isSurvivalWavesComplete
            else isCampaignDistanceReached

            // Persist mission completion and survival records atomically
            if (isVictory && !state.isVictory) {
                if (state.isSurvivalMode) {
                    viewModel.claimSurvivalRewards(curWave, newKills.toLong())
                } else {
                    viewModel.completeMission(missionId, newKills.toLong())
                }
                // Persist bonus supplies collected from tactical gates
                if (newPlayer.bonusCoinsEarned > 0 || newPlayer.bonusMetalEarned > 0) {
                    viewModel.addSupplies(newPlayer.bonusCoinsEarned, newPlayer.bonusMetalEarned)
                }
            }

            combatState = state.copy(
                player = newPlayer,
                tacticalGates = updatedGates,
                laneObstacles = updatedObstacles,
                laneShockwaves = newShockwaves,
                boss = updatedBoss,
                zombies = updatedZombies,
                laneBullets = remainingBullets,
                floatingDamages = survivingDamages,
                killCount = newKills,
                currentWave = curWave,
                wavesClearedInRun = wavesCleared,
                waveAnnouncement = if (waveNoticeTtl > 0) waveNotice else null,
                waveAnnouncementTtl = waveNoticeTtl,
                screenShakeIntensity = screenShakeIntensity,
                comboBanner = banner,
                isGameOver = isGameOver,
                isVictory = isVictory
            )

            delay(16) // ~60 FPS update rate
        }
    }

    // Root UI Container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1215))
    ) {
        // Main 3D Perspective Canvas with Touch Swipe Controls
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(sessionKey) {
                    var totalDragX = 0f
                    detectDragGestures(
                        onDragStart = { totalDragX = 0f },
                        onDragEnd = {
                            if (totalDragX < -40f) {
                                switchLaneLeft()
                            } else if (totalDragX > 40f) {
                                switchLaneRight()
                            }
                            totalDragX = 0f
                        },
                        onDrag = { _, dragAmount ->
                            totalDragX += dragAmount.x
                        }
                    )
                }
        ) {
            val environmentMode = GameSettings.getEnvironmentMode(context)
            LaneRenderer.renderScene(
                drawScope = this,
                state = combatState,
                width = size.width,
                height = size.height,
                environmentMode = environmentMode
            )
        }

        // TOP TACTICAL HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Player Health & Squad Size
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "HP: ${combatState.player.health}/${combatState.player.maxHealth}",
                            color = if (combatState.player.health > 40) Color(0xFF00E676) else Color(0xFFFF1744),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.width(8.dp))
                        // Squad Size Badge
                        Surface(
                            color = Color(0xDD0288D1),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "👥 SQUAD: ${combatState.player.squadCount}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    // Health bar
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(6.dp)
                            .background(Color(0xFF263238), RoundedCornerShape(3.dp))
                    ) {
                        val hpRatio = (combatState.player.health.toFloat() / combatState.player.maxHealth).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(hpRatio)
                                .background(
                                    if (hpRatio > 0.4f) Color(0xFF00E676) else Color(0xFFFF1744),
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }

                // Center: Distance to Extraction or Survival Wave
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (combatState.isSurvivalMode) {
                        Text(
                            text = "WAVE ${combatState.currentWave} / ${combatState.maxWaves}",
                            color = Color(0xFFE040FB),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "💀 KILLS: ${combatState.killCount}",
                            color = Color(0xFFB0BEC5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        val distMeters = combatState.player.runDistance.toInt()
                        val targetMeters = combatState.player.targetDistance.toInt()
                        Text(
                            text = "EXTRACTION: $distMeters / ${targetMeters}m",
                            color = Color(0xFFFFB300),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        // Progress bar
                        val distRatio = (combatState.player.runDistance / combatState.player.targetDistance).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(5.dp)
                                .background(Color(0xFF263238), RoundedCornerShape(2.5.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(distRatio)
                                    .background(Color(0xFFFFB300), RoundedCornerShape(2.5.dp))
                            )
                        }
                    }
                }

                // Right: Weapon, Ammo, and Pause Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = weaponDef.name.uppercase(),
                            color = Color(0xFF90A4AE),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (combatState.player.isReloading) "RELOADING..."
                            else "${combatState.player.ammoCurrent} / ${combatState.player.ammoMax}",
                            color = if (combatState.player.ammoCurrent > 5) Color.White else Color(0xFFFF1744),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { combatState = combatState.copy(isPaused = true) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xCC263238), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Top Boss Bar when Boss is Active
            combatState.boss?.let { boss ->
                Spacer(Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xDD1A0026)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE040FB), RoundedCornerShape(6.dp))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "⚠️ THE ABOMINATION // APEX MUTANT",
                                color = Color(0xFFE040FB),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${boss.health} / ${boss.maxHealth} HP",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        val bossHpRatio = (boss.health.toFloat() / boss.maxHealth).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(Color(0xFF311B92), RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(bossHpRatio)
                                    .background(Color(0xFFE040FB), RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }

            // Wave Announcement Banner (Survival Mode)
            combatState.waveAnnouncement?.let { ann ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color(0xDD4A148C),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFFE040FB), RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = ann,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // BOTTOM COMBAT CONTROLS (Ergonomic Touchscreen Layout)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Lane Switch Button
                Button(
                    onClick = { switchLaneLeft() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD263238)),
                    modifier = Modifier
                        .size(width = 90.dp, height = 54.dp)
                        .border(1.5.dp, Color(0xFF455A64), RoundedCornerShape(12.dp))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Lane Left", tint = Color.White)
                        Spacer(Modifier.width(2.dp))
                        Text("LANE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Center: Reload and Rapid Fire Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Reload Button
                    Button(
                        onClick = { triggerReload() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD37474F)),
                        modifier = Modifier
                            .height(54.dp)
                            .border(1.5.dp, Color(0xFFFFB300), RoundedCornerShape(12.dp))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color(0xFFFFB300))
                            Spacer(Modifier.width(4.dp))
                            Text("RELOAD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Manual Rapid Fire Button
                    Button(
                        onClick = { fireWeapon(force = true) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xDDE53935)),
                        modifier = Modifier
                            .height(54.dp)
                            .border(1.5.dp, Color(0xFFFF5252), RoundedCornerShape(12.dp))
                    ) {
                        Text("FIRE 🎯", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }

                // Right Lane Switch Button
                Button(
                    onClick = { switchLaneRight() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD263238)),
                    modifier = Modifier
                        .size(width = 90.dp, height = 54.dp)
                        .border(1.5.dp, Color(0xFF455A64), RoundedCornerShape(12.dp))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("LANE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.width(2.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Lane Right", tint = Color.White)
                    }
                }
            }
        }

        // PAUSE DIALOG
        if (combatState.isPaused && !combatState.isGameOver && !combatState.isVictory) {
            AlertDialog(
                onDismissRequest = { combatState = combatState.copy(isPaused = false) },
                title = { Text("MISSION PAUSED", fontWeight = FontWeight.Black, color = Color(0xFFFFB300)) },
                text = {
                    Column {
                        Text("Tactical operations are suspended.", color = Color.LightGray)
                        Spacer(Modifier.height(8.dp))
                        Text("Distance: ${combatState.player.runDistance.toInt()}m", color = Color.White)
                        Text("Kills: ${combatState.killCount}", color = Color.White)
                        Text("Squad Size: ${combatState.player.squadCount}", color = Color(0xFF00E676))
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { combatState = combatState.copy(isPaused = false) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                    ) {
                        Text("RESUME", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { navController.popBackStack() }) {
                        Text("RETURN TO BASE", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E2228)
            )
        }

        // Tutorial Overlay
        if (combatState.player.runDistance < 100f && !combatState.isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TUTORIAL", color = Color(0xFFFFB300), fontWeight = FontWeight.Black, fontSize = 24.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("SWIPE TO CHOOSE YOUR LANE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Use swipe gestures or bottom buttons to dodge hazards.", color = Color.LightGray, fontSize = 14.sp)
                }
            }
        }

        // VICTORY DIALOG
        if (combatState.isVictory) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text("OPERATION SUCCESSFUL // EXTRACTED", fontWeight = FontWeight.Black, color = Color(0xFF00E676))
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Perimeter breached and extraction confirmed!", color = Color.LightGray, fontSize = 12.sp)
                        HorizontalDivider(color = Color(0xFF37474F))
                        Text("Distance Run: ${combatState.player.runDistance.toInt()}m", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Zombies Eliminated: ${combatState.killCount}", color = Color.White)
                        Text("Surviving Squad: ${combatState.player.squadCount} operatives", color = Color(0xFF00E676))
                        Spacer(Modifier.height(4.dp))
                        Text("REWARDS EARNED:", fontWeight = FontWeight.Bold, color = Color(0xFFFFB300), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("🪙 +${rewardCoins + combatState.player.bonusCoinsEarned} Coins", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                            Text("🍞 +$rewardFood Food", color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                            if (combatState.player.bonusMetalEarned > 0) {
                                Text("🔩 +${combatState.player.bonusMetalEarned} Metal", color = Color(0xFF90A4AE), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                    ) {
                        Text("RETURN TO BASE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            sessionKey++
                            combatState = createInitialState()
                        }
                    ) {
                        Text("REPLAY MISSION", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1A211D)
            )
        }

        // DEFEAT DIALOG
        if (combatState.isGameOver) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text("SQUAD ELIMINATED // KIA", fontWeight = FontWeight.Black, color = Color(0xFFFF1744))
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Your squad was overwhelmed on the highway.", color = Color.LightGray, fontSize = 12.sp)
                        HorizontalDivider(color = Color(0xFF37474F))
                        Text("Distance Reached: ${combatState.player.runDistance.toInt()}m", color = Color.White)
                        Text("Zombies Eliminated: ${combatState.killCount}", color = Color.White)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            sessionKey++
                            combatState = createInitialState()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
                    ) {
                        Text("RETRY RUN", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { navController.popBackStack() }) {
                        Text("RETURN TO BASE", color = Color.White)
                    }
                },
                containerColor = Color(0xFF241517)
            )
        }
    }
}
